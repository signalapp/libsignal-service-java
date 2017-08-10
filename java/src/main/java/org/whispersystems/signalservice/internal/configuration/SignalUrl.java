package org.whispersystems.signalservice.internal.configuration;


import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.internal.util.BlacklistingTrustManager;

import javax.net.ssl.TrustManager;

import okhttp3.ConnectionSpec;

public class SignalUrl {

  private final String                   url;
  private final Optional<String>         hostHeader;
  private final Optional<ConnectionSpec> connectionSpec;
  private       TrustManager[]           trustManagers;

  public SignalUrl(String url, TrustStore trustStore) {
    this(url, null, trustStore, null);
  }

  public SignalUrl(String url, String hostHeader,
                   TrustStore trustStore,
                   ConnectionSpec connectionSpec)
  {
    this.url            = url;
    this.hostHeader     = Optional.fromNullable(hostHeader);
    this.trustManagers  = BlacklistingTrustManager.createFor(trustStore);
    this.connectionSpec = Optional.fromNullable(connectionSpec);
  }


  public Optional<String> getHostHeader() {
    return hostHeader;
  }

  public String getUrl() {
    return url;
  }

  public TrustManager[] getTrustManagers() {
    return trustManagers;
  }

  public Optional<ConnectionSpec> getConnectionSpec() {
    return connectionSpec;
  }

}
