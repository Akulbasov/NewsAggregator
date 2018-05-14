package naggr.utils

import org.scalatest.FlatSpec
class FileUtilsSpec extends FlatSpec with FileUtils {

  override def getBaseAppURI = {
    System.getProperty("user.dir")+"/src/test/app"
  }

  "File with content " +
    "testing" +
    "test" +
    "test and " +
    "format txt" must "get Seq lines of this file" in {
      assert(getLinesOfFile.get == Seq("testing","test","test"))
    }



}
