package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream;

public class DeviceContact {

  private final String                               number;
  private final Optional<String>                     name;
  private final Optional<SignalServiceAttachmentStream> avatar;

  public DeviceContact(String number, Optional<String> name, Optional<SignalServiceAttachmentStream> avatar) {
    this.number = number;
    this.name   = name;
    this.avatar = avatar;
  }

  public Optional<SignalServiceAttachmentStream> getAvatar() {
    return avatar;
  }

  public Optional<String> getName() {
    return name;
  }

  public String getNumber() {
    return number;
  }

}
