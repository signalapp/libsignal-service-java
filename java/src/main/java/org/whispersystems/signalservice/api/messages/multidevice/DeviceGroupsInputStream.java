/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DeviceGroupsInputStream extends ChunkedInputStream{

  public DeviceGroupsInputStream(InputStream in) {
    super(in);
  }

  public DeviceGroup read() throws IOException {
    long   detailsLength     = readRawVarint32();
    byte[] detailsSerialized = new byte[(int)detailsLength];
    Util.readFully(in, detailsSerialized);

    SignalServiceProtos.GroupDetails details = SignalServiceProtos.GroupDetails.parseFrom(detailsSerialized);

    if (!details.hasId()) {
      throw new IOException("ID missing on group record!");
    }

    byte[]                                  id      = details.getId().toByteArray();
    Optional<String>                        name    = Optional.fromNullable(details.getName());
    List<String>                            members = details.getMembersList();
    Optional<SignalServiceAttachmentStream> avatar  = Optional.absent();
    boolean                                 active  = details.getActive();

    if (details.hasAvatar()) {
      long        avatarLength      = details.getAvatar().getLength();
      InputStream avatarStream      = new ChunkedInputStream.LimitedInputStream(in, avatarLength);
      String      avatarContentType = details.getAvatar().getContentType();

      avatar = Optional.of(new SignalServiceAttachmentStream(avatarStream, avatarContentType, avatarLength, Optional.<String>absent(), null));
    }

    return new DeviceGroup(id, name, members, avatar, active);
  }

}
