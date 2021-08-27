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
                              settings: Settings, properties:
                                  ElasticsearchProperties,
                              headers: HttpRequestHeaders)
  extends AbstractElasticsearchPublisher[ILoggingEvent](context, errorReporter, settings, properties, headers) {

  override def buildPropertyAndEncoder(context: Context, property: Property): AbstractPropertyAndEncoder[ILoggingEvent] = {
    new ClassicPropertyAndEncoder(property, context)
  }

  override def serializeCommonFields(gen: JsonGenerator, event: ILoggingEvent): Unit = {
    gen.writeObjectField("@timestamp", getTimestamp(event.getTimeStamp))

    val fullMessage = event.getFormattedMessage

    val wsEvent = event.getLoggerName.contains("io.gatling.http.action.ws")

    if (!wsEvent && (event.getLevel == DEBUG || event.getLevel == TRACE)) GatlingLogParser.httpFields(gen, fullMessage)
    else if (settings.isRawJsonMessage) {
      gen.writeFieldName("message")
      gen.writeRawValue(fullMessage)
    }
    else {
      var formattedMessage = fullMessage
      if (settings.getMaxMessageSize > 0 && formattedMessage.length > settings.getMaxMessageSize)
        formattedMessage = formattedMessage.substring(0, settings.getMaxMessageSize) + ".."
      gen.writeObjectField("message", formattedMessage)
      if (wsEvent) gen.writeObjectField("protocol", "ws")
    }

    if (settings.isIncludeMdc) {
      for (entry <- event.getMDCPropertyMap.entrySet.asScala) {
        gen.writeObjectField(entry.getKey, entry.getValue)
      }
    }

  }

}
