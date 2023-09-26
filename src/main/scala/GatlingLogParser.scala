import com.fasterxml.jackson.core.JsonGenerator

object GatlingLogParser {

  private val separator = "========================="

  def httpFields(gen: JsonGenerator, fullMessage: String, extractSessionAttributes: Option[String] = None): Unit = {

    // Gatling since 3.4.2 write two logs instead one.
    // First log only with message.
    if (!fullMessage.contains(separator)) {
      gen.writeObjectField("message", fullMessage)
    }
    else {
      val partOfMessage = fullMessage.split(separator)
      val infoPart = partOfMessage(0)
      val sessionPart = partOfMessage(1)
      val requestPart = partOfMessage(2)
      val responsePart = partOfMessage(3)

      val firstPattern = """Request:\n(.*):\s(.*)""".r.unanchored
      val secondPattern = """Session:\n(.*)""".r.unanchored
      val methodAndUrlPattern = """HTTP request:\n(\w+)\s(.*)""".r.unanchored
      val requestBodyPattern = """body:([\s\S]*)\n""".r.unanchored
      val responseBodyPattern = """body:\n([\s\S]*)\n""".r.unanchored
      val requestHeadersPattern = {
        if (requestPart.contains("byteArraysBody")) """headers:\n\t([\s\S]*)\nbyteArraysBody""".r.unanchored
        else if (requestPart.contains("body")) """headers:\n\t([\s\S]*)\nbody""".r.unanchored
        else """headers:\n\t([\s\S]*)\n""".r.unanchored
      }
      val responseHeadersPattern = """headers:\n\t([\s\S]*?)\n\n""".r.unanchored

      val statusPattern = """status:\n\t(\d{3})""".r.unanchored
      val sessionNamePattern = """Session\((.*?),""".r.unanchored


      val firstPattern(requestName, messageRaw) = infoPart
      val message = messageRaw.trim
      val secondPattern(session) = sessionPart
      val methodAndUrlPattern(method, url) = requestPart

      val requestBody = requestPart match {
        case requestBodyPattern(result) => result
        case _ => "%empty%"
      }
      val responseBody = responsePart match {
        case responseBodyPattern(result) => result
        case _ => "%empty%"
      }
      val requestHeadersPattern(requestHeadersRaw) = requestPart
      val requestHeaders = requestHeadersRaw.replaceAll("\t", "")
      val statusCode = responsePart match {
        case statusPattern(result) => result
        case _ => "%empty%"
      }
      val responseHeadersRaw = responsePart match {
        case responseHeadersPattern(result) => result
        case _ => "%empty%"
      }
      val responseHeaders = responseHeadersRaw.replaceAll("\t", "")

      val sessionNamePattern(scenario) = session
      val userIdPattern = s"""$scenario,(\\d+),""".r.unanchored
      val userIdPattern(userId) = session

      extractSessionAttributes match {
        case Some(attributes) if attributes.nonEmpty =>
          val extract = attributes.split(";")
          extract.foreach { key =>
            val regex = raw"""$key\s->\s([^,)]*)""".r.unanchored

            session match {
              case regex(value) => gen.writeObjectField(key, value)
              case _ =>
            }
          }
        case _ =>
      }

      gen.writeObjectField("message", message)
      gen.writeObjectField("method", method)
      gen.writeObjectField("url", url)
      gen.writeObjectField("request_body", requestBody)
      gen.writeObjectField("request_headers", requestHeaders)
      gen.writeObjectField("status_code", statusCode)
      gen.writeObjectField("response_body", responseBody)
      gen.writeObjectField("response_headers", responseHeaders)
      gen.writeObjectField("session", session)
      gen.writeObjectField("scenario", scenario)
      gen.writeObjectField("userId", userId)
      gen.writeObjectField("request_name", requestName)
      gen.writeObjectField("protocol", "http")
    }
  }

