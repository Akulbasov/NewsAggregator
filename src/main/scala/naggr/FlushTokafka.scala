package naggr

import com.github.catalystcode.fortis.spark.streaming.rss.RSSInputDStream
import naggr.implicits.SparkImplicits
import naggr.utils.{FileUtils, StringUtils}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{get_json_object, _}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}




/**
  * Read stream from rss and write to kafka .
  */

object FlushTokafka extends FileUtils with SparkImplicits with StringUtils{
  def main(args: Array[String]): Unit = {

    val durationSeconds = 1
    val conf = new SparkConf().setAppName("RSS Spark Application")
    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(durationSeconds))
    sc.setLogLevel("ERROR")
    val urlCSV = {
      "http://rss.cnn.com/rss/cnn_topstories.rss," +
      "http://rss.cnn.com/rss/edition.rss," +
        "http://rss.cnn.com/rss/edition_world.rss," +
        "http://rss.cnn.com/rss/edition_africa.rss," +
        "http://rss.cnn.com/rss/edition_americas.rss," +
        "http://rss.cnn.com/rss/edition_asia.rss," +
        "http://rss.cnn.com/rss/edition_europe.rss," +
        "http://rss.cnn.com/rss/edition_meast.rss," +
        "http://rss.cnn.com/rss/edition_us.rss," +
        "http://rss.cnn.com/rss/money_news_international.rss," +
        "http://rss.cnn.com/rss/edition_technology.rss," +
        "http://rss.cnn.com/rss/edition_space.rss," +
        "http://rss.cnn.com/rss/edition_entertainment.rss," +
        "http://rss.cnn.com/rss/edition_sport.rss," +
        "http://rss.cnn.com/rss/edition_football.rss," +
        "http://rss.cnn.com/rss/edition_golf.rss," +
        "http://rss.cnn.com/rss/edition_motorsport.rss," +
        "http://rss.cnn.com/rss/edition_tennis.rss," +
        "http://rss.cnn.com/rss/edition_travel.rss," +
        "http://rss.cnn.com/rss/cnn_freevideo.rss," +
        "https://trends.google.com/trends/hottrends/atom/feed?pn=p1"
    }
    val urls = urlCSV.split(",")
    val spark = SparkSession.builder().appName(sc.appName).getOrCreate()


      spark
        .readStream
          .withKafkaConsumer("cnntopic1","earliest")
        .load()
        .select(
          get_json_object(col("value").cast("string"), "$.publishedDate").as("publishedDate"),
          get_json_object(col("value").cast("string"), "$.title").as("title"),
          get_json_object(col("value").cast("string"), "$.uri").as("uri"),
          get_json_object(lower(col("value")).cast("string"), "$.description.value").as("description"),
          explode(split(
            removeStopWordsUdf(
              removeHtmlElementsUdf(
                get_json_object(lower(col("value")).cast("string"), "$.description.value")
              )
            )," ")
          ).alias("cvalue")
        ).groupBy("description","uri","title","publishedDate","cvalue")
        .count()
        .select(to_json(struct("*")) as 'value)
        .writeStream
        .withKafkaProducer("cnntopicagg","/tmp/sparkCheckpoint/cnn")
        .start()



      spark
        .readStream.withKafkaConsumer("trendstopic1","earliest")
        .load()
        .select(
          get_json_object(col("value").cast("string"), "$.publishedDate").as("publishedDate"),
          get_json_object(col("value").cast("string"), "$.title").as("title"),
          get_json_object(col("value").cast("string"), "$.uri").as("uri"),
          get_json_object(lower(col("value")).cast("string"), "$.description.value").as("description"),
            explode(split(
              removeHtmlElementsUdf(
              get_json_object(lower(col("value")).cast("string"), "$.title")
            )," ")
          ).alias("tvalue")
        ).groupBy("description","uri","title","publishedDate","tvalue")
        .count()
        .select(to_json(struct("*")) as 'value)
        .writeStream
        .withKafkaProducer("trendstopicagg","/tmp/sparkCheckpoint/trends")
        .start()



    val cnntopicagg = spark.readStream.withKafkaConsumer("cnntopicagg","latest")
      .load()
      .select(
        get_json_object(col("value").cast("string"), "$.publishedDate").as("publishedDate"),
        get_json_object(col("value").cast("string"), "$.title").as("title"),
        get_json_object(col("value").cast("string"), "$.uri").as("uri"),
        get_json_object(col("value").cast("string"), "$.cvalue").alias("cvalue")
      ).dropDuplicates()

    val trendstopicagg = spark.readStream.withKafkaConsumer("trendstopicagg","latest")
      .load()
      .select(
        get_json_object(col("value").cast("string"), "$.publishedDate").as("publishedDate"),
        get_json_object(col("value").cast("string"), "$.title").as("title"),
        get_json_object(col("value").cast("string"), "$.uri").as("uri"),
        get_json_object(col("value").cast("string"), "$.tvalue").alias("tvalue")
      ).dropDuplicates()



    cnntopicagg
      .join(trendstopicagg,expr("""tvalue = cvalue"""))
      .select(to_json(struct("*")) as 'value)
      .dropDuplicates()
      .writeStream
      .withKafkaProducer("filtertopic","/tmp/sparkCheckpoint/filter")
      .outputMode("append")
      .start()


    val stream = new RSSInputDStream(urls, Map[String, String](
      "User-Agent" -> "Mozilla/5.0 (X11; Linux x86_64; rv:59.0) Gecko/20100101 Firefox/59.0"
    ), ssc, StorageLevel.MEMORY_ONLY, pollingPeriodInSeconds = durationSeconds,readTimeout = 7000)
    stream
      .foreachRDD(rdd=>{
        if(!rdd.isEmpty){
          import spark.sqlContext.implicits._
          val rssDf = rdd.toDF(
            "source",
            "uri",
            "title",
            "links",
            "content",
            "description",
            "enclosures",
            "publishedDate",
            "updatedDate",
            "authors",
            "contributors"
          )

          val rssDfCNN = rssDf.filter($"uri".contains("rss.cnn.com"))
            .withColumn("parsingtimestamp", lit(current_timestamp()))

          val rssDfTrends = rssDf.filter($"uri".contains("trends"))
            .withColumn("parsingtimestamp", lit(current_timestamp()))

          rssDfCNN
            .select(to_json(struct("*")) as 'value)
            .write
            .withKafkaProducer("cnntopic1")
            .save()

          rssDfTrends
            .select(to_json(struct("*")) as 'value)
            .write
            .withKafkaProducer("trendstopic1")
            .save()
        }
      })



    ssc.start()
    ssc.awaitTermination()
  }
}