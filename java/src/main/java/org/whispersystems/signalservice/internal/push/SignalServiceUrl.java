package org.whispersystems.signalservice.internal.push;


import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.push.TrustStore;

public class SignalServiceUrl {

  private final String           url;
  private final Optional<String> hostHeader;
  private       TrustStore       trustStore;

  public SignalServiceUrl(String url, TrustStore trustStore) {
    this(url, null, trustStore);
  }

  public SignalServiceUrl(String url, String hostHeader, TrustStore trustStore) {
    this.url        = url;
    this.hostHeader = Optional.fromNullable(hostHeader);
    this.trustStore = trustStore;
  }


  public Optional<String> getHostHeader() {
    return hostHeader;
  }

  public String getUrl() {
    return url;
  }

  public TrustStore getTrustStore() {
    return trustStore;
  }
}
