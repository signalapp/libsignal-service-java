package org.whispersystems.signalservice.api.messages.multidevice;

public class ViewOnceOpenMessage {

  private final String sender;
  private final long   timestamp;

  public ViewOnceOpenMessage(String sender, long timestamp) {
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
