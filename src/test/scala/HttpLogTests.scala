import TestHelpers._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.{be, convertToAnyShouldWrapper}

class HttpLogTests extends AnyFunSuite {

  test("Fields matching") {
    val expected = Set("status_code", "method", "session", "response_body", "message", "userId", "url", "request_name",
      "protocol", "response_headers", "request_body", "scenario", "request_headers")

    val result = parseHttpLog("simpleGet.txt")

    assert(result.keySet == expected)
  }

  test("Compare values - [get]") {
    val result = parseHttpLog("simpleGet.txt")

    softAssert(
      withClue("Request name: ")(result("request_name") should be("get request")),
      withClue("Message: ")(result("message") should be("OK")),
      withClue("Session: ")(result("session") should be("Session(Example scenario,1,Map(gatling.http.ssl.sslContexts -> io.gatling.http.util.SslContexts@434d148, gatling.http.cache.dns -> io.gatling.http.resolver.ShufflingNameResolver@105cb8b8, gatling.http.cache.baseUrl -> https://httpbin.org, identifier -> ),OK,List(),io.gatling.core.protocol.ProtocolComponentsRegistry$$Lambda$529/0x0000000800604840@1e8ad005,io.netty.channel.nio.NioEventLoop@60723d6a)")),
      withClue("Method: ")(result("method") should be("GET")),
      withClue("Request body: ")(result("request_body") should be("%empty%")),
      withClue("Request headers: ")(result("request_headers") should be("accept: */*\nhost: httpbin.org")),
      withClue("Url: ")(result("url") should be("https://httpbin.org/get")),
      withClue("Status code: ")(result("status_code") should be("200")),
      withClue("Response headers: ")(result("response_headers") should be("Date: Wed, 25 Aug 2021 08:31:38 GMT\nContent-Type: application/json\nConnection: keep-alive\nServer: gunicorn/19.9.0\nAccess-Control-Allow-Origin: *\nAccess-Control-Allow-Credentials: true\ncontent-length: 223")),
      withClue("Response body: ")(result("response_body") should be("{\n  \"args\": {},\n  \"headers\": {\n    \"Accept\": \"*/*\",\n    \"Host\": \"httpbin.org\",\n    \"X-Amzn-Trace-Id\": \"Root=1-6125ffea-3e25d40360dd3cc425c1a26f\"\n  },\n  \"origin\": \"2.2.2.2\",\n  \"url\": \"https://httpbin.org/get\"\n}\n")),
      withClue("Protocol: ")(result("protocol") should be("http")),
      withClue("Scenario: ")(result("scenario") should be("Example scenario")),
      withClue("UserId: ")(result("userId") should be("1")),
    )
  }

  test("Compare values - [post]") {
    val result = parseHttpLog("simplePost.txt")

    softAssert(
      withClue("Request name: ")(result("request_name") should be("post request")),
      withClue("Message: ")(result("message") should be("KO jsonPath($.origin).find.is(1.1.1.1), but actually found 2.2.2.2")),
      withClue("Session: ")(result("session") should be("Session(Example scenario,1,Map(gatling.http.ssl.sslContexts -> io.gatling.http.util.SslContexts@72dcbe5f, gatling.http.cache.dns -> io.gatling.http.resolver.ShufflingNameResolver@9646ea9, gatling.http.cache.baseUrl -> https://httpbin.org, identifier -> 123),KO,List(),io.gatling.core.protocol.ProtocolComponentsRegistry$$Lambda$558/0x000000080065b840@5205309,io.netty.channel.nio.NioEventLoop@3241713e)")),
      withClue("Method: ")(result("method") should be("POST")),
      withClue("Request body: ")(result("request_body") should be("StringChunksRequestBody{contentType='application/json', charset=UTF-8, content={\n  \"someKey\" : \"someValue\"\n}}")),
      withClue("Request headers: ")(result("request_headers") should be("accept: application/json\nhost: httpbin.org\ncontent-type: application/json\ncontent-length: 29")),
      withClue("Url: ")(result("url") should be("https://httpbin.org/post")),
      withClue("Status code: ")(result("status_code") should be("200")),
      withClue("Response headers: ")(result("response_headers") should be("Date: Fri, 27 Aug 2021 08:58:01 GMT\nContent-Type: application/json\nConnection: keep-alive\nServer: gunicorn/19.9.0\nAccess-Control-Allow-Origin: *\nAccess-Control-Allow-Credentials: true\ncontent-length: 433")),
      withClue("Response body: ")(result("response_body") should be("{\n  \"args\": {},\n  \"data\": \"{\\n  \\\"someKey\\\" : \\\"someValue\\\"\\n}\",\n  \"files\": {},\n  \"form\": {},\n  \"headers\": {\n    \"Accept\": \"application/json\",\n    \"Content-Length\": \"29\",\n    \"Content-Type\": \"application/json\",\n    \"Host\": \"httpbin.org\",\n    \"X-Amzn-Trace-Id\": \"Root=1-6128a919-7d81641a7210d573410c6592\"\n  },\n  \"json\": {\n    \"someKey\": \"someValue\"\n  },\n  \"origin\": \"2.2.2.2\",\n  \"url\": \"https://httpbin.org/post\"\n}\n")),
      withClue("Protocol: ")(result("protocol") should be("http")),
      withClue("Scenario: ")(result("scenario") should be("Example scenario")),
      withClue("UserId: ")(result("userId") should be("1")),
    )
  }

  test("Without response body") {
    val result = parseHttpLog("withoutResponse.txt")
    softAssert(
      withClue("Response body: ")(result("response_body") should be("%empty%")),
      withClue("Response headers: ")(result("response_headers") should be("%empty%")),
      withClue("Status code: ")(result("status_code") should be("%empty%"))
    )
  }

  test("Extract Session Attributes") {
    val result = parseHttpLog("simplePost.txt",
      Some("gatling.http.cache.baseUrl;gatling.http.cache.dns;identifier"))
    softAssert(
      withClue("Param[1] gatling.http.cache.baseUrl")(result("gatling.http.cache.baseUrl") should be("https://httpbin.org")),
      withClue("Param[2] gatling.http.cache.dns")(result("gatling.http.cache.dns") should be("io.gatling.http.resolver.ShufflingNameResolver@9646ea9")),
      withClue("Param[3] identifier")(result("identifier") should be("123")),
    )
  }

}
