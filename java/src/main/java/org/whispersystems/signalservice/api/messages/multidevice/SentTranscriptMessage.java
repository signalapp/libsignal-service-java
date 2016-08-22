/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

public class SentTranscriptMessage {

  private final Optional<String>         destination;
  private final long                     timestamp;
  private final long                     expirationStartTimestamp;
  private final SignalServiceDataMessage message;

  public SentTranscriptMessage(String destination, long timestamp, SignalServiceDataMessage message, long expirationStartTimestamp) {
    this.destination              = Optional.of(destination);
    this.timestamp                = timestamp;
    this.message                  = message;
    this.expirationStartTimestamp = expirationStartTimestamp;
  }

  public SentTranscriptMessage(long timestamp, SignalServiceDataMessage message) {
    this.destination              = Optional.absent();
    this.timestamp                = timestamp;
    this.message                  = message;
    this.expirationStartTimestamp = 0;
  }

  public Optional<String> getDestination() {
    return destination;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getExpirationStartTimestamp() {
    return expirationStartTimestamp;
  }

  public SignalServiceDataMessage getMessage() {
    return message;
  }
}
