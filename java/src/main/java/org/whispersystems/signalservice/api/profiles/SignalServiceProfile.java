package org.whispersystems.signalservice.api.profiles;


import com.fasterxml.jackson.annotation.JsonProperty;

public class SignalServiceProfile {

  @JsonProperty
  private String identityKey;

  @JsonProperty
  private String name;

  @JsonProperty
  private String avatar;

  public SignalServiceProfile() {}

  public String getIdentityKey() {
    return identityKey;
  }

  public String getName() {
    return name;
  }

  public String getAvatar() {
    return avatar;
  }
}
