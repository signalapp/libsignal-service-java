/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.internal.push.GroupDetails;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okio.ByteString;

public class DeviceGroupsOutputStream extends ChunkedOutputStream {

  public DeviceGroupsOutputStream(OutputStream out) {
    super(out);
  }

  public void write(DeviceGroup group) throws IOException {
    writeGroupDetails(group);
    writeAvatarImage(group);
  }

  public void close() throws IOException {
    out.close();
  }

  private void writeAvatarImage(DeviceGroup contact) throws IOException {
    if (contact.getAvatar().isPresent()) {
      writeStream(contact.getAvatar().get().getInputStream());
    }
  }

  private void writeGroupDetails(DeviceGroup group) throws IOException {
    GroupDetails.Builder groupDetails = new GroupDetails.Builder();
    groupDetails.id(ByteString.of(group.getId()));

    if (group.getName().isPresent()) {
      groupDetails.name(group.getName().get());
    }

    if (group.getAvatar().isPresent()) {
      GroupDetails.Avatar.Builder avatarBuilder = new GroupDetails.Avatar.Builder();
      avatarBuilder.contentType(group.getAvatar().get().getContentType());
      avatarBuilder.length((int)group.getAvatar().get().getLength());
      groupDetails.avatar(avatarBuilder.build());
    }

    if (group.getExpirationTimer().isPresent()) {
      groupDetails.expireTimer(group.getExpirationTimer().get());
    }

    if (group.getColor().isPresent()) {
      groupDetails.color(group.getColor().get());
    }

    List<GroupDetails.Member> members     = new ArrayList<>(group.getMembers().size());
    List<String>              membersE164 = new ArrayList<>(group.getMembers().size());

    for (SignalServiceAddress address : group.getMembers()) {
      if (address.getNumber().isPresent()) {
        membersE164.add(address.getNumber().get());

        GroupDetails.Member.Builder builder = new GroupDetails.Member.Builder();
        builder.e164(address.getNumber().get());
        members.add(builder.build());
      }
    }

    groupDetails.members(members);
    groupDetails.membersE164(membersE164);
    groupDetails.active(group.isActive());
    groupDetails.blocked(group.isBlocked());
    groupDetails.archived(group.isArchived());

    if (group.getInboxPosition().isPresent()) {
      groupDetails.inboxPosition(group.getInboxPosition().get());
    }

    byte[] serializedContactDetails = groupDetails.build().encode();

    writeVarint32(serializedContactDetails.length);
    out.write(serializedContactDetails);
  }


}
