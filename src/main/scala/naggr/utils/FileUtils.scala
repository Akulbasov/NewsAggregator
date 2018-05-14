package naggr.utils

import java.io.{File, IOException}
import java.net.{URISyntaxException, URL}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import scala.io.Source

trait FileUtils extends Serializable {
  private[utils] def getBaseAppURI = {
    "/app/"
  }
  @throws[IOException]
  @throws[URISyntaxException]
  @throws[ArrayIndexOutOfBoundsException]
  private[utils] def getListOfAllFilesInResources: Option[Array[File]] = {
    val f = for (file <- new File(getBaseAppURI).listFiles;
                 if file.isFile;
                 if file.toString.endsWith(".txt")
    ) yield {
      file
    }
    if(f.isEmpty)
      None
    else
      Some(f)
  }
  private[utils] def getLinesOfFile:Option[List[String]] = {
    getListOfAllFilesInResources match {
      case Some(x) =>
        Some(Source.fromFile(x(0).getAbsolutePath).getLines.toList)
      case None =>
        None
    }
  }

}
