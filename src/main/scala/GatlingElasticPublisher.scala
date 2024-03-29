import ch.qos.logback.classic.Level.{DEBUG, TRACE}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Context
import com.agido.logback.elasticsearch.util.ErrorReporter
import com.fasterxml.jackson.core.JsonGenerator
import com.agido.logback.elasticsearch.AbstractElasticsearchPublisher
import com.agido.logback.elasticsearch.AbstractElasticsearchPublisher.getTimestamp
import com.agido.logback.elasticsearch.config.{ElasticsearchProperties, HttpRequestHeaders, Property, Settings}
import com.agido.logback.elasticsearch.util.{AbstractPropertyAndEncoder, ClassicPropertyAndEncoder, ErrorReporter}

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
    val sessionHookEvent = event.getLoggerName.contains("io.gatling.core.action.builder.SessionHookBuilder")

    val levelCondition = event.getLevel == DEBUG || event.getLevel == TRACE

    (httpEvent, wsEvent, levelCondition, sessionHookEvent) match {
      case (true, false, true, false) => GatlingLogParser.httpFields(gen, message, extractSessionAttributes)
      case (false, true, true, false) => GatlingLogParser.wsFields(gen, message, extractSessionAttributes)
      case (false, false, false, true) => GatlingLogParser.sessionFields(gen, message, extractSessionAttributes)
      case _ => gen.writeObjectField("message", message)
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
