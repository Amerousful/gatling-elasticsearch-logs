import ch.qos.logback.classic.spi.ILoggingEvent
import com.internetitem.logback.elasticsearch.AbstractElasticsearchAppender

import scala.util.control.Breaks.break

class ElasticGatlingAppender extends AbstractElasticsearchAppender[ILoggingEvent] {

  override def buildElasticsearchPublisher(): GatlingElasticPublisher = {
    new GatlingElasticPublisher(getContext, errorReporter, settings, elasticsearchProperties, headers)
  }

  override def appendInternal(eventObject: ILoggingEvent): Unit = {
    val targetLogger = eventObject.getLoggerName

    val loggerName = settings.getLoggerName
    if (loggerName != null && loggerName == targetLogger) break()

    val errorLoggerName = settings.getErrorLoggerName
    if (errorLoggerName != null && errorLoggerName == targetLogger) break()

    eventObject.prepareForDeferredProcessing()
    if (settings.isIncludeCallerData) eventObject.getCallerData

    publishEvent(eventObject)
  }
}