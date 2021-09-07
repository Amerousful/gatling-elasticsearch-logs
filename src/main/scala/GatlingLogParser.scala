import com.fasterxml.jackson.core.JsonGenerator

object GatlingLogParser {

  def httpFields(gen: JsonGenerator, fullMessage: String): Unit = {
    val separator = "========================="

    // Gatling since 3.4.2 write two logs instead one.
    // First log only with message and not useful for us. Parse and send log only with separator.
    if (!fullMessage.contains(separator)) {}
    else {
      val partOfMessage = fullMessage.split(separator)
      val infoPart = partOfMessage(0)
      val sessionPart = partOfMessage(1)
      val requestPart = partOfMessage(2)
      val responsePart = partOfMessage(3)

      val firstPattern = """Request:\n(.*?):\s(.*)""".r.unanchored
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
      val statusPattern(statusCode) = responsePart
      val responseHeadersPattern(responseHeadersRaw) = responsePart
      val responseHeaders = responseHeadersRaw.replaceAll("\t", "")

      val sessionNamePattern(scenario) = session
      val userIdPattern = s"""$scenario,(\\d+),""".r.unanchored
      val userIdPattern(userId) = session

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

  def wsFields(gen: JsonGenerator, fullMessage: String): Unit = {
    gen.writeObjectField("message", fullMessage)
    gen.writeObjectField("protocol", "ws")
  }

}
