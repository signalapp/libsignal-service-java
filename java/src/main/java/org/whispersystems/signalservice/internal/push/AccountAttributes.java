/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountAttributes {

  @JsonProperty
  private String  signalingKey;

  @JsonProperty
  private int     registrationId;

  @JsonProperty
  private boolean voice;

  @JsonProperty
  private boolean video;

  public AccountAttributes(String signalingKey, int registrationId, boolean voice, boolean video) {
    this.signalingKey   = signalingKey;
    this.registrationId = registrationId;
    this.voice          = voice;
    this.video          = video;
  }

  public AccountAttributes() {}

  public String getSignalingKey() {
    return signalingKey;
  }

  public int getRegistrationId() {
    return registrationId;
  }

  public boolean isVoice() {
    return voice;
  }

  public boolean isVideo() {
    return video;
  }
}
