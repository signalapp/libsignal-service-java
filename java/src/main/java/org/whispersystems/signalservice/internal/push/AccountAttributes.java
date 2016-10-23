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
  private boolean fetchesMessages;

  public AccountAttributes(String signalingKey, int registrationId, boolean voice, boolean fetchesMessages) {
    this.signalingKey    = signalingKey;
    this.registrationId  = registrationId;
    this.voice           = voice;
    this.fetchesMessages = fetchesMessages;
  }
  
  public AccountAttributes(String signalingKey, int registrationId, boolean voice) {
    this(signalingKey, registrationId, voice, false);
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
}
