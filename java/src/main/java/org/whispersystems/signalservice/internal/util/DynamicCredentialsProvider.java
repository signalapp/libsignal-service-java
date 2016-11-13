package org.whispersystems.signalservice.internal.util;

import org.whispersystems.signalservice.api.util.CredentialsProvider;

public class DynamicCredentialsProvider implements CredentialsProvider {

  private String user;
  private String password;
  private String signalingKey;
  private int deviceId;
  
  public DynamicCredentialsProvider(String user, String password, String signalingKey, int deviceId) {
    super();
    this.user = user;
    this.password = password;
    this.signalingKey = signalingKey;
    this.deviceId = deviceId;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSignalingKey() {
    return signalingKey;
  }

  public void setSignalingKey(String signalingKey) {
    this.signalingKey = signalingKey;
  }

  @Override
  public int getDeviceId() {
    return deviceId;
  }
  
  public void setDeviceId(int deviceId) {
    this.deviceId = deviceId;
  }
  
}
