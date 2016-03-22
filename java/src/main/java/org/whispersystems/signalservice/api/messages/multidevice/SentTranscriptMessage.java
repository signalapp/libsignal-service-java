package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

public class SentTranscriptMessage {

  private final Optional<String>      destination;
  private final long                  timestamp;
  private final SignalServiceDataMessage message;

  public SentTranscriptMessage(String destination, long timestamp, SignalServiceDataMessage message) {
    this.destination = Optional.of(destination);
    this.timestamp   = timestamp;
    this.message     = message;
  }

  public SentTranscriptMessage(long timestamp, SignalServiceDataMessage message) {
    this.destination = Optional.absent();
    this.timestamp   = timestamp;
    this.message     = message;
  }

  public Optional<String> getDestination() {
    return destination;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SignalServiceDataMessage getMessage() {
    return message;
  }
}
