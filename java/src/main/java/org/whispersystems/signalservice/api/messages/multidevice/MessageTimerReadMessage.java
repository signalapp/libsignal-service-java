package org.whispersystems.signalservice.api.messages.multidevice;

public class MessageTimerReadMessage {

  private final String sender;
  private final long   timestamp;

  public MessageTimerReadMessage(String sender, long timestamp) {
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
