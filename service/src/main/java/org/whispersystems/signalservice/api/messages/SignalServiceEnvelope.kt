/*
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package org.whispersystems.signalservice.api.messages

import okio.ByteString
import org.signal.core.util.Base64
import org.whispersystems.signalservice.api.push.ServiceId
import org.whispersystems.signalservice.api.push.ServiceId.Companion.parseOrNull
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.internal.push.Envelope
import org.whispersystems.signalservice.internal.push.Envelope.Type.Companion.fromValue
import org.whispersystems.signalservice.internal.serialize.protos.SignalServiceEnvelopeProto
import java.io.IOException
import java.lang.AssertionError
import java.util.Optional

/**
 * This class represents an encrypted Signal Service envelope.
 *
 *
 * The envelope contains the wrapping information, such as the sender, the
 * message timestamp, the encrypted message type, etc.
 *
 * @author Moxie Marlinspike
 */
class SignalServiceEnvelope {
  val proto: Envelope

  /**
   * @return The server timestamp of when the envelope was delivered to us.
   */
  val serverDeliveredTimestamp: Long

  /**
   * Construct an envelope from a serialized, Base64 encoded SignalServiceEnvelope, encrypted
   * with a signaling key.
   *
   * @param message The serialized SignalServiceEnvelope, base64 encoded and encrypted.
   */
  constructor(message: String, serverDeliveredTimestamp: Long) : this(
    Base64.decode(message),
    serverDeliveredTimestamp
  )

  /**
   * Construct an envelope from a serialized SignalServiceEnvelope, encrypted with a signaling key.
   *
   * @param input The serialized and (optionally) encrypted SignalServiceEnvelope.
   */
  constructor(input: ByteArray, serverDeliveredTimestamp: Long) : this(
    Envelope.ADAPTER.decode(input),
    serverDeliveredTimestamp
  )

  constructor(envelope: Envelope, serverDeliveredTimestamp: Long) {
    proto = envelope
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
  }

  constructor(
    type: Int?,
    sender: Optional<SignalServiceAddress>,
    senderDevice: Int?,
    timestamp: Long?,
    content: ByteArray?,
    serverReceivedTimestamp: Long?,
    serverDeliveredTimestamp: Long?,
    uuid: String?,
    destinationServiceId: String?,
    urgent: Boolean,
    story: Boolean,
    reportingToken: ByteArray?,
    updatedPni: String?
  ) {
    val builder = Envelope.Builder()
      .type(type?.let { fromValue(it) })
      .sourceDevice(senderDevice)
      .timestamp(timestamp)
      .serverTimestamp(serverReceivedTimestamp)
      .destinationServiceId(destinationServiceId)
      .urgent(urgent)
      .updatedPni(updatedPni)
      .story(story)
    if (sender.isPresent) {
      builder.sourceServiceId(sender.get().serviceId.toString())
    }
    if (uuid != null) {
      builder.serverGuid(uuid)
    }
    if (content != null) {
      builder.content(ByteString.of(*content))
    }
    if (reportingToken != null) {
      builder.reportingToken(ByteString.of(*reportingToken))
    }
    proto = builder.build()
    this.serverDeliveredTimestamp = serverDeliveredTimestamp ?: 0
  }

  constructor(
    type: Int,
    timestamp: Long,
    content: ByteArray?,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    uuid: String?,
    destinationServiceId: String?,
    urgent: Boolean,
    story: Boolean,
    reportingToken: ByteArray?,
    updatedPni: String?
  ) {
    val builder = Envelope.Builder()
      .type(fromValue(type))
      .timestamp(timestamp)
      .serverTimestamp(serverReceivedTimestamp)
      .destinationServiceId(destinationServiceId)
      .urgent(urgent)
      .updatedPni(updatedPni)
      .story(story)
    if (uuid != null) {
      builder.serverGuid(uuid)
    }
    if (content != null) {
      builder.content(ByteString.of(*content))
    }
    if (reportingToken != null) {
      builder.reportingToken(ByteString.of(*reportingToken))
    }
    proto = builder.build()
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
  }

  val serverGuid: String?
    get() = proto.serverGuid

  fun hasServerGuid(): Boolean {
    return proto.serverGuid != null
  }

  val sourceServiceId: Optional<String>
    /**
     * @return The envelope's sender as a UUID.
     */
    get() = Optional.ofNullable(proto.sourceServiceId)

  fun hasSourceDevice(): Boolean {
    return proto.sourceDevice != null
  }

