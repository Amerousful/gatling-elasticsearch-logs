trait ParseGatlingLogSettings {

  val gatlingLogSettings: GatlingLogSettings = new GatlingLogSettings

  def setExtractSessionAttributes(extractSessionAttributes: Option[String]): Unit = {
    gatlingLogSettings.extractSessionAttributes = extractSessionAttributes
  }

}
