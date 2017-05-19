package org.whispersystems.signalservice.api.push;


import com.fasterxml.jackson.annotation.JsonProperty;

public class SignalServiceProfile {

  @JsonProperty
  private String identityKey;

  public SignalServiceProfile() {}

  public String getIdentityKey() {
    return identityKey;
  }
}
