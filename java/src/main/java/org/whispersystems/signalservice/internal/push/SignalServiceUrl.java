package org.whispersystems.signalservice.internal.push;


import org.whispersystems.libsignal.util.guava.Optional;

public class SignalServiceUrl {

  private final Optional<String> hostHeader;
  private final String           url;

  public SignalServiceUrl(String url) {
    this(url, null);
  }

  public SignalServiceUrl(String url, String hostHeader) {
    this.url        = url;
    this.hostHeader = Optional.fromNullable(hostHeader);
  }


  public Optional<String> getHostHeader() {
    return hostHeader;
  }

  public String getUrl() {
    return url;
  }
}
