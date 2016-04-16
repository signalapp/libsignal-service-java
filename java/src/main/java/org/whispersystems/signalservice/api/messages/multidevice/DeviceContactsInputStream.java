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
    int detailsLength = readRawVarint32();
    if (detailsLength == -1) {
      return null;
    }
    byte[] detailsSerialized = new byte[detailsLength];
    Util.readFully(in, detailsSerialized);

    SignalServiceProtos.ContactDetails      details = SignalServiceProtos.ContactDetails.parseFrom(detailsSerialized);
    String                                  number  = details.getNumber();
    Optional<String>                        name    = Optional.fromNullable(details.getName());
    Optional<SignalServiceAttachmentStream> avatar  = Optional.absent();

    if (details.hasAvatar()) {
      long        avatarLength      = details.getAvatar().getLength();
      InputStream avatarStream      = new LimitedInputStream(in, avatarLength);
      String      avatarContentType = details.getAvatar().getContentType();

      avatar = Optional.of(new SignalServiceAttachmentStream(avatarStream, avatarContentType, avatarLength, null));
    }

    return new DeviceContact(number, name, avatar);
  }

}
