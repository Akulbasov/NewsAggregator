package naggr.utils
import org.apache.spark.sql.Column
import org.scalatest.FlatSpec
import org.apache.spark._
import scala.util._

class StringUtilsSpec extends FlatSpec with StringUtils {

  override def getBaseAppURI = {
    System.getProperty("user.dir")+"/src/test/app/"
  }

  "String with stop words --test and not stop word moscow" must "delete this stopwords --test" in {
    assert(removeStopWords("test moscow").get==" moscow")
  }

  "String without stop words" must "not delete any string" in {
    assert(removeStopWords("moscow").get=="moscow")
  }

  "String with stop words --test" must "delete this stopwords --test" in {
    assert(removeStopWords("test").get=="")
  }

  "String with <.*> " must "delete this pattern" in {
    assert(
      removeHtmlElements(
        """Gaming developers in Morocco are tapping into the eSports market,
          |which is estimated to have a global revenue of over $900 million.
          |<img src="http://feeds.feedburner.com/~r/rss/edition_world/~4/W_6v4E8YVCU"
          |height="1" width="1" alt=""/>"""
        .stripMargin.replaceAll("\n", "")
      ).get
        ===
        """Gaming developers in Morocco are tapping into the eSports market,
          |which is estimated to have a global revenue of over $900 million."""
          .stripMargin.replaceAll("\n", "")
    )
  }

  "String with <.* " must "not delete , but delete square br" in {
    assert(
      removeHtmlElements(
        """Gaming developers in Morocco are tapping into the eSports market,
          |which is estimated to have a global revenue of over $900 million.
          |<img src="http://feeds.feedburner.com/~r/rss/edition_world/~4/W_6v4E8YVCU"""
          .stripMargin.replaceAll("\n", "").replaceAll("\"","").replaceAll("<.*>","")
      ).get
        ===
        """Gaming developers in Morocco are tapping into the eSports market,
          |which is estimated to have a global revenue of over $900 million.
          |<img src=http://feeds.feedburner.com/~r/rss/edition_world/~4/W_6v4E8YVCU"""
          .stripMargin.replaceAll("\n", "")
    )
  }

  "Empty string" must "return empty string" in {
    val empty =
      removeHtmlElements(
        ""
      )

    assert(empty.get === "")
  }
}
