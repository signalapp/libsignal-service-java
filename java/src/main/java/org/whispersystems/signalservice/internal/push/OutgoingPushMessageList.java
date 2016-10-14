/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OutgoingPushMessageList {

  @JsonProperty
  private String destination;

  @JsonProperty
  private String relay;

  @JsonProperty
  private long timestamp;

  @JsonProperty
  private List<OutgoingPushMessage> messages;

  public OutgoingPushMessageList(String destination, long timestamp, String relay,
                                 List<OutgoingPushMessage> messages)
  {
    this.timestamp   = timestamp;
    this.destination = destination;
    this.relay       = relay;
    this.messages    = messages;
  }

  public String getDestination() {
    return destination;
  }

  public List<OutgoingPushMessage> getMessages() {
    return messages;
  }

  public String getRelay() {
    return relay;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
