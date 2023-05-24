/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.signalservice.api.messages.multidevice

import org.signal.core.models.ServiceId.ACI
import org.signal.core.util.readNBytesOrThrow
import org.signal.core.util.readVarInt32OrNull
import org.signal.core.util.skipNBytesOrThrow
import org.signal.core.util.stream.LimitedInputStream
import org.signal.core.util.stream.NonClosingInputStream
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentStream
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.internal.push.GroupDetails
import java.io.IOException
import java.io.InputStream
import java.util.Optional

/** Reads groups written by [DeviceGroupsWriter]. */
class DeviceGroupsReader(private val input: InputStream) {

  companion object {
    private const val MAX_DETAILS_LENGTH = 1024 * 1024
  }

  private var activeAvatarStream: LimitedInputStream? = null
  private var activeAvatarLength: Long = 0

  var skippedRecords: Int = 0
    private set

  fun groups(): Sequence<DeviceGroup> = sequence {
    while (true) {
      val group = try {
        readGroup()
      } catch (e: IOException) {
        skippedRecords++
        skipRemainingAvatar()
        continue
      } ?: break

      yield(group)
      skipRemainingAvatar()
    }
  }.constrainOnce()

  @Throws(IOException::class)
  private fun readGroup(): DeviceGroup? {
    val detailsLength = input.readVarInt32OrNull() ?: return null
    if (detailsLength !in 0..MAX_DETAILS_LENGTH) {
      throw IOException("Group details length out of range: $detailsLength")
    }

    val details = GroupDetails.ADAPTER.decode(input.readNBytesOrThrow(detailsLength))
    val avatarStream = openAvatar(details)
    val id = details.id?.toByteArray() ?: run {
      skipRemainingAvatar()
      throw IOException("ID missing on group record!")
    }

    val members = details.members.map { member ->
      val e164 = member.e164?.takeIf { it.isNotEmpty() }
        ?: throw IOException("Missing group member address!")
      SignalServiceAddress(ACI.UNKNOWN, e164)
    }

    val expirationTimer = details.expireTimer?.takeIf { it > 0 }
    val inboxPosition = details.inboxPosition

    return DeviceGroup(
      id,
      Optional.ofNullable(details.name),
      members,
      Optional.ofNullable(avatarStream),
      details.active ?: false,
      Optional.ofNullable(expirationTimer),
      Optional.ofNullable(details.color),
      details.blocked ?: false,
      Optional.ofNullable(inboxPosition),
      details.archived ?: false
    )
  }

  private fun openAvatar(details: GroupDetails): SignalServiceAttachmentStream? {
    val avatar = details.avatar ?: return null
    val length = avatar.length?.toLong() ?: return null
    if (length < 0) {
      throw IOException("Avatar length out of range: $length")
    }

    val stream = LimitedInputStream(NonClosingInputStream(input), length)
    activeAvatarStream = stream
    activeAvatarLength = length
    return SignalServiceAttachmentStream(
      stream,
      avatar.contentType ?: "image/*",
      length,
      Optional.empty(),
      false,
      false,
      false,
      false,
      null,
      null
    )
  }

  private fun skipRemainingAvatar() {
    val stream = activeAvatarStream ?: return
    stream.skipNBytesOrThrow(activeAvatarLength - stream.bytesRead)
    activeAvatarStream = null
    activeAvatarLength = 0
  }
}