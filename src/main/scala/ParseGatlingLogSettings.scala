trait ParseGatlingLogSettings {

  val gatlingLogSettings: GatlingLogSettings = new GatlingLogSettings

  def setExtractSessionAttributes(extractSessionAttributes: String): Unit = {
    gatlingLogSettings.extractSessionAttributes = extractSessionAttributes
  }

}
