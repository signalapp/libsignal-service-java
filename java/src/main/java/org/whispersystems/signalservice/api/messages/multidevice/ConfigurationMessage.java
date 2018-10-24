package org.whispersystems.signalservice.api.messages.multidevice;


import org.whispersystems.libsignal.util.guava.Optional;

public class ConfigurationMessage {

  private final Optional<Boolean> readReceipts;
  private final Optional<Boolean> unidentifiedDeliveryIndicators;

  public ConfigurationMessage(Optional<Boolean> readReceipts,
                              Optional<Boolean> unidentifiedDeliveryIndicators)
  {
    this.readReceipts                   = readReceipts;
    this.unidentifiedDeliveryIndicators = unidentifiedDeliveryIndicators;
  }

  public Optional<Boolean> getReadReceipts() {
    return readReceipts;
  }

  public Optional<Boolean> getUnidentifiedDeliveryIndicators() {
    return unidentifiedDeliveryIndicators;
  }
}
