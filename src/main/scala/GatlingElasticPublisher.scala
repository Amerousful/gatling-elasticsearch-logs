import ch.qos.logback.classic.Level.{DEBUG, TRACE}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Context
import com.fasterxml.jackson.core.JsonGenerator
import com.internetitem.logback.elasticsearch.AbstractElasticsearchPublisher
import com.internetitem.logback.elasticsearch.AbstractElasticsearchPublisher.getTimestamp
import com.internetitem.logback.elasticsearch.config.{ElasticsearchProperties, HttpRequestHeaders, Property, Settings}
import com.internetitem.logback.elasticsearch.util.{AbstractPropertyAndEncoder, ClassicPropertyAndEncoder, ErrorReporter}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class GatlingElasticPublisher(context: Context,
                              errorReporter: ErrorReporter,
                              settings: Settings,
                              properties: ElasticsearchProperties,
                              headers: HttpRequestHeaders,
                              gatlingLogSettings: GatlingLogSettings)
  extends AbstractElasticsearchPublisher[ILoggingEvent](context, errorReporter, settings, properties, headers) {

  override def buildPropertyAndEncoder(context: Context, property: Property): AbstractPropertyAndEncoder[ILoggingEvent] = {
    new ClassicPropertyAndEncoder(property, context)
  }

  override def serializeCommonFields(gen: JsonGenerator, event: ILoggingEvent): Unit = {
    gen.writeObjectField("@timestamp", getTimestamp(event.getTimeStamp))

    val extractSessionAttributes = gatlingLogSettings.extractSessionAttributes

    val message = formatMessageBySize(event.getFormattedMessage)

    val wsEvent = event.getLoggerName.contains("io.gatling.http.action.ws")
    val httpEvent = if (gatlingLogSettings.excludeResources.getOrElse(false))
      event.getLoggerName.equals("io.gatling.http.engine.response.DefaultStatsProcessor")
    else event.getLoggerName.contains("io.gatling.http.engine.response")

    val levelCondition = event.getLevel == DEBUG || event.getLevel == TRACE

    if (httpEvent && levelCondition) {
      GatlingLogParser.httpFields(gen, message, extractSessionAttributes)
    }
    else if (wsEvent && levelCondition) {
      GatlingLogParser.wsFields(gen, message)
    }
    else if (settings.isRawJsonMessage) {
      gen.writeFieldName("message")
      gen.writeRawValue(event.getFormattedMessage)
    }

    if (settings.isIncludeMdc) {
      for (entry <- event.getMDCPropertyMap.entrySet.asScala) {
        gen.writeObjectField(entry.getKey, entry.getValue)
      }
    }

  }

  def formatMessageBySize(message: String): String = {
    if (settings.getMaxMessageSize > 0 && message.length > settings.getMaxMessageSize)
      message.substring(0, settings.getMaxMessageSize) + ".."
    else message
  }

}
