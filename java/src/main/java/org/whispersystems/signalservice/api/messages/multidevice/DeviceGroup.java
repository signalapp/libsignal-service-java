/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream;

import java.util.List;

public class DeviceGroup {

  private final byte[]                               id;
  private final Optional<String>                     name;
  private final List<String>                         members;
  private final Optional<SignalServiceAttachmentStream> avatar;
  private final boolean                              active;

  public DeviceGroup(byte[] id, Optional<String> name, List<String> members, Optional<SignalServiceAttachmentStream> avatar, boolean active) {
    this.id       = id;
    this.name     = name;
    this.members  = members;
    this.avatar   = avatar;
    this.active   = active;
  }

  public Optional<SignalServiceAttachmentStream> getAvatar() {
    return avatar;
  }

  public Optional<String> getName() {
    return name;
  }

  public byte[] getId() {
    return id;
  }

  public List<String> getMembers() {
    return members;
  }

  public boolean isActive() {
    return active;
  }
}
