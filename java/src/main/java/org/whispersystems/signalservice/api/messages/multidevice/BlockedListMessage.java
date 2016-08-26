package org.whispersystems.signalservice.api.messages.multidevice;

import java.util.List;

public class BlockedListMessage {

  private final List<String> numbers;

  public BlockedListMessage(List<String> numbers) {
    this.numbers = numbers;
  }

  public List<String> getNumbers() {
    return numbers;
  }
}
