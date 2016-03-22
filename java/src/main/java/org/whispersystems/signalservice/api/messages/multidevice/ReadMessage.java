package org.whispersystems.signalservice.api.messages.multidevice;

import java.util.LinkedList;
import java.util.List;

public class ReadMessage {

  private final String sender;
  private final long   timestamp;

  public ReadMessage(String sender, long timestamp) {
    this.sender    = sender;
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getSender() {
    return sender;
  }

}
