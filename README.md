# Gatling Elasticsearch Logs

Logger which parse raw Gatling logs and send them to the Elasticsearch.

## Motivation

By default, Gatling writes logs to the console, which is inconvenient for analysing, collecting and storing information.
In fact, metrics don't contain the details of errors, they only have request status OK or KO.
When a metric occurs with an error it's impossible to figure out what happened: a check failed? got 404? or it was 502? etc.
Also, if you run load tests in the distributed mode, it will store your logs in separate injectors.
This logger allows getting all useful information so that it will be possible to correlate with your metrics.

To recap, the Logger is solving two main problems:

- Distributed metrics sending and storing them
- You can build a Graph with errors details for correlation by metrics

## Install

### Maven:

Add to your `pom.xml`

```xml
<dependency>
  <groupId>io.github.amerousful</groupId>
  <artifactId>gatling-elasticsearch-logs</artifactId>
  <version>1.0</version>
</dependency>
```

### SBT

Add to your `build.sbt`

```scala
libraryDependencies += "io.github.amerousful" % "gatling-elasticsearch-logs" % "1.0"
```

## How to configure `logback.xml`

I provide minimal configuration, but you can add additional things what you need

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="ELASTIC" class="ElasticGatlingAppender">
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>${logLevel}</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <url>http://${elkUrl}/_bulk</url>
        <index>gatling-%date{yyyy.MM.dd}</index>
        <type>gatling</type>
        <errorsToStderr>true</errorsToStderr>

        <headers>
            <header>
                <name>Content-Type</name>
                <value>application/json</value>
            </header>
        </headers>
    </appender>

    <logger name="io.gatling.http.engine.response" level="${logLevel}"/>

    <appender name="ASYNC ELK" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="ELASTIC"/>
    </appender>

    <root level="WARN">
        <appender-ref ref="ASYNC ELK"/>
    </root>

</configuration>
```
***
This logger based on https://github.com/internetitem/logback-elasticsearch-appender which is directly responsible for sending to Elasticsearch.
There you can find some addition and useful options related with sending
***

Pay attention on two variables in config:

`elkUrl` - URL of your Elasticsearch

`logLevel` - log's level. **DEBUG** to log all failed HTTP requests. **TRACE** to log all HTTP requests

Example how to pass the above variable during the run load test:

```shell
mvn gatling:test -DelkUrl=%URL%:%PORT% -DlogLevel=%LEVEL%
```

### Parse Session

Logger can also parse Session attributes and send them to Elasticsearch.
As an example, your test might contain some entity id: userId, serverId, etc. It's useful for filtering data.
Here is what you need to add to the appender:

```xml
<appender name="ELASTIC" class="ElasticGatlingAppender">

    <extractSessionAttributes>userId;serverId</extractSessionAttributes>

</appender>
```

In my case, I add `simulation` name to Session. 
Although metrics writes by **simulation** name, logs contain only **class** name, and you can't match them.
During several simultaneously running tests, you will get mixed logs which we can filter by `simulation` name.

Example how to override `scenario` method and write `simulation` to Session:
```scala

class Example extends Simulation {
  
  override def scenario(name: String): ScenarioBuilder = {
    import io.gatling.commons.util.StringHelper._


    val simulationName = RichString(this.getClass.getSimpleName).clean

    io.gatling.core.Predef.scenario(name)
      .exec(_.set("simulation", simulationName))
  }

  val scn: ScenarioBuilder = scenario("Example scenario")
    .exec(request)
    ...
}
```

***

Exclude logs from failed resources in silent mode (`NoopStatsProcessor`):
```xml
 <excludeResources>true</excludeResources>
