/*
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package org.whispersystems.signalservice.api.messages

import okio.ByteString
import org.signal.libsignal.metadata.ProtocolInvalidKeyException
import org.signal.libsignal.metadata.ProtocolInvalidMessageException
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.InvalidMessageException
import org.signal.libsignal.protocol.InvalidVersionException
import org.signal.libsignal.protocol.LegacyMessageException
import org.signal.libsignal.protocol.logging.Log
import org.signal.libsignal.protocol.message.DecryptionErrorMessage
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import org.signal.libsignal.zkgroup.InvalidInputException
import org.signal.libsignal.zkgroup.groups.GroupMasterKey
import org.signal.libsignal.zkgroup.receipts.ReceiptCredentialPresentation
import org.whispersystems.signalservice.api.InvalidMessageStructureException
import org.whispersystems.signalservice.api.crypto.EnvelopeMetadata
import org.whispersystems.signalservice.api.kbs.MasterKey
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.Companion.newBuilder
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.Mention
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.PaymentActivation
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.PaymentNotification
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.Quote.Type.Companion.fromProto
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.RemoteDelete
import org.whispersystems.signalservice.api.messages.calls.AnswerMessage
import org.whispersystems.signalservice.api.messages.calls.BusyMessage
import org.whispersystems.signalservice.api.messages.calls.HangupMessage
import org.whispersystems.signalservice.api.messages.calls.IceUpdateMessage
import org.whispersystems.signalservice.api.messages.calls.OfferMessage
import org.whispersystems.signalservice.api.messages.calls.OpaqueMessage
import org.whispersystems.signalservice.api.messages.calls.SignalServiceCallMessage
import org.whispersystems.signalservice.api.messages.multidevice.BlockedListMessage
import org.whispersystems.signalservice.api.messages.multidevice.ConfigurationMessage
import org.whispersystems.signalservice.api.messages.multidevice.ContactsMessage
import org.whispersystems.signalservice.api.messages.multidevice.KeysMessage
import org.whispersystems.signalservice.api.messages.multidevice.MessageRequestResponseMessage
import org.whispersystems.signalservice.api.messages.multidevice.OutgoingPaymentMessage
import org.whispersystems.signalservice.api.messages.multidevice.ReadMessage
import org.whispersystems.signalservice.api.messages.multidevice.RequestMessage
import org.whispersystems.signalservice.api.messages.multidevice.SentTranscriptMessage
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage
import org.whispersystems.signalservice.api.messages.multidevice.StickerPackOperationMessage
import org.whispersystems.signalservice.api.messages.multidevice.VerifiedMessage
import org.whispersystems.signalservice.api.messages.multidevice.VerifiedMessage.VerifiedState
import org.whispersystems.signalservice.api.messages.multidevice.ViewOnceOpenMessage
import org.whispersystems.signalservice.api.messages.multidevice.ViewedMessage
import org.whispersystems.signalservice.api.messages.shared.SharedContact
import org.whispersystems.signalservice.api.payments.Money
import org.whispersystems.signalservice.api.push.ServiceId
import org.whispersystems.signalservice.api.push.ServiceId.ACI
import org.whispersystems.signalservice.api.push.ServiceId.Companion.parseOrNull
import org.whispersystems.signalservice.api.push.ServiceId.Companion.parseOrThrow
import org.whispersystems.signalservice.api.push.ServiceId.PNI
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.api.storage.StorageKey
import org.whispersystems.signalservice.api.util.AttachmentPointerUtil
import org.whispersystems.signalservice.internal.push.AttachmentPointer
import org.whispersystems.signalservice.internal.push.BodyRange
import org.whispersystems.signalservice.internal.push.CallMessage
import org.whispersystems.signalservice.internal.push.Content
import org.whispersystems.signalservice.internal.push.DataMessage
import org.whispersystems.signalservice.internal.push.EditMessage
import org.whispersystems.signalservice.internal.push.Envelope
import org.whispersystems.signalservice.internal.push.GroupContext
import org.whispersystems.signalservice.internal.push.GroupContextV2
import org.whispersystems.signalservice.internal.push.Preview
import org.whispersystems.signalservice.internal.push.ReceiptMessage
import org.whispersystems.signalservice.internal.push.StoryMessage
import org.whispersystems.signalservice.internal.push.SyncMessage
import org.whispersystems.signalservice.internal.push.SyncMessage.Sent.StoryMessageRecipient
import org.whispersystems.signalservice.internal.push.SyncMessage.StickerPackOperation
import org.whispersystems.signalservice.internal.push.TextAttachment
import org.whispersystems.signalservice.internal.push.TypingMessage
import org.whispersystems.signalservice.internal.push.UnsupportedDataMessageException
import org.whispersystems.signalservice.internal.push.UnsupportedDataMessageProtocolVersionException
import org.whispersystems.signalservice.internal.push.Verified
import org.whispersystems.signalservice.internal.serialize.SignalServiceAddressProtobufSerializer
import org.whispersystems.signalservice.internal.serialize.SignalServiceMetadataProtobufSerializer
import org.whispersystems.signalservice.internal.serialize.protos.SignalServiceContentProto
import java.io.IOException
import java.util.LinkedList
import java.util.Optional
import java.util.stream.Collectors

class SignalServiceContent {
  val sender: SignalServiceAddress
  val senderDevice: Int
  val timestamp: Long
  val serverReceivedTimestamp: Long
  val serverDeliveredTimestamp: Long
  val isNeedsReceipt: Boolean
  private val serializedState: SignalServiceContentProto
  val serverUuid: String
  val groupId: Optional<ByteArray>
  val destinationServiceId: String
  val dataMessage: Optional<SignalServiceDataMessage>
  val syncMessage: Optional<SignalServiceSyncMessage>
  val callMessage: Optional<SignalServiceCallMessage>
  val receiptMessage: Optional<SignalServiceReceiptMessage>
  val typingMessage: Optional<SignalServiceTypingMessage>
  val senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>
  val decryptionErrorMessage: Optional<DecryptionErrorMessage>
  val storyMessage: Optional<SignalServiceStoryMessage>
  val pniSignatureMessage: Optional<SignalServicePniSignatureMessage>
  val editMessage: Optional<SignalServiceEditMessage>

  private constructor(
    message: SignalServiceDataMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.ofNullable(message)
    syncMessage = Optional.empty()
    callMessage = Optional.empty()
    receiptMessage = Optional.empty()
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.empty()
    storyMessage = Optional.empty()
    this.pniSignatureMessage = pniSignatureMessage
    editMessage = Optional.empty()
  }

  private constructor(
    synchronizeMessage: SignalServiceSyncMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.ofNullable(synchronizeMessage)
    callMessage = Optional.empty()
    receiptMessage = Optional.empty()
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.empty()
    storyMessage = Optional.empty()
    this.pniSignatureMessage = pniSignatureMessage
    editMessage = Optional.empty()
  }

  private constructor(
    callMessage: SignalServiceCallMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.empty()
    this.callMessage = Optional.of(callMessage)
    receiptMessage = Optional.empty()
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.empty()
    storyMessage = Optional.empty()
    this.pniSignatureMessage = pniSignatureMessage
    editMessage = Optional.empty()
  }

  private constructor(
    receiptMessage: SignalServiceReceiptMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.empty()
    callMessage = Optional.empty()
    this.receiptMessage = Optional.of(receiptMessage)
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.empty()
    storyMessage = Optional.empty()
    this.pniSignatureMessage = pniSignatureMessage
    editMessage = Optional.empty()
  }

  private constructor(
    errorMessage: DecryptionErrorMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.empty()
    callMessage = Optional.empty()
    receiptMessage = Optional.empty()
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.of(errorMessage)
    storyMessage = Optional.empty()
    this.pniSignatureMessage = pniSignatureMessage
    editMessage = Optional.empty()
  }

  private constructor(
    typingMessage: SignalServiceTypingMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.empty()
    callMessage = Optional.empty()
    receiptMessage = Optional.empty()
    this.typingMessage = Optional.of(typingMessage)
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.empty()
    storyMessage = Optional.empty()
    this.pniSignatureMessage = pniSignatureMessage
    editMessage = Optional.empty()
  }

  private constructor(
    senderKeyDistributionMessage: SenderKeyDistributionMessage,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.empty()
    callMessage = Optional.empty()
    receiptMessage = Optional.empty()
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = Optional.of(senderKeyDistributionMessage)
    decryptionErrorMessage = Optional.empty()
    storyMessage = Optional.empty()
    this.pniSignatureMessage = pniSignatureMessage
    editMessage = Optional.empty()
  }

  private constructor(
    pniSignatureMessage: SignalServicePniSignatureMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.empty()
    callMessage = Optional.empty()
    receiptMessage = Optional.empty()
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.empty()
    storyMessage = Optional.empty()
    this.pniSignatureMessage = Optional.of(pniSignatureMessage)
    editMessage = Optional.empty()
  }

  private constructor(
    storyMessage: SignalServiceStoryMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.empty()
    callMessage = Optional.empty()
    receiptMessage = Optional.empty()
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.empty()
    this.storyMessage = Optional.of(storyMessage)
    this.pniSignatureMessage = pniSignatureMessage
    editMessage = Optional.empty()
  }

  private constructor(
    editMessage: SignalServiceEditMessage,
    senderKeyDistributionMessage: Optional<SenderKeyDistributionMessage>,
    pniSignatureMessage: Optional<SignalServicePniSignatureMessage>,
    sender: SignalServiceAddress,
    senderDevice: Int,
    timestamp: Long,
    serverReceivedTimestamp: Long,
    serverDeliveredTimestamp: Long,
    needsReceipt: Boolean,
    serverUuid: String,
    groupId: Optional<ByteArray>,
    destinationUuid: String,
    serializedState: SignalServiceContentProto
  ) {
    this.sender = sender
    this.senderDevice = senderDevice
    this.timestamp = timestamp
    this.serverReceivedTimestamp = serverReceivedTimestamp
    this.serverDeliveredTimestamp = serverDeliveredTimestamp
    isNeedsReceipt = needsReceipt
    this.serverUuid = serverUuid
    this.groupId = groupId
    destinationServiceId = destinationUuid
    this.serializedState = serializedState
    dataMessage = Optional.empty()
    syncMessage = Optional.empty()
    callMessage = Optional.empty()
    receiptMessage = Optional.empty()
    typingMessage = Optional.empty()
    this.senderKeyDistributionMessage = senderKeyDistributionMessage
    decryptionErrorMessage = Optional.empty()
    storyMessage = Optional.empty()
    this.pniSignatureMessage = pniSignatureMessage
    this.editMessage = Optional.of(editMessage)
  }

  fun serialize(): ByteArray {
    return serializedState.encode()
  }

  companion object {
    private val TAG = SignalServiceContent::class.java.simpleName
    fun deserialize(data: ByteArray?): SignalServiceContent? {
      return try {
        if (data == null) return null
        val signalServiceContentProto = SignalServiceContentProto.ADAPTER.decode(data)
        createFromProto(signalServiceContentProto)
      } catch (e: IOException) {
        // We do not expect any of these exceptions if this byte[] has come from serialize.
        throw AssertionError(e)
      } catch (e: ProtocolInvalidMessageException) {
        throw AssertionError(e)
      } catch (e: ProtocolInvalidKeyException) {
        throw AssertionError(e)
      } catch (e: UnsupportedDataMessageException) {
        throw AssertionError(e)
      } catch (e: InvalidMessageStructureException) {
        throw AssertionError(e)
      }
    }

    @Throws(
      ProtocolInvalidKeyException::class,
      ProtocolInvalidMessageException::class,
      UnsupportedDataMessageException::class,
      InvalidMessageStructureException::class
    )
    fun createFrom(
      localNumber: String?,
      envelope: Envelope,
      envelopeMetadata: EnvelopeMetadata,
      content: Content?,
      serverDeliveredTimestamp: Long
    ): SignalServiceContent? {
      val localAddress = SignalServiceAddress(
        envelopeMetadata.destinationServiceId,
        Optional.ofNullable(localNumber)
      )
      val metadata = SignalServiceMetadata(
        SignalServiceAddress(
          envelopeMetadata.sourceServiceId,
          Optional.ofNullable(envelopeMetadata.sourceE164)
        ),
        envelopeMetadata.sourceDeviceId,
        envelope.timestamp ?: 0,
        envelope.serverTimestamp ?: 0,
        serverDeliveredTimestamp,
        envelopeMetadata.sealedSender,
        envelope.serverGuid,
        Optional.ofNullable(envelopeMetadata.groupId),
        envelopeMetadata.destinationServiceId.toString()
      )

      val contentProto = SignalServiceContentProto.Builder()
        .localAddress(SignalServiceAddressProtobufSerializer.toProtobuf(localAddress))
        .metadata(SignalServiceMetadataProtobufSerializer.toProtobuf(metadata))
        .content(content)
        .build()
      return createFromProto(contentProto)
    }

    /**
     * Takes internal protobuf serialization format and processes it into a [SignalServiceContent].
     */
    @Throws(
      ProtocolInvalidMessageException::class,
      ProtocolInvalidKeyException::class,
      UnsupportedDataMessageException::class,
      InvalidMessageStructureException::class
    )
    fun createFromProto(serviceContentProto: SignalServiceContentProto): SignalServiceContent? {
      if (serviceContentProto.metadata == null) {
        throw InvalidMessageStructureException("Missing metadata")
      }
      if (serviceContentProto.localAddress == null) {
        throw InvalidMessageStructureException("Missing local address")
      }
      val metadata =
        SignalServiceMetadataProtobufSerializer.fromProtobuf(serviceContentProto.metadata)
      val localAddress =
        SignalServiceAddressProtobufSerializer.fromProtobuf(serviceContentProto.localAddress)
      if (serviceContentProto.legacyDataMessage != null) {
        throw InvalidMessageStructureException("Legacy message!")
      } else if (serviceContentProto.content != null) {
        val message = serviceContentProto.content
        var senderKeyDistributionMessage = Optional.empty<SenderKeyDistributionMessage>()
        if (message.senderKeyDistributionMessage != null) {
          try {
            senderKeyDistributionMessage = Optional.of(
              SenderKeyDistributionMessage(
                message.senderKeyDistributionMessage.toByteArray()
              )
            )
          } catch (e: LegacyMessageException) {
            Log.w(TAG, "Failed to parse SenderKeyDistributionMessage!", e)
          } catch (e: InvalidMessageException) {
            Log.w(TAG, "Failed to parse SenderKeyDistributionMessage!", e)
          } catch (e: InvalidVersionException) {
            Log.w(TAG, "Failed to parse SenderKeyDistributionMessage!", e)
          } catch (e: InvalidKeyException) {
            Log.w(TAG, "Failed to parse SenderKeyDistributionMessage!", e)
          }
        }
        var pniSignatureMessage = Optional.empty<SignalServicePniSignatureMessage>()
        if (message.pniSignatureMessage?.pni != null && message.pniSignatureMessage.signature != null) {
          val pni = PNI.parseOrNull(message.pniSignatureMessage.pni.toByteArray())
          if (pni != null) {
            pniSignatureMessage = Optional.of(
              SignalServicePniSignatureMessage(
                pni,
                message.pniSignatureMessage.signature.toByteArray()
              )
            )
          } else {
            Log.w(TAG, "Invalid PNI on PNI signature message! Ignoring.")
          }
        }
        if (message.dataMessage != null) {
          return SignalServiceContent(
            createSignalServiceDataMessage(metadata, message.dataMessage),
            senderKeyDistributionMessage,
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            metadata.isNeedsReceipt,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (message.syncMessage != null && localAddress.matches(metadata.sender)) {
          return SignalServiceContent(
            createSynchronizeMessage(metadata, message.syncMessage),
            senderKeyDistributionMessage,
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            metadata.isNeedsReceipt,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (message.callMessage != null) {
          return SignalServiceContent(
            createCallMessage(
              message.callMessage
            ),
            senderKeyDistributionMessage,
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            metadata.isNeedsReceipt,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (message.receiptMessage != null) {
          return SignalServiceContent(
            createReceiptMessage(metadata, message.receiptMessage),
            senderKeyDistributionMessage,
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            metadata.isNeedsReceipt,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (message.typingMessage != null) {
          return SignalServiceContent(
            createTypingMessage(metadata, message.typingMessage),
            senderKeyDistributionMessage,
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            false,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (message.decryptionErrorMessage != null) {
          return SignalServiceContent(
            createDecryptionErrorMessage(metadata, message.decryptionErrorMessage),
            senderKeyDistributionMessage,
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            metadata.isNeedsReceipt,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (message.storyMessage != null) {
          return SignalServiceContent(
            createStoryMessage(
              message.storyMessage
            ),
            senderKeyDistributionMessage,
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            false,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (pniSignatureMessage.isPresent) {
          return SignalServiceContent(
            pniSignatureMessage.get(),
            senderKeyDistributionMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            false,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (message.editMessage != null) {
          return SignalServiceContent(
            createEditMessage(metadata, message.editMessage),
            senderKeyDistributionMessage,
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            metadata.isNeedsReceipt,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        } else if (senderKeyDistributionMessage.isPresent) {
          // IMPORTANT: This block should always be last, since you can pair SKDM's with other content
          return SignalServiceContent(
            senderKeyDistributionMessage.get(),
            pniSignatureMessage,
            metadata.sender,
            metadata.senderDevice,
            metadata.timestamp,
            metadata.serverReceivedTimestamp,
            metadata.serverDeliveredTimestamp,
            false,
            metadata.serverGuid,
            metadata.groupId,
            metadata.destinationUuid,
            serviceContentProto
          )
        }
      }
      return null
    }

    @Throws(UnsupportedDataMessageException::class, InvalidMessageStructureException::class)
    private fun createSignalServiceDataMessage(
      metadata: SignalServiceMetadata,
      content: DataMessage
    ): SignalServiceDataMessage {
      val groupInfoV1 = createGroupV1Info(content)
      val groupInfoV2 = createGroupV2Info(content)
      val groupContext = try {
        SignalServiceGroupContext.createOptional(groupInfoV1, groupInfoV2)
      } catch (e: InvalidMessageException) {
        throw InvalidMessageStructureException(e)
      }
      val attachments: MutableList<SignalServiceAttachment> = LinkedList()
      val endSession =
        content.flags != null && content.flags and DataMessage.Flags.END_SESSION.value != 0
      val expirationUpdate =
        content.flags != null && content.flags and DataMessage.Flags.EXPIRATION_TIMER_UPDATE.value != 0
      val profileKeyUpdate =
        content.flags != null && content.flags and DataMessage.Flags.PROFILE_KEY_UPDATE.value != 0
      val isGroupV2 = groupInfoV2 != null
      val quote = createQuote(content, isGroupV2)
      val sharedContacts = createSharedContacts(content)
      val previews = createPreviews(content)
      val mentions = createMentions(
        content.bodyRanges,
        content.body,
        isGroupV2
      )
      val sticker = createSticker(content)
      val reaction = createReaction(content)
      val remoteDelete = createRemoteDelete(content)
      val groupCallUpdate = createGroupCallUpdate(content)
      val storyContext = createStoryContext(content)
      val giftBadge = createGiftBadge(content)
      val bodyRanges = createBodyRanges(
        content.bodyRanges,
        content.body
      )
      val requiredProtocolVersion = content.requiredProtocolVersion ?: 0
      if (requiredProtocolVersion > DataMessage.ProtocolVersion.CURRENT.value) {
        throw UnsupportedDataMessageProtocolVersionException(
          DataMessage.ProtocolVersion.CURRENT.value,
          requiredProtocolVersion,
          metadata.sender.identifier,
          metadata.senderDevice,
          groupContext
        )
      }
      val payment = createPayment(content)
      if (requiredProtocolVersion > DataMessage.ProtocolVersion.CURRENT.value) {
        throw UnsupportedDataMessageProtocolVersionException(
          DataMessage.ProtocolVersion.CURRENT.value,
          requiredProtocolVersion,
          metadata.sender.identifier,
          metadata.senderDevice,
          groupContext
        )
      }
      for (pointer in content.attachments) {
        attachments.add(createAttachmentPointer(pointer))
      }
      if (content.timestamp != null && content.timestamp != metadata.timestamp) {
        throw InvalidMessageStructureException(
          "Timestamps don't match: " + content.timestamp + " vs " + metadata.timestamp,
          metadata.sender.identifier,
          metadata.senderDevice
        )
      }
      return newBuilder()
        .withTimestamp(metadata.timestamp)
        .asGroupMessage(groupInfoV1)
        .asGroupMessage(groupInfoV2)
        .withAttachments(attachments)
        .withBody(content.body)
        .asEndSessionMessage(endSession)
        .withExpiration(content.expireTimer ?: 0)
        .asExpirationUpdate(expirationUpdate)
        .withProfileKey(content.profileKey?.toByteArray())
        .asProfileKeyUpdate(profileKeyUpdate)
        .withQuote(quote)
        .withSharedContacts(sharedContacts)
        .withPreviews(previews)
        .withMentions(mentions)
        .withSticker(sticker)
        .withViewOnce(java.lang.Boolean.TRUE == content.isViewOnce)
        .withReaction(reaction)
        .withRemoteDelete(remoteDelete)
        .withGroupCallUpdate(groupCallUpdate)
        .withPayment(payment)
        .withStoryContext(storyContext)
        .withGiftBadge(giftBadge)
        .withBodyRanges(bodyRanges)
        .build()
    }

    @Throws(
      ProtocolInvalidKeyException::class,
      UnsupportedDataMessageException::class,
      InvalidMessageStructureException::class
    )
    private fun createSynchronizeMessage(
      metadata: SignalServiceMetadata,
      content: SyncMessage
    ): SignalServiceSyncMessage {
      if (content.sent != null) {
        val unidentifiedStatuses: MutableMap<ServiceId?, Boolean?> = HashMap()
        val sentContent = content.sent
        val message = sentContent.message
        val dataMessage = if (message != null) {
          Optional.of(
            createSignalServiceDataMessage(metadata, sentContent.message)
          )
        } else {
          Optional.empty()
        }
        val storyMessage = if (sentContent.storyMessage != null) {
          Optional.of(
            createStoryMessage(
              sentContent.storyMessage
            )
          )
        } else {
          Optional.empty()
        }
        val editMessage = if (sentContent.editMessage != null) {
          Optional.of(
            createEditMessage(metadata, sentContent.editMessage)
          )
        } else {
          Optional.empty()
        }
        val address =
          if (sentContent.destinationServiceId != null && parseOrNull(
              sentContent.destinationServiceId
            ) != null
          ) {
            Optional.of(
              SignalServiceAddress(
                parseOrThrow(
                  sentContent.destinationServiceId
                ),
                sentContent.destinationE164
              )
            )
          } else {
            Optional.empty()
          }
        val recipientManifest = sentContent.storyMessageRecipients
          .stream()
          .filter { it.destinationServiceId != null }
          .map { storyMessageRecipient: StoryMessageRecipient ->
            createSignalServiceStoryMessageRecipient(
              storyMessageRecipient
            )
          }
          .collect(Collectors.toSet())
        if (address.isEmpty &&
          dataMessage.flatMap(SignalServiceDataMessage::groupContext).isEmpty &&
          storyMessage.flatMap { obj: SignalServiceStoryMessage -> obj.groupContext }.isEmpty &&
          recipientManifest.isEmpty() &&
          dataMessage.flatMap(SignalServiceDataMessage::remoteDelete).isEmpty &&
          editMessage.isEmpty()
        ) {
          throw InvalidMessageStructureException("SyncMessage missing destination, group ID, and recipient manifest!")
        }
        for (status in sentContent.unidentifiedStatus) {
          if (SignalServiceAddress.isValidAddress(status.destinationServiceId, null)) {
            unidentifiedStatuses[ServiceId.parseOrNull(status.destinationServiceId)] =
              status.unidentified
          } else {
            Log.w(
              TAG,
              "Encountered an invalid UnidentifiedDeliveryStatus in a SentTranscript! Ignoring."
            )
          }
        }
        return SignalServiceSyncMessage.forSentTranscript(
          SentTranscriptMessage(
            address,
            sentContent.timestamp ?: 0,
            dataMessage,
            sentContent.expirationStartTimestamp ?: 0,
            unidentifiedStatuses,
            java.lang.Boolean.TRUE == sentContent.isRecipientUpdate,
            storyMessage,
            recipientManifest,
            editMessage
          )
        )
      }
      if (content.request != null) {
        return SignalServiceSyncMessage.forRequest(RequestMessage(content.request))
      }
      if (content.read.isNotEmpty()) {
        val readMessages: MutableList<ReadMessage> = LinkedList()
        for (read in content.read) {
          val aci = ACI.parseOrNull(read.senderAci)
          if (aci != null && read.timestamp != null) {
            readMessages.add(ReadMessage(aci, read.timestamp))
          } else {
            Log.w(TAG, "Encountered an invalid ReadMessage! Ignoring.")
          }
        }
        return SignalServiceSyncMessage.forRead(readMessages)
      }
      if (content.viewed.isNotEmpty()) {
        val viewedMessages: MutableList<ViewedMessage> = LinkedList()
        for (viewed in content.viewed) {
          val aci = ACI.parseOrNull(viewed.senderAci)
          if (aci != null && viewed.timestamp != null) {
            viewedMessages.add(ViewedMessage(aci, viewed.timestamp))
          } else {
            Log.w(TAG, "Encountered an invalid ReadMessage! Ignoring.")
          }
        }
        return SignalServiceSyncMessage.forViewed(viewedMessages)
      }
      if (content.viewOnceOpen != null) {
        val aci = ACI.parseOrNull(content.viewOnceOpen.senderAci)
        return if (aci != null) {
          val timerRead = ViewOnceOpenMessage(aci, content.viewOnceOpen.timestamp ?: 0)
          SignalServiceSyncMessage.forViewOnceOpen(timerRead)
        } else {
          throw InvalidMessageStructureException("ViewOnceOpen message has no sender!")
        }
      }
      if (content.verified != null) {
        val verified = content.verified
        if (verified.destinationAci == null || !SignalServiceAddress.isValidAddress(verified.destinationAci)) {
          throw InvalidMessageStructureException("Verified message has no sender!")
        }
        try {
          val destination = SignalServiceAddress(
            parseOrThrow(
              verified.destinationAci
            )
          )
          if (verified.identityKey == null) {
            throw InvalidMessageStructureException("Verified message has no identity key!")
          }
          val identityKey = IdentityKey(verified.identityKey.toByteArray(), 0)
          val verifiedState = when (verified.state) {
            Verified.State.DEFAULT -> VerifiedState.DEFAULT
            Verified.State.VERIFIED -> VerifiedState.VERIFIED
            Verified.State.UNVERIFIED -> VerifiedState.UNVERIFIED
            else -> throw InvalidMessageStructureException(
              "Unknown state: " + verified.state?.value,
              metadata.sender.identifier,
              metadata.senderDevice
            )
          }
          return SignalServiceSyncMessage.forVerified(
            VerifiedMessage(
              destination,
              identityKey,
              verifiedState,
              System.currentTimeMillis()
            )
          )
        } catch (e: InvalidKeyException) {
          throw ProtocolInvalidKeyException(
            e,
            metadata.sender.identifier,
            metadata.senderDevice
          )
        }
      }

      if (content.stickerPackOperation.isNotEmpty()) {
        val operations: MutableList<StickerPackOperationMessage> = LinkedList()
        for (operation in content.stickerPackOperation) {
          val packId =
            if (operation.packId != null) operation.packId.toByteArray() else null
          val packKey =
            if (operation.packKey != null) operation.packKey.toByteArray() else null
          var type: StickerPackOperationMessage.Type? = null
          if (operation.type != null) {
            type = when (operation.type) {
              StickerPackOperation.Type.INSTALL -> StickerPackOperationMessage.Type.INSTALL
              StickerPackOperation.Type.REMOVE -> StickerPackOperationMessage.Type.REMOVE
            }
          }
          operations.add(StickerPackOperationMessage(packId, packKey, type))
        }
        return SignalServiceSyncMessage.forStickerPackOperations(operations)
      }
      if (content.blocked != null) {
        val numbers = content.blocked.numbers
        val uuids = content.blocked.acis
        val addresses: MutableList<SignalServiceAddress> =
          ArrayList(numbers.size + uuids.size)
        val groupIds: MutableList<ByteArray> = ArrayList(
          content.blocked.groupIds.size
        )
        for (uuid in uuids) {
          val address = SignalServiceAddress.fromRaw(uuid, null)
          if (address.isPresent) {
            addresses.add(address.get())
          }
        }
        for (groupId in content.blocked.groupIds) {
          groupIds.add(groupId.toByteArray())
        }
        return SignalServiceSyncMessage.forBlocked(BlockedListMessage(addresses, groupIds))
      }
      if (content.configuration != null) {
        val readReceipts = content.configuration.readReceipts
        val unidentifiedDeliveryIndicators =
          content.configuration.unidentifiedDeliveryIndicators
        val typingIndicators = content.configuration.typingIndicators
        val linkPreviews = content.configuration.linkPreviews
        return SignalServiceSyncMessage.forConfiguration(
          ConfigurationMessage(
            Optional.ofNullable(readReceipts),
            Optional.ofNullable(unidentifiedDeliveryIndicators),
            Optional.ofNullable(typingIndicators),
            Optional.ofNullable(linkPreviews)
          )
        )
      }
      if (content.fetchLatest?.type != null) {
        return when (content.fetchLatest.type) {
          SyncMessage.FetchLatest.Type.LOCAL_PROFILE -> SignalServiceSyncMessage.forFetchLatest(
            SignalServiceSyncMessage.FetchType.LOCAL_PROFILE
          )

          SyncMessage.FetchLatest.Type.STORAGE_MANIFEST -> SignalServiceSyncMessage.forFetchLatest(
            SignalServiceSyncMessage.FetchType.STORAGE_MANIFEST
          )

          SyncMessage.FetchLatest.Type.SUBSCRIPTION_STATUS -> SignalServiceSyncMessage.forFetchLatest(
            SignalServiceSyncMessage.FetchType.SUBSCRIPTION_STATUS
          )

          SyncMessage.FetchLatest.Type.UNKNOWN -> throw InvalidMessageStructureException("Message request response has an invalid thread identifier!")
        }
      }
      if (content.messageRequestResponse?.type != null) {
        val type = when (content.messageRequestResponse.type) {
          SyncMessage.MessageRequestResponse.Type.ACCEPT -> MessageRequestResponseMessage.Type.ACCEPT
          SyncMessage.MessageRequestResponse.Type.DELETE -> MessageRequestResponseMessage.Type.DELETE
          SyncMessage.MessageRequestResponse.Type.BLOCK -> MessageRequestResponseMessage.Type.BLOCK
          SyncMessage.MessageRequestResponse.Type.BLOCK_AND_DELETE -> MessageRequestResponseMessage.Type.BLOCK_AND_DELETE
          else -> MessageRequestResponseMessage.Type.UNKNOWN
        }
        val responseMessage = if (content.messageRequestResponse.groupId != null) {
          MessageRequestResponseMessage.forGroup(
            content.messageRequestResponse.groupId.toByteArray(),
            type
          )
        } else {
          val aci = ACI.parseOrNull(content.messageRequestResponse.threadAci)
          if (aci != null) {
            MessageRequestResponseMessage.forIndividual(aci, type)
          } else {
            throw InvalidMessageStructureException("Message request response has an invalid thread identifier!")
          }
        }
        return SignalServiceSyncMessage.forMessageRequestResponse(responseMessage)
      }
      if (content.groups != null) {
        return SignalServiceSyncMessage.forGroups(
          createAttachmentPointer(
            content.groups.blob
          )
        )
      }
      if (content.outgoingPayment != null) {
        val outgoingPayment = content.outgoingPayment
        return if (outgoingPayment.mobileCoin != null) {
          val mobileCoin = outgoingPayment.mobileCoin
          val amount = Money.picoMobileCoin(mobileCoin.amountPicoMob ?: 0)
          val fee = Money.picoMobileCoin(mobileCoin.feePicoMob ?: 0)
          val address = mobileCoin.recipientAddress
          val recipient = Optional.ofNullable(
            ServiceId.parseOrNull(
              outgoingPayment.recipientServiceId
            )
          )
          val addressBytes = address?.toByteArray()
          SignalServiceSyncMessage.forOutgoingPayment(
            OutgoingPaymentMessage(
              recipient,
              amount,
              fee,
              mobileCoin.receipt,
              mobileCoin.ledgerBlockIndex ?: 0,
              mobileCoin.ledgerBlockTimestamp ?: 0,
              if (addressBytes == null || addressBytes.isEmpty()) {
                Optional.empty()
              } else {
                Optional.of(
                  addressBytes
                )
              },
              Optional.ofNullable(outgoingPayment.note),
              mobileCoin.outputPublicKeys,
              mobileCoin.spentKeyImages
            )
          )
        } else {
          SignalServiceSyncMessage.empty()
        }
      }
      if (content.keys?.storageService != null) {
        val storageKey = content.keys.storageService.toByteArray()
        val masterKey = content.keys.master?.toByteArray()
        return SignalServiceSyncMessage.forKeys(
          KeysMessage(
            Optional.of(
              StorageKey(
                storageKey
              )
            ),
            Optional.ofNullable(
              masterKey?.let {
                MasterKey(
                  it
                )
              }
            )
          )
        )
      }
      if (content.contacts != null) {
        return SignalServiceSyncMessage.forContacts(
          ContactsMessage(
            createAttachmentPointer(
              content.contacts.blob
            ),
            java.lang.Boolean.TRUE == content.contacts.complete
          )
        )
      }
      if (content.pniChangeNumber != null) {
        return SignalServiceSyncMessage.forPniChangeNumber(content.pniChangeNumber)
      }
      return if (content.callEvent != null) {
        SignalServiceSyncMessage.forCallEvent(content.callEvent)
      } else {
        SignalServiceSyncMessage.empty()
      }
    }

    private fun createSignalServiceStoryMessageRecipient(storyMessageRecipient: StoryMessageRecipient): SignalServiceStoryMessageRecipient {
      return SignalServiceStoryMessageRecipient(
        SignalServiceAddress(
          parseOrThrow(
            storyMessageRecipient.destinationServiceId ?: ""
          )
        ),
        storyMessageRecipient.distributionListIds,
        java.lang.Boolean.TRUE == storyMessageRecipient.isAllowedToReply
      )
    }

    private fun createCallMessage(content: CallMessage): SignalServiceCallMessage {
      val isMultiRing = false
      val destinationDeviceId = content.destinationDeviceId
      if (content.offer != null) {
        val offerContent = content.offer
        return SignalServiceCallMessage.forOffer(
          OfferMessage(
            offerContent.id ?: 0,
            OfferMessage.Type.fromProto(
              offerContent.type
            ),
            if (offerContent.opaque != null) offerContent.opaque.toByteArray() else null
          ),
          destinationDeviceId
        )
      } else if (content.answer != null) {
        val answerContent = content.answer
        return SignalServiceCallMessage.forAnswer(
          AnswerMessage(
            answerContent.id ?: 0,
            if (answerContent.opaque != null) answerContent.opaque.toByteArray() else null
          ),
          destinationDeviceId
        )
      } else if (content.iceUpdate.isNotEmpty()) {
        val iceUpdates: MutableList<IceUpdateMessage> = LinkedList()
        for (iceUpdate in content.iceUpdate) {
          iceUpdates.add(
            IceUpdateMessage(
              iceUpdate.id ?: 0,
              if (iceUpdate.opaque != null) iceUpdate.opaque.toByteArray() else null
            )
          )
        }
        return SignalServiceCallMessage.forIceUpdates(
          iceUpdates,
          destinationDeviceId
        )
      } else if (content.hangup != null) {
        val hangup = content.hangup
        return SignalServiceCallMessage.forHangup(
          HangupMessage(
            hangup.id ?: 0,
            HangupMessage.Type.fromProto(
              hangup.type
            ),
            hangup.deviceId ?: 0
          ),
          destinationDeviceId
        )
      } else if (content.busy != null) {
        val busy = content.busy
        return SignalServiceCallMessage.forBusy(
          BusyMessage(busy.id ?: 0),
          destinationDeviceId
        )
      } else if (content.opaque != null) {
        val opaque = content.opaque
        return SignalServiceCallMessage.forOpaque(
          OpaqueMessage(
            opaque.data_?.toByteArray() ?: ByteArray(0),
            null
          ),
          destinationDeviceId
        )
      }
      return SignalServiceCallMessage.empty()
    }

    private fun createReceiptMessage(
      metadata: SignalServiceMetadata,
      content: ReceiptMessage
    ): SignalServiceReceiptMessage {
      val type = when (content.type) {
        ReceiptMessage.Type.DELIVERY -> SignalServiceReceiptMessage.Type.DELIVERY
        ReceiptMessage.Type.READ -> SignalServiceReceiptMessage.Type.READ
        ReceiptMessage.Type.VIEWED -> SignalServiceReceiptMessage.Type.VIEWED
        else -> SignalServiceReceiptMessage.Type.UNKNOWN
      }
      return SignalServiceReceiptMessage(type, content.timestamp, metadata.timestamp)
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createDecryptionErrorMessage(
      metadata: SignalServiceMetadata,
      content: ByteString
    ): DecryptionErrorMessage {
      return try {
        DecryptionErrorMessage(content.toByteArray())
      } catch (e: InvalidMessageException) {
        throw InvalidMessageStructureException(
          e,
          metadata.sender.identifier,
          metadata.senderDevice
        )
      }
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createTypingMessage(
      metadata: SignalServiceMetadata,
      content: TypingMessage
    ): SignalServiceTypingMessage {
      val action =
        when (content.action) {
          TypingMessage.Action.STARTED -> SignalServiceTypingMessage.Action.STARTED
          TypingMessage.Action.STOPPED -> SignalServiceTypingMessage.Action.STOPPED
          else -> SignalServiceTypingMessage.Action.UNKNOWN
        }
      if (content.timestamp != null && content.timestamp != metadata.timestamp) {
        throw InvalidMessageStructureException(
          "Timestamps don't match: " + content.timestamp + " vs " + metadata.timestamp,
          metadata.sender.identifier,
          metadata.senderDevice
        )
      }
      return SignalServiceTypingMessage(
        action,
        content.timestamp ?: 0,
        if (content.groupId != null) Optional.of(content.groupId.toByteArray()) else Optional.empty()
      )
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createStoryMessage(content: StoryMessage): SignalServiceStoryMessage {
      val profileKey = content.profileKey?.toByteArray()
      return if (content.fileAttachment != null) {
        SignalServiceStoryMessage.forFileAttachment(
          profileKey,
          createGroupV2Info(content),
          createAttachmentPointer(content.fileAttachment),
          java.lang.Boolean.TRUE == content.allowsReplies,
          content.bodyRanges
        )
      } else {
        SignalServiceStoryMessage.forTextAttachment(
          profileKey,
          createGroupV2Info(content),
          createTextAttachment(content.textAttachment),
          java.lang.Boolean.TRUE == content.allowsReplies,
          content.bodyRanges
        )
      }
    }

    @Throws(InvalidMessageStructureException::class, UnsupportedDataMessageException::class)
    private fun createEditMessage(
      metadata: SignalServiceMetadata,
      content: EditMessage
    ): SignalServiceEditMessage {
      return if (content.dataMessage != null && content.targetSentTimestamp != null) {
        SignalServiceEditMessage(
          content.targetSentTimestamp,
          createSignalServiceDataMessage(
            metadata,
            content.dataMessage
          )
        )
      } else {
        throw InvalidMessageStructureException("Missing data message or timestamp from edit message.")
      }
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createQuote(
      content: DataMessage,
      isGroupV2: Boolean
    ): SignalServiceDataMessage.Quote? {
      val quote = content.quote ?: return null
      val attachments: MutableList<SignalServiceDataMessage.Quote.QuotedAttachment> =
        LinkedList()
      for (attachment in quote.attachments) {
        attachments.add(
          SignalServiceDataMessage.Quote.QuotedAttachment(
            attachment.contentType ?: "",
            attachment.fileName,
            if (attachment.thumbnail != null) createAttachmentPointer(attachment.thumbnail) else null
          )
        )
      }
      val author = ACI.parseOrNull(quote.authorAci)
      return if (author != null) {
        SignalServiceDataMessage.Quote(
          quote.id ?: 0,
          author,
          quote.text ?: "",
          attachments,
          createMentions(quote.bodyRanges, quote.text, isGroupV2),
          quote.type?.let { fromProto(it) } ?: SignalServiceDataMessage.Quote.Type.NORMAL,
          createBodyRanges(quote.bodyRanges, quote.text)
        )
      } else {
        Log.w(TAG, "Quote was missing an author! Returning null.")
        null
      }
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createPreviews(content: DataMessage): List<SignalServicePreview>? {
      if (content.preview.isEmpty()) return null
      val results: MutableList<SignalServicePreview> = LinkedList()
      for (preview in content.preview) {
        results.add(createPreview(preview))
      }
      return results
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createPreview(preview: Preview): SignalServicePreview {
      var attachment: SignalServiceAttachment? = null
      if (preview.image != null) {
        attachment = createAttachmentPointer(preview.image)
      }
      return SignalServicePreview(
        preview.url,
        preview.title,
        preview.description,
        preview.date ?: 0,
        Optional.ofNullable(attachment)
      )
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createMentions(
      bodyRanges: List<BodyRange>?,
      body: String?,
      isGroupV2: Boolean
    ): List<Mention>? {
      if (bodyRanges.isNullOrEmpty() || body == null) {
        return null
      }
      val mentions: MutableList<Mention> = LinkedList()
      for (bodyRange in bodyRanges) {
        if (bodyRange.mentionAci != null) {
          try {
            mentions.add(
              Mention(
                parseOrThrow(bodyRange.mentionAci),
                bodyRange.start ?: 0,
                bodyRange.length ?: 0
              )
            )
          } catch (e: IllegalArgumentException) {
            throw InvalidMessageStructureException("Invalid body range!")
          }
        }
      }
      if (mentions.size > 0 && !isGroupV2) {
        Log.w(TAG, "Mentions received in non-GV2 message")
      }
      return mentions
    }

    private fun createBodyRanges(
      bodyRanges: List<BodyRange>?,
      body: String?
    ): List<BodyRange>? {
      if (bodyRanges.isNullOrEmpty() || body == null) {
        return null
      }
      val ranges: MutableList<BodyRange> = LinkedList()
      for (bodyRange in bodyRanges) {
        if (bodyRange.style != null) {
          ranges.add(bodyRange)
        }
      }
      return ranges
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createSticker(content: DataMessage): SignalServiceDataMessage.Sticker? {
      val sticker = content.sticker
      if (sticker?.packId == null || sticker.packKey == null || sticker.stickerId == null || sticker.data_ == null) {
        return null
      }
      return SignalServiceDataMessage.Sticker(
        sticker.packId.toByteArray(),
        sticker.packKey.toByteArray(),
        sticker.stickerId,
        sticker.emoji,
        createAttachmentPointer(sticker.data_)
      )
    }

    private fun createReaction(content: DataMessage): SignalServiceDataMessage.Reaction? {
      val reaction = content.reaction
      if (reaction?.emoji == null || reaction.targetAuthorAci == null || reaction.targetSentTimestamp == null) {
        return null
      }
      val aci = ACI.parseOrNull(reaction.targetAuthorAci)
      if (aci == null) {
        Log.w(TAG, "Cannot parse author UUID on reaction")
        return null
      }
      return SignalServiceDataMessage.Reaction(
        reaction.emoji,
        java.lang.Boolean.TRUE == reaction.remove,
        aci,
        reaction.targetSentTimestamp
      )
    }

    private fun createRemoteDelete(content: DataMessage): RemoteDelete? {
      val delete = content.delete
      if (delete?.targetSentTimestamp == null) {
        return null
      }
      return RemoteDelete(delete.targetSentTimestamp)
    }

    private fun createGroupCallUpdate(content: DataMessage): SignalServiceDataMessage.GroupCallUpdate? {
      if (content.groupCallUpdate == null) {
        return null
      }
      val groupCallUpdate = content.groupCallUpdate
      return SignalServiceDataMessage.GroupCallUpdate(
        groupCallUpdate.eraId
      )
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createPayment(content: DataMessage): SignalServiceDataMessage.Payment? {
      if (content.payment == null) {
        return null
      }
      val payment = content.payment
      return if (payment.notification != null) {
        SignalServiceDataMessage.Payment(
          createPaymentNotification(payment),
          null
        )
      } else if (payment.activation != null) {
        SignalServiceDataMessage.Payment(
          null,
          createPaymentActivation(payment)
        )
      } else {
        throw InvalidMessageStructureException("Unknown payment item")
      }
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createStoryContext(content: DataMessage): SignalServiceDataMessage.StoryContext? {
      if (content.storyContext == null) {
        return null
      }
      val aci = ACI.parseOrNull(content.storyContext.authorAci)
        ?: throw InvalidMessageStructureException("Invalid author ACI!")
      return SignalServiceDataMessage.StoryContext(aci, content.storyContext.sentTimestamp ?: 0)
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createGiftBadge(content: DataMessage): SignalServiceDataMessage.GiftBadge? {
      if (content.giftBadge == null) {
        return null
      }
      if (content.giftBadge.receiptCredentialPresentation == null) {
        throw InvalidMessageStructureException("GiftBadge does not contain a receipt credential presentation!")
      }
      return try {
        val receiptCredentialPresentation = ReceiptCredentialPresentation(
          content.giftBadge.receiptCredentialPresentation.toByteArray()
        )
        SignalServiceDataMessage.GiftBadge(receiptCredentialPresentation)
      } catch (invalidInputException: InvalidInputException) {
        throw InvalidMessageStructureException(invalidInputException)
      }
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createPaymentNotification(content: DataMessage.Payment): PaymentNotification {
      val payment = content.notification
      if (payment?.mobileCoin?.receipt == null) {
        throw InvalidMessageStructureException("Badly-formatted payment notification!")
      }
      return PaymentNotification(
        payment.mobileCoin.receipt.toByteArray(),
        payment.note ?: ""
      )
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createPaymentActivation(content: DataMessage.Payment): PaymentActivation {
      val payment = content.activation
      if (payment?.type == null) {
        throw InvalidMessageStructureException("Badly-formatted payment activation!")
      }
      return PaymentActivation(payment.type)
    }

    @Throws(InvalidMessageStructureException::class)
    fun createSharedContacts(content: DataMessage): List<SharedContact>? {
      if (content.contact.isEmpty()) return null
      val results: MutableList<SharedContact> = LinkedList()
      for (contact in content.contact) {
        val builder = SharedContact.newBuilder()
          .setName(
            SharedContact.Name.newBuilder()
              .setDisplay(contact.name?.displayName)
              .setFamily(contact.name?.familyName)
              .setGiven(contact.name?.givenName)
              .setMiddle(contact.name?.middleName)
              .setPrefix(contact.name?.prefix)
              .setSuffix(contact.name?.suffix)
              .build()
          )
        if (contact.address.isNotEmpty()) {
          for (address in contact.address) {
            val type = when (address.type) {
              DataMessage.Contact.PostalAddress.Type.WORK -> SharedContact.PostalAddress.Type.WORK
              DataMessage.Contact.PostalAddress.Type.HOME -> SharedContact.PostalAddress.Type.HOME
              DataMessage.Contact.PostalAddress.Type.CUSTOM -> SharedContact.PostalAddress.Type.CUSTOM
              null -> SharedContact.PostalAddress.Type.HOME
            }
            builder.withAddress(
              SharedContact.PostalAddress.newBuilder()
                .setCity(address.city)
                .setCountry(address.country)
                .setLabel(address.label)
                .setNeighborhood(address.neighborhood)
                .setPobox(address.pobox)
                .setPostcode(address.postcode)
                .setRegion(address.region)
                .setStreet(address.street)
                .setType(type)
                .build()
            )
          }
        }
        if (contact.number.isNotEmpty()) {
          for (phone in contact.number) {
            val type = when (phone.type) {
              DataMessage.Contact.Phone.Type.HOME -> SharedContact.Phone.Type.HOME
              DataMessage.Contact.Phone.Type.WORK -> SharedContact.Phone.Type.WORK
              DataMessage.Contact.Phone.Type.MOBILE -> SharedContact.Phone.Type.MOBILE
              DataMessage.Contact.Phone.Type.CUSTOM -> SharedContact.Phone.Type.CUSTOM
              null -> SharedContact.Phone.Type.HOME
            }
            builder.withPhone(
              SharedContact.Phone.newBuilder()
                .setLabel(phone.label)
                .setType(type)
                .setValue(phone.value_)
                .build()
            )
          }
        }
        if (contact.email.isNotEmpty()) {
          for (email in contact.email) {
            val type = when (email.type) {
              DataMessage.Contact.Email.Type.HOME -> SharedContact.Email.Type.HOME
              DataMessage.Contact.Email.Type.WORK -> SharedContact.Email.Type.WORK
              DataMessage.Contact.Email.Type.MOBILE -> SharedContact.Email.Type.MOBILE
              DataMessage.Contact.Email.Type.CUSTOM -> SharedContact.Email.Type.CUSTOM
              null -> SharedContact.Email.Type.HOME
            }
            builder.withEmail(
              SharedContact.Email.newBuilder()
                .setLabel(email.label)
                .setType(type)
                .setValue(email.value_)
                .build()
            )
          }
        }
        if (contact.avatar != null) {
          builder.setAvatar(
            SharedContact.Avatar.newBuilder()
              .withAttachment(createAttachmentPointer(contact.avatar.avatar))
              .withProfileFlag(java.lang.Boolean.TRUE == contact.avatar.isProfile)
              .build()
          )
        }
        if (contact.organization != null) {
          builder.withOrganization(contact.organization)
        }
        results.add(builder.build())
      }
      return results
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createAttachmentPointer(pointer: AttachmentPointer?): SignalServiceAttachmentPointer {
      return AttachmentPointerUtil.createSignalAttachmentPointer(pointer)
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createTextAttachment(attachment: TextAttachment?): SignalServiceTextAttachment {
      val style = when (attachment?.textStyle) {
        TextAttachment.Style.DEFAULT -> SignalServiceTextAttachment.Style.DEFAULT
        TextAttachment.Style.REGULAR -> SignalServiceTextAttachment.Style.REGULAR
        TextAttachment.Style.BOLD -> SignalServiceTextAttachment.Style.BOLD
        TextAttachment.Style.SERIF -> SignalServiceTextAttachment.Style.SERIF
        TextAttachment.Style.SCRIPT -> SignalServiceTextAttachment.Style.SCRIPT
        TextAttachment.Style.CONDENSED -> SignalServiceTextAttachment.Style.CONDENSED
        null -> TODO()
      }
      val text = Optional.ofNullable(
        attachment.text
      )
      val textForegroundColor = Optional.ofNullable(
        attachment.textForegroundColor
      )
      val textBackgroundColor = Optional.ofNullable(
        attachment.textBackgroundColor
      )
      val preview = Optional.ofNullable(
        if (attachment.preview != null) {
          createPreview(
            attachment.preview
          )
        } else {
          null
        }
      )
      if (attachment.gradient != null) {
        val attachmentGradient = attachment.gradient
        val startColor = attachmentGradient.startColor
        val endColor = attachmentGradient.endColor
        val angle = attachmentGradient.angle
        val colors: List<Int>
        val positions: List<Float>
        if (attachmentGradient.colors.isNotEmpty() && attachmentGradient.colors.size == attachmentGradient.positions.size) {
          colors = ArrayList(attachmentGradient.colors)
          positions = ArrayList(attachmentGradient.positions)
        } else if (startColor != null && endColor != null) {
          colors = listOf(startColor, endColor)
          positions = listOf(0f, 1f)
        } else {
          colors = emptyList()
          positions = emptyList()
        }
        val gradient = SignalServiceTextAttachment.Gradient(
          Optional.ofNullable(angle),
          colors,
          positions
        )
        return SignalServiceTextAttachment.forGradientBackground(
          text,
          Optional.ofNullable(style),
          textForegroundColor,
          textBackgroundColor,
          preview,
          gradient
        )
      } else if (attachment.color != null) {
        return SignalServiceTextAttachment.forSolidBackground(
          text,
          Optional.ofNullable(style),
          textForegroundColor,
          textBackgroundColor,
          preview,
          attachment.color
        )
      }
      throw InvalidMessageStructureException("Missing gradient or color")
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createGroupV1Info(content: DataMessage): SignalServiceGroup? {
      if (content.group == null) return null
      val type = when (content.group.type) {
        GroupContext.Type.DELIVER -> SignalServiceGroup.Type.DELIVER
        GroupContext.Type.UPDATE -> SignalServiceGroup.Type.UPDATE
        GroupContext.Type.QUIT -> SignalServiceGroup.Type.QUIT
        GroupContext.Type.REQUEST_INFO -> SignalServiceGroup.Type.REQUEST_INFO
        GroupContext.Type.UNKNOWN, null -> SignalServiceGroup.Type.UNKNOWN
      }
      if (content.group.id == null) {
        throw InvalidMessageStructureException("Group had no id!")
      }
      if (content.group.type !== GroupContext.Type.DELIVER) {
        var name: String? = null
        var members: MutableList<SignalServiceAddress?>? = null
        var avatar: SignalServiceAttachmentPointer? = null
        if (content.group.name != null) {
          name = content.group.name
        }
        if (content.group.members.isNotEmpty()) {
          members = ArrayList(content.group.members.size)
          for (member in content.group.members) {
            if (!member.e164.isNullOrEmpty()) {
              members.add(SignalServiceAddress(ACI.UNKNOWN, member.e164))
            } else {
              throw InvalidMessageStructureException("GroupContext.Member had no address!")
            }
          }
        } else if (content.group.membersE164.isNotEmpty()) {
          members = ArrayList(content.group.membersE164.size)
          for (member in content.group.membersE164) {
            members.add(SignalServiceAddress(ACI.UNKNOWN, member))
          }
        }
        val pointer = content.group.avatar
        if (pointer?.key != null) {
          avatar = SignalServiceAttachmentPointer(
            pointer.cdnNumber ?: 0,
            SignalServiceAttachmentRemoteId.from(pointer),
            pointer.contentType,
            pointer.key.toByteArray(),
            Optional.ofNullable(pointer.size),
            Optional.empty(), 0, 0,
            Optional.ofNullable(if (pointer.digest != null) pointer.digest.toByteArray() else null),
            Optional.ofNullable(if (pointer.incrementalMac != null) pointer.incrementalMac.toByteArray() else null),
            pointer.incrementalMacChunkSize ?: 0,
            Optional.empty(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            pointer.uploadTimestamp ?: 0
          )
        }
        return SignalServiceGroup(
          type,
          content.group.id.toByteArray(),
          name,
          members,
          avatar
        )
      }
      return SignalServiceGroup(content.group.id.toByteArray())
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createGroupV2Info(storyMessage: StoryMessage): SignalServiceGroupV2? {
      return if (storyMessage.group == null) {
        null
      } else {
        createGroupV2Info(storyMessage.group)
      }
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createGroupV2Info(dataMessage: DataMessage): SignalServiceGroupV2? {
      return if (dataMessage.groupV2 == null) {
        null
      } else {
        createGroupV2Info(dataMessage.groupV2)
      }
    }

    @Throws(InvalidMessageStructureException::class)
    private fun createGroupV2Info(groupV2: GroupContextV2): SignalServiceGroupV2? {
      if (groupV2.masterKey == null) {
        throw InvalidMessageStructureException("No GV2 master key on message")
      }
      if (groupV2.revision == null) {
        throw InvalidMessageStructureException("No GV2 revision on message")
      }
      val builder: SignalServiceGroupV2.Builder = try {
        SignalServiceGroupV2.newBuilder(GroupMasterKey(groupV2.masterKey.toByteArray()))
          .withRevision(groupV2.revision)
      } catch (e: InvalidInputException) {
        throw InvalidMessageStructureException("Invalid GV2 input!")
      }
      if (groupV2.groupChange != null && groupV2.groupChange.toByteArray().isNotEmpty()) {
        builder.withSignedGroupChange(groupV2.groupChange.toByteArray())
      }
      return builder.build()
    }
  }
}