  def wsFields(gen: JsonGenerator, fullMessage: String, extractSessionAttributes: Option[String] = None): Unit = {

    // Gatling since 3.4.2 write two logs instead one.
    // First log only with message.
    if (!fullMessage.contains(separator)) {
      gen.writeObjectField("message", fullMessage)
    }
    else {
      val partOfMessage = fullMessage.split(separator)

      val firstPattern = """Request:\n(.*):\s(.*)""".r.unanchored
      val secondPattern = """Session:\n(.*)""".r.unanchored
      val sessionNamePattern = """Session\((.*?),""".r.unanchored
      val checkNamePattern = """WebSocket check:\n(.*)""".r.unanchored
      val requestPattern = """WebSocket request:\n([\s\S]*)\n""".r.unanchored
      val receivedPattern = """WebSocket received messages:\n([\s\S]*)\n""".r.unanchored

      val wsLog: WsLog = partOfMessage.length match {
        // for request without check and response body
        case 4 => WsLog(partOfMessage(0), partOfMessage(1), None, partOfMessage(2), None)
        case 5 => WsLog(partOfMessage(0), partOfMessage(1), Some(partOfMessage(2)), partOfMessage(3), Some(partOfMessage(4)))
        case _ => throw new RuntimeException(s"Failed to parse WS log: ${fullMessage}")
      }

      val secondPattern(session) = wsLog.sessionPart

      val checkName = wsLog.checkPart.getOrElse("") match {
        case checkNamePattern(result) => result
        case _ => "%empty%"
      }

      val responseBody = wsLog.responsePart.getOrElse("") match {
        case receivedPattern(result) => result
        case _ => "%empty%"
      }

      val requestPattern(requestBody) = wsLog.requestPart
      val firstPattern(requestName, messageRaw) = wsLog.infoPart
      val message = messageRaw.trim

      val urlRegex = """gatling\.http\.cache\.wsBaseUrl\s->\s([^,)]*)""".r.unanchored
      val urlRegex(url) = session

      val sessionNamePattern(scenario) = session
      val userIdPattern = s"""$scenario,(\\d+),""".r.unanchored
      val userIdPattern(userId) = session

      extractSessionAttributes match {
        case Some(attributes) if attributes.nonEmpty =>
          val extract = attributes.split(";")
          extract.foreach { key =>
            val regex = raw"""$key\s->\s([^,)]*)""".r.unanchored

            session match {
              case regex(value) => gen.writeObjectField(key, value)
              case _ =>
            }
          }
        case _ =>
      }

      gen.writeObjectField("message", message)
      gen.writeObjectField("url", url)
      gen.writeObjectField("request_body", requestBody)
      gen.writeObjectField("response_body", responseBody)
      gen.writeObjectField("check_name", checkName)
      gen.writeObjectField("session", session)
      gen.writeObjectField("scenario", scenario)
      gen.writeObjectField("userId", userId)
      gen.writeObjectField("request_name", requestName)
      gen.writeObjectField("protocol", "ws")
    }
  }

  def sessionFields(gen: JsonGenerator, fullMessage: String, extractSessionAttributes: Option[String] = None): Unit = {
    val messagePattern = """(.*?)\sSession.*\)(.*)""".r.unanchored
    val sessionPattern = """(Session.*\))""".r.unanchored
    val scenarioNamePattern = """Session\((.*?),""".r.unanchored

    val sessionPattern(session) = fullMessage
    val scenarioNamePattern(scenario) = session

    val userIdPattern = s"""$scenario,(\\d+),""".r.unanchored
    val userIdPattern(userId) = session

    extractSessionAttributes match {
      case Some(attributes) if attributes.nonEmpty =>
        val extract = attributes.split(";")
        extract.foreach { key =>
          val regex = raw"""$key\s->\s([^,)]*)""".r.unanchored

          session match {
            case regex(value) => gen.writeObjectField(key, value)
            case _ =>
          }
        }
      case _ =>
    }

    val s"${messagePattern(firstPart, secondPart)}" = fullMessage
    val message = firstPart + secondPart

    gen.writeObjectField("message", message)
    gen.writeObjectField("session", session)
    gen.writeObjectField("scenario", scenario)
    gen.writeObjectField("userId", userId)
  }

}
