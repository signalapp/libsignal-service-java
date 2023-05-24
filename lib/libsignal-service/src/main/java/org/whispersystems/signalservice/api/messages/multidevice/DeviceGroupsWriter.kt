/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.signalservice.api.messages.multidevice

import okio.ByteString
import org.signal.core.util.writeVarInt32
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.internal.push.GroupDetails
import java.io.IOException
import java.io.OutputStream

/** Writes groups using a varint length, a [GroupDetails] proto, then raw avatar bytes. */
class DeviceGroupsWriter(private val out: OutputStream) {

  @Throws(IOException::class)
  fun write(group: DeviceGroup) {
    val avatar = group.avatar.orElse(null)
    val avatarBytes = avatar?.inputStream?.use { it.readBytes() } ?: ByteArray(0)
    val details = groupDetails(group, avatar, avatarBytes.size.takeIf { it > 0 }).encode()

    out.writeVarInt32(details.size)
    out.write(details)
    out.write(avatarBytes)
  }

  @Throws(IOException::class)
  fun close() {
    out.close()
  }

  private fun groupDetails(
    group: DeviceGroup,
    avatar: SignalServiceAttachmentStream?,
    avatarLength: Int?
  ): GroupDetails {
    val members = mutableListOf<GroupDetails.Member>()
    val membersE164 = mutableListOf<String>()

    for (address: SignalServiceAddress in group.members) {
      address.number.ifPresent { e164 ->
        membersE164 += e164
        members += GroupDetails.Member.Builder().e164(e164).build()
      }
    }

    val details = GroupDetails.Builder()
      .id(ByteString.of(*group.id))
      .members(members)
      .membersE164(membersE164)
      .active(group.isActive)
      .blocked(group.isBlocked)
      .archived(group.isArchived)

    group.name.ifPresent(details::name)
    group.expirationTimer.ifPresent(details::expireTimer)
    group.color.ifPresent(details::color)
    group.inboxPosition.ifPresent(details::inboxPosition)

    if (avatar != null && avatarLength != null) {
      details.avatar(
        GroupDetails.Avatar.Builder()
          .contentType(avatar.contentType)
          .length(avatarLength)
          .build()
      )
    }

    return details.build()
  }
}