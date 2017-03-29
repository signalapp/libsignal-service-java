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

public class DeviceContactsInputStream extends ChunkedInputStream {

  public DeviceContactsInputStream(InputStream in) {
    super(in);
  }

  public DeviceContact read() throws IOException {
    long   detailsLength     = readRawVarint32();
    byte[] detailsSerialized = new byte[(int)detailsLength];
    Util.readFully(in, detailsSerialized);

    SignalServiceProtos.ContactDetails      details = SignalServiceProtos.ContactDetails.parseFrom(detailsSerialized);
    String                                  number  = details.getNumber();
    Optional<String>                        name    = Optional.fromNullable(details.getName());
    Optional<SignalServiceAttachmentStream> avatar  = Optional.absent();
    Optional<String>                        color   = details.hasColor() ? Optional.of(details.getColor()) : Optional.<String>absent();

    if (details.hasAvatar()) {
      long        avatarLength      = details.getAvatar().getLength();
      InputStream avatarStream      = new LimitedInputStream(in, avatarLength);
      String      avatarContentType = details.getAvatar().getContentType();

      avatar = Optional.of(new SignalServiceAttachmentStream(avatarStream, avatarContentType, avatarLength, Optional.<String>absent(), null));
    }

    return new DeviceContact(number, name, avatar, color);
  }

}
