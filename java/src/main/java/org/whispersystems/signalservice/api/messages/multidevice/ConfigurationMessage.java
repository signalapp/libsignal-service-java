package org.whispersystems.signalservice.api.messages.multidevice;


import org.whispersystems.libsignal.util.guava.Optional;

public class ConfigurationMessage {

  private final Optional<Boolean> readReceipts;
  private final Optional<Boolean> unidentifiedDeliveryIndicators;
  private final Optional<Boolean> typingIndicators;

  public ConfigurationMessage(Optional<Boolean> readReceipts,
                              Optional<Boolean> unidentifiedDeliveryIndicators,
                              Optional<Boolean> typingIndicators)
  {
    this.readReceipts                   = readReceipts;
    this.unidentifiedDeliveryIndicators = unidentifiedDeliveryIndicators;
    this.typingIndicators               = typingIndicators;
  }

  public Optional<Boolean> getReadReceipts() {
    return readReceipts;
  }

  public Optional<Boolean> getUnidentifiedDeliveryIndicators() {
    return unidentifiedDeliveryIndicators;
  }

  public Optional<Boolean> getTypingIndicators() {
    return typingIndicators;
  }
}
