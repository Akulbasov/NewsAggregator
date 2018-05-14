package naggr.utils

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf
trait StringUtils extends Serializable with FileUtils {

  val removeHtmlElementsUdf:UserDefinedFunction = udf[Option[String], String](removeHtmlElements)
  val removeStopWordsUdf:UserDefinedFunction = udf[Option[String], String](removeStopWords)

  private[utils] def removeHtmlElements(str:String):Option[String] = {
    Option(str).getOrElse(return None)
    Some(str.replaceAll("\"","").replaceAll("<.*>",""))
  }
  private[utils] def removeStopWords(str:String):Option[String] = {
    Option(str).getOrElse(return None)
    val linesStopWords = getLinesOfFile.get
    val strList = str.split(" ")
    if(linesStopWords.intersect(strList).nonEmpty)
      Some(str.replaceAll(linesStopWords.intersect(strList) mkString "|",""))
    else
      Some(str)
  }

}

//def betterLowerRemoveAllWhitespace(s: String): Option[String] = {
//val str = Option(s).getOrElse(return None)
//Some(str.toLowerCase().replaceAll("\\s", ""))
//}
//
//val betterLowerRemoveAllWhitespaceUDF = udf[Option[String], String](betterLowerRemoveAllWhitespace)

