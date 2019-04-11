/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SentTranscriptMessage {

  private final Optional<String>         destination;
  private final long                     timestamp;
  private final long                     expirationStartTimestamp;
  private final SignalServiceDataMessage message;
  private final Map<String, Boolean>     unidentifiedStatus;
  private final boolean                  isRecipientUpdate;

  public SentTranscriptMessage(String destination, long timestamp, SignalServiceDataMessage message,
                               long expirationStartTimestamp, Map<String, Boolean> unidentifiedStatus,
                               boolean isRecipientUpdate)
  {
    this.destination              = Optional.of(destination);
    this.timestamp                = timestamp;
    this.message                  = message;
    this.expirationStartTimestamp = expirationStartTimestamp;
    this.unidentifiedStatus       = new HashMap<>(unidentifiedStatus);
    this.isRecipientUpdate        = isRecipientUpdate;
  }

  public SentTranscriptMessage(long timestamp, SignalServiceDataMessage message) {
    this.destination              = Optional.absent();
    this.timestamp                = timestamp;
    this.message                  = message;
    this.expirationStartTimestamp = 0;
    this.unidentifiedStatus       = Collections.emptyMap();
    this.isRecipientUpdate        = false;
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

  public boolean isUnidentified(String destination) {
    if (unidentifiedStatus.containsKey(destination)) {
      return unidentifiedStatus.get(destination);
    }
    return false;
  }

  public Set<String> getRecipients() {
    return unidentifiedStatus.keySet();
  }

  public boolean isRecipientUpdate() {
    return isRecipientUpdate;
  }
}
