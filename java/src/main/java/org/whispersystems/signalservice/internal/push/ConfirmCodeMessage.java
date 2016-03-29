package org.whispersystems.signalservice.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfirmCodeMessage {
  
  @JsonProperty
  private String signalingKey;
  
  @JsonProperty
  private boolean supportsSms;
  
  @JsonProperty
  private boolean fetchesMessages;
  
  @JsonProperty
  private int registrationId;
  
  @JsonProperty
  private String name;
  
  public ConfirmCodeMessage(String signalingKey, boolean supportsSms, boolean fetchesMessages, int registrationId,
      String name) {
    super();
    this.signalingKey = signalingKey;
    this.supportsSms = supportsSms;
    this.fetchesMessages = fetchesMessages;
    this.registrationId = registrationId;
    this.name = name;
  }

}
