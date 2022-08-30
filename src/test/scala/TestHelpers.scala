import GatlingLogParser.httpFields
import com.fasterxml.jackson.core.{JsonFactory, JsonToken}

import java.io.StringWriter
import scala.collection.mutable
import scala.io.Source
import scala.util.Using
import org.scalatest.Checkpoints._

object TestHelpers {

  private val factory = new JsonFactory

  private def readFile(filename: String): String = {
    val path = getClass.getClassLoader.getResource(filename).getPath
    Using(Source.fromFile(path))(_.mkString).getOrElse("")
  }

  private def fetchContent(fullMessage: String, extractSessionAttributes: Option[String]): String = {
    val jsonObjectWriter = new StringWriter
    val generator = factory.createGenerator(jsonObjectWriter)
    generator.useDefaultPrettyPrinter
    generator.writeStartObject()

    httpFields(generator, fullMessage, extractSessionAttributes)

    generator.close()
    jsonObjectWriter.toString
  }

  def parseHttpLog(fileName: String, extractSessionAttributes: Option[String] = None) = {
    val raw = readFile(fileName)

    // via httpFields(...)
    val parsedContent = fetchContent(raw, extractSessionAttributes)

    // parse and add to map for test
    val parser = factory.createParser(parsedContent)
    val result: mutable.HashMap[String, String] = new mutable.HashMap

    if (parser.nextToken().equals(JsonToken.START_OBJECT)) {
      while (JsonToken.END_OBJECT != parser.nextToken()) {
        val k = parser.getCurrentName
        parser.nextToken()
        val v = parser.getText()
        result += (k -> v)
      }
    }
    result
  }

  implicit class CheckpointWrapper[A](function: => A) {
    def apply(): A = function
  }

  def softAssert[Assertion](attempts: CheckpointWrapper[Assertion]*): Unit = {
    val softy = new Checkpoint
    attempts.foreach(assert => softy(assert.apply()))
    softy.reportAll()
  }

}
