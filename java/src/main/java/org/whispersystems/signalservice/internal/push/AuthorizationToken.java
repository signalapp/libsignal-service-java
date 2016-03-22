package org.whispersystems.signalservice.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationToken {

  @JsonProperty
  private String token;

  public String getToken() {
    return token;
  }
}
