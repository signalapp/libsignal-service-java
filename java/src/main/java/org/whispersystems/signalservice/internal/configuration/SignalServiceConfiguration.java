package org.whispersystems.signalservice.internal.configuration;


public class SignalServiceConfiguration {

  private final SignalServiceUrl[] signalServiceUrls;
  private final SignalCdnUrl[]     signalCdnUrls;


  public SignalServiceConfiguration(SignalServiceUrl[] signalServiceUrls, SignalCdnUrl[] signalCdnUrls) {
    this.signalServiceUrls = signalServiceUrls;
    this.signalCdnUrls     = signalCdnUrls;
  }

  public SignalServiceUrl[] getSignalServiceUrls() {
    return signalServiceUrls;
  }

  public SignalCdnUrl[] getSignalCdnUrls() {
    return signalCdnUrls;
  }
}