```

***

## How it works

The principle of works is to parse logs and then separate them by necessary fields. Currently, the Logger supports only two protocols :

- HTTP
- WebSocket


Example of how the Logger parsing a raw log by fields:

### Raw log:
```text
>>>>>>>>>>>>>>>>>>>>>>>>>>
Request:
get request: OK
=========================
Session:
Session(Example scenario,1,Map(gatling.http.ssl.sslContexts -> io.gatling.http.util.SslContexts@434d148, gatling.http.cache.dns -> io.gatling.http.resolver.ShufflingNameResolver@105cb8b8, gatling.http.cache.baseUrl -> https://httpbin.org, identifier -> ),OK,List(),io.gatling.core.protocol.ProtocolComponentsRegistry$$Lambda$529/0x0000000800604840@1e8ad005,io.netty.channel.nio.NioEventLoop@60723d6a)
=========================
HTTP request:
GET https://httpbin.org/get
headers:
   accept: */*
   host: httpbin.org
=========================
HTTP response:
status:
   200 OK
headers:
   Date: Wed, 25 Aug 2021 08:31:38 GMT
   Content-Type: application/json
   Connection: keep-alive
   Server: gunicorn/19.9.0
   Access-Control-Allow-Origin: *
   Access-Control-Allow-Credentials: true
   content-length: 223

body:
{
  "args": {},
  "headers": {
    "Accept": "*/*",
    "Host": "httpbin.org",
    "X-Amzn-Trace-Id": "Root=1-6125ffea-3e25d40360dd3cc425c1a26f"
  },
  "origin": "2.2.2.2",
  "url": "https://httpbin.org/get"
}

<<<<<<<<<<<<<<<<<<<<<<<<<
```

### Result:

| Field name       | Value                                                                                                                                                                                                                                                                                                                                                                                                            |
|:-----------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| request_name     | get request                                                                                                                                                                                                                                                                                                                                                                                                      |
| message          | OK                                                                                                                                                                                                                                                                                                                                                                                                               |
| session          | Session(Example scenario,1,Map(gatling.http.ssl.sslContexts -> io.gatling.http.util.SslContexts@434d148, gatling.http.cache.dns -> io.gatling.http.resolver.ShufflingNameResolver@105cb8b8, gatling.http.cache.baseUrl -> https://httpbin.org, identifier -> ),OK,List(),io.gatling.core.protocol.ProtocolComponentsRegistry$$Lambda$529/0x0000000800604840@1e8ad005,io.netty.channel.nio.NioEventLoop@60723d6a) | 
| method           | GET                                                                                                                                                                                                                                                                                                                                                                                                              |
| request_body     | %empty%                                                                                                                                                                                                                                                                                                                                                                                                          |
| request_headers  | accept: \*/\* <br /> host: httpbin.org                                                                                                                                                                                                                                                                                                                                                                           |
| url              | https://httpbin.org/get                                                                                                                                                                                                                                                                                                                                                                                          |
| status_code      | 200                                                                                                                                                                                                                                                                                                                                                                                                              |
| response_headers | Date: Wed, 25 Aug 2021 08:31:38 GMT<br />Content-Type: application/json<br />Connection: keep-alive<br />Server: gunicorn/19.9.0<br />Access-Control-Allow-Origin: *<br />Access-Control-Allow-Credentials: true<br />content-length: 223                                                                                                                                                                        |
| response_body    | {<br>&nbsp;"args": {},<br>&nbsp;"headers": {<br>&nbsp;&nbsp;"Accept": "\*/\*",<br>&nbsp;&nbsp;"Host": "httpbin.org",<br>&nbsp;&nbsp;"X-Amzn-Trace-Id": "Root=1-6125ffea-3e25d40360dd3cc425c1a26f"<br>&nbsp;},<br>&nbsp;"origin": "2.2.2.2",<br>&nbsp;"url": "https://httpbin.org/get" <br>}                                                                                                                      |
| protocol         | http                                                                                                                                                                                                                                                                                                                                                                                                             |
| scenario         | Example scenario                                                                                                                                                                                                                                                                                                                                                                                                 |
| userId           | 1                                                                                                                                                                                                                                                                                                                                                                                                                |


## Grafana

Integration Elasticsearch with Grafana. You can find addition information here

- https://grafana.com/docs/grafana/latest/datasources/elasticsearch/

Example of a graph which based on these logs
![Grafana example](https://user-images.githubusercontent.com/22199881/187403299-90f42a4b-360c-48b4-8bb9-7b6cd9152201.png)







## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License

[MIT](https://choosealicense.com/licenses/mit/)