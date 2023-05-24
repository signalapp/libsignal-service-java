/*
 * Copyright (C) 2014-2018 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream;
import org.whispersystems.signalservice.api.push.ServiceId;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.internal.push.GroupDetails;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeviceGroupsInputStream extends ChunkedInputStream {

  public DeviceGroupsInputStream(InputStream in) {
    super(in);
  }

  public DeviceGroup read() throws IOException {
    long detailsLength = readRawVarint32();
    if (detailsLength == -1) {
      return null;
    }
    byte[] detailsSerialized = new byte[(int) detailsLength];
    Util.readFully(in, detailsSerialized);

    GroupDetails details = GroupDetails.ADAPTER.decode(detailsSerialized);

    if (details.id == null) {
      throw new IOException("ID missing on group record!");
    }

    byte[]                                  id              = details.id.toByteArray();
    Optional<String>                        name            = Optional.ofNullable(details.name);
    List<GroupDetails.Member>               members         = details.members;
    Optional<SignalServiceAttachmentStream> avatar          = Optional.empty();
    boolean                                 active          = details.active;
    Optional<Integer>                       expirationTimer = Optional.empty();
    Optional<String>                        color           = Optional.ofNullable(details.color);
    boolean                                 blocked         = details.blocked;
    Optional<Integer>                       inboxPosition   = Optional.empty();
    boolean                                 archived        = false;

    if (details.avatar != null && details.avatar.length != null) {
      long        avatarLength      = details.avatar.length;
      InputStream avatarStream      = new ChunkedInputStream.LimitedInputStream(in, avatarLength);
      String      avatarContentType = details.avatar.contentType;

      avatar = Optional.of(new SignalServiceAttachmentStream(avatarStream, avatarContentType, avatarLength, Optional.empty(), false, false, false, false, null, null));
    }

    if (details.expireTimer != null && details.expireTimer > 0) {
      expirationTimer = Optional.of(details.expireTimer);
    }

    List<SignalServiceAddress> addressMembers = new ArrayList<>(members.size());
    for (GroupDetails.Member member : members) {
      if (member.e164 != null && !member.e164.isEmpty()) {
        addressMembers.add(new SignalServiceAddress(ServiceId.ACI.UNKNOWN, member.e164));
      } else {
        throw new IOException("Missing group member address!");
      }
    }

    if (details.inboxPosition != null) {
      inboxPosition = Optional.of(details.inboxPosition);
    }

    if (details.archived != null) {
      archived = details.archived;
    }

    return new DeviceGroup(id, name, addressMembers, avatar, active, expirationTimer, color, blocked, inboxPosition, archived);
  }
}
