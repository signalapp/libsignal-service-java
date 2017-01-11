package org.whispersystems.signalservice.internal.push;


import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.push.TrustStore;

import okhttp3.ConnectionSpec;

public class SignalServiceUrl {

  private final String                   url;
  private final Optional<String>         hostHeader;
  private final Optional<ConnectionSpec> connectionSpec;
  private       TrustStore               trustStore;

  public SignalServiceUrl(String url, TrustStore trustStore) {
    this(url, null, trustStore, null);
  }

  public SignalServiceUrl(String url, String hostHeader,
                          TrustStore trustStore,
                          ConnectionSpec connectionSpec)
  {
    this.url            = url;
    this.hostHeader     = Optional.fromNullable(hostHeader);
    this.trustStore     = trustStore;
    this.connectionSpec = Optional.fromNullable(connectionSpec);
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

  public Optional<ConnectionSpec> getConnectionSpec() {
    return connectionSpec;
  }
}