  val sourceDevice: Int
    /**
     * @return The envelope's sender device ID.
     */
    get() = proto.sourceDevice ?: 0
  val type: Int?
    /**
     * @return The envelope content type.
     */
    get() = proto.type?.value
  val timestamp: Long
    /**
     * @return The timestamp this envelope was sent.
     */
    get() = proto.timestamp ?: 0
  val serverReceivedTimestamp: Long
    /**
     * @return The server timestamp of when the server received the envelope.
     */
    get() = proto.serverTimestamp ?: 0

  /**
   * @return Whether the envelope contains an encrypted SignalServiceContent
   */
  fun hasContent(): Boolean {
    return proto.content != null
  }

  val content: ByteArray?
    /**
     * @return The envelope's encrypted SignalServiceContent.
     */
    get() = proto.content?.toByteArray()
  val isSignalMessage: Boolean
    /**
     * @return true if the containing message is a [org.signal.libsignal.protocol.message.SignalMessage]
     */
    get() = proto.type === Envelope.Type.CIPHERTEXT
  val isPreKeySignalMessage: Boolean
    /**
     * @return true if the containing message is a [org.signal.libsignal.protocol.message.PreKeySignalMessage]
     */
    get() = proto.type === Envelope.Type.PREKEY_BUNDLE
  val isReceipt: Boolean
    /**
     * @return true if the containing message is a delivery receipt.
     */
    get() = proto.type === Envelope.Type.RECEIPT
  val isUnidentifiedSender: Boolean
    get() = proto.type === Envelope.Type.UNIDENTIFIED_SENDER
  val isPlaintextContent: Boolean
    get() = proto.type === Envelope.Type.PLAINTEXT_CONTENT

  val destinationServiceId: ServiceId?
    get() = parseOrNull(proto.destinationServiceId)
  val isUrgent: Boolean
    get() = proto.urgent != null && proto.urgent

  val updatedPni: String?
    get() = proto.updatedPni
  val isStory: Boolean
    get() = proto.story != null && proto.story

  fun hasReportingToken(): Boolean {
    return proto.reportingToken != null
  }

  val reportingToken: ByteArray?
    get() = proto.reportingToken?.toByteArray()

  private fun serializeToProto(): SignalServiceEnvelopeProto.Builder {
    val builder = SignalServiceEnvelopeProto.Builder()
      .type(type)
      .deviceId(sourceDevice)
      .timestamp(timestamp)
      .serverReceivedTimestamp(serverReceivedTimestamp)
      .serverDeliveredTimestamp(serverDeliveredTimestamp)
      .urgent(isUrgent)
      .story(isStory)
    if (sourceServiceId.isPresent) {
      builder.sourceServiceId(sourceServiceId.get())
    }
    val contentBytes = content
    if (contentBytes != null) {
      builder.content(ByteString.of(*contentBytes))
    }
    if (hasServerGuid()) {
      builder.serverGuid(serverGuid)
    }
    if (destinationServiceId != null) {
      builder.destinationServiceId(destinationServiceId.toString())
    }
    val reportingTokenBytes = reportingToken
    if (reportingTokenBytes != null) {
      builder.reportingToken(ByteString.of(*reportingTokenBytes))
    }
    return builder
  }

  fun serialize(): ByteArray {
    return serializeToProto().build().encode()
  }

  companion object {
    private val TAG = SignalServiceEnvelope::class.java.simpleName
    fun deserialize(serialized: ByteArray): SignalServiceEnvelope {
      val proto: SignalServiceEnvelopeProto
      try {
        proto = SignalServiceEnvelopeProto.ADAPTER.decode(serialized)
      } catch (e: IOException) {
        e.printStackTrace()
        throw AssertionError(e)
      }
      val sourceServiceId = if (proto.sourceServiceId != null) {
        parseOrNull(
          proto.sourceServiceId
        )
      } else {
        null
      }
      return SignalServiceEnvelope(
        proto.type,
        if (sourceServiceId != null) Optional.of(SignalServiceAddress(sourceServiceId)) else Optional.empty(),
        proto.deviceId,
        proto.timestamp,
        if (proto.content != null) proto.content.toByteArray() else null,
        proto.serverReceivedTimestamp,
        proto.serverDeliveredTimestamp,
        proto.serverGuid,
        proto.destinationServiceId,
        proto.urgent != null && proto.urgent,
        proto.story != null && proto.story,
        if (proto.reportingToken != null) proto.reportingToken.toByteArray() else null,
        ""
      )
    }
  }
}
