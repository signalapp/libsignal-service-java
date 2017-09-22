package org.whispersystems.signalservice.api.messages.multidevice;


import org.whispersystems.libsignal.util.guava.Optional;

public class ConfigurationMessage {

  private final Optional<Boolean> readReceipts;

  public ConfigurationMessage(Optional<Boolean> readReceipts) {
    this.readReceipts = readReceipts;
  }

  public Optional<Boolean> getReadReceipts() {
    return readReceipts;
  }
}
