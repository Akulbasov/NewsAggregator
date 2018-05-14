package naggr.implicits

import org.apache.spark.sql.streaming.{DataStreamReader, DataStreamWriter}
import org.apache.spark.sql.{DataFrameWriter, Dataset, Row, SparkSession}


trait SparkImplicits {
  private val kafkaBootServer = "kafka:9092"

  implicit class KafkaStreamWriter(dW:DataStreamWriter[Row]) {
    def withKafkaProducer(topic:String, checkpoint:String): DataStreamWriter[Row] = {
        dW
          .format("kafka")
          .option("kafka.bootstrap.servers", kafkaBootServer)
          .option("topic", topic).option("checkpointLocation", checkpoint)
          .outputMode("complete")
      }
  }

  implicit class KafkaFrameWriter(dW:DataFrameWriter[Row]) {
    def withKafkaProducer(topic:String): DataFrameWriter[Row] = {
      dW
        .format("kafka")
        .option("kafka.bootstrap.servers", kafkaBootServer)
        .option("topic", topic)
    }
  }

  implicit class KafkaReader(dsR: DataStreamReader) {
    def withKafkaConsumer(topic:String,offset:String): DataStreamReader = {
      dsR
        .format("kafka")
        .option("kafka.bootstrap.servers", kafkaBootServer)
        .option("subscribe", topic)
        .option("startingOffsets", offset)
    }
  }
}
