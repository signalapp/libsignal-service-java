package org.whispersystems.signalservice.api.util;

public interface CredentialsProvider {
  public String getUser();
  public String getPassword();
  public String getSignalingKey();
}
