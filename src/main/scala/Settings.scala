trait Settings {

  val gatlingLogSettings: GatlingLogSettings = new GatlingLogSettings

  def setExtractSessionAttributes(extractSessionAttributes: String): Unit = {
    gatlingLogSettings.extractSessionAttributes = Some(extractSessionAttributes)
  }

}
