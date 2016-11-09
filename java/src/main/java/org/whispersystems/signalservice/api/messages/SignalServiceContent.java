/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.calls.SignalServiceCallMessage;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;

public class SignalServiceContent {

  private final Optional<SignalServiceDataMessage> message;
  private final Optional<SignalServiceSyncMessage> synchronizeMessage;
  private final Optional<SignalServiceCallMessage> callMessage;

  public SignalServiceContent() {
    this.message            = Optional.absent();
    this.synchronizeMessage = Optional.absent();
    this.callMessage        = Optional.absent();
  }

  public SignalServiceContent(SignalServiceDataMessage message) {
    this.message            = Optional.fromNullable(message);
    this.synchronizeMessage = Optional.absent();
    this.callMessage        = Optional.absent();
  }

  public SignalServiceContent(SignalServiceSyncMessage synchronizeMessage) {
    this.message            = Optional.absent();
    this.synchronizeMessage = Optional.fromNullable(synchronizeMessage);
    this.callMessage        = Optional.absent();
  }

  public SignalServiceContent(SignalServiceCallMessage callMessage) {
    this.message            = Optional.absent();
    this.synchronizeMessage = Optional.absent();
    this.callMessage        = Optional.of(callMessage);
  }

  public Optional<SignalServiceDataMessage> getDataMessage() {
    return message;
  }

  public Optional<SignalServiceSyncMessage> getSyncMessage() {
    return synchronizeMessage;
  }

  public Optional<SignalServiceCallMessage> getCallMessage() {
    return callMessage;
  }
}
