/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.crypto;

import com.google.protobuf.InvalidProtocolBufferException;

import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.calls.AnswerMessage;
import org.whispersystems.signalservice.api.messages.calls.BusyMessage;
import org.whispersystems.signalservice.api.messages.calls.HangupMessage;
import org.whispersystems.signalservice.api.messages.calls.IceUpdateMessage;
import org.whispersystems.signalservice.api.messages.calls.OfferMessage;
import org.whispersystems.signalservice.api.messages.calls.SignalServiceCallMessage;
import org.whispersystems.signalservice.api.messages.multidevice.ReadMessage;
import org.whispersystems.signalservice.api.messages.multidevice.RequestMessage;
import org.whispersystems.signalservice.api.messages.multidevice.SentTranscriptMessage;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.internal.push.OutgoingPushMessage;
import org.whispersystems.signalservice.internal.push.PushTransportDetails;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.AttachmentPointer;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.Content;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.DataMessage;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.Envelope.Type;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.SyncMessage;
import org.whispersystems.signalservice.internal.util.Base64;

import java.util.LinkedList;
import java.util.List;

import static org.whispersystems.signalservice.internal.push.SignalServiceProtos.CallMessage;
import static org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext.Type.DELIVER;

/**
 * This is used to decrypt received {@link SignalServiceEnvelope}s.
 *
 * @author Moxie Marlinspike
 */
public class SignalServiceCipher {

  private static final String TAG = SignalServiceCipher.class.getSimpleName();

  private final SignalProtocolStore      signalProtocolStore;
  private final SignalServiceAddress localAddress;

  public SignalServiceCipher(SignalServiceAddress localAddress, SignalProtocolStore signalProtocolStore) {
    this.signalProtocolStore = signalProtocolStore;
    this.localAddress = localAddress;
  }

  public OutgoingPushMessage encrypt(SignalProtocolAddress destination, byte[] unpaddedMessage, boolean legacy, boolean silent) {
    SessionCipher        sessionCipher        = new SessionCipher(signalProtocolStore, destination);
    PushTransportDetails transportDetails     = new PushTransportDetails(sessionCipher.getSessionVersion());
    CiphertextMessage    message              = sessionCipher.encrypt(transportDetails.getPaddedMessageBody(unpaddedMessage));
    int                  remoteRegistrationId = sessionCipher.getRemoteRegistrationId();
    String               body                 = Base64.encodeBytes(message.serialize());

    int type;

    switch (message.getType()) {
      case CiphertextMessage.PREKEY_TYPE:  type = Type.PREKEY_BUNDLE_VALUE; break;
      case CiphertextMessage.WHISPER_TYPE: type = Type.CIPHERTEXT_VALUE;    break;
      default: throw new AssertionError("Bad type: " + message.getType());
    }

    return new OutgoingPushMessage(type, destination.getDeviceId(), remoteRegistrationId,
                                   legacy ? body : null, legacy ? null : body, silent);
  }

  /**
   * Decrypt a received {@link SignalServiceEnvelope}
   *
   * @param envelope The received SignalServiceEnvelope
   *
   * @return a decrypted SignalServiceContent
   * @throws InvalidVersionException
   * @throws InvalidMessageException
   * @throws InvalidKeyException
   * @throws DuplicateMessageException
   * @throws InvalidKeyIdException
   * @throws UntrustedIdentityException
   * @throws LegacyMessageException
   * @throws NoSessionException
   */
  public SignalServiceContent decrypt(SignalServiceEnvelope envelope)
      throws InvalidVersionException, InvalidMessageException, InvalidKeyException,
             DuplicateMessageException, InvalidKeyIdException, UntrustedIdentityException,
             LegacyMessageException, NoSessionException
  {
    try {
      SignalServiceContent content = new SignalServiceContent();

      if (envelope.hasLegacyMessage()) {
        DataMessage message = DataMessage.parseFrom(decrypt(envelope, envelope.getLegacyMessage()));
        content = new SignalServiceContent(createSignalServiceMessage(envelope, message));
      } else if (envelope.hasContent()) {
        Content message = Content.parseFrom(decrypt(envelope, envelope.getContent()));

        if (message.hasDataMessage()) {
          content = new SignalServiceContent(createSignalServiceMessage(envelope, message.getDataMessage()));
        } else if (message.hasSyncMessage() && localAddress.getNumber().equals(envelope.getSource())) {
          content = new SignalServiceContent(createSynchronizeMessage(envelope, message.getSyncMessage()));
        } else if (message.hasCallMessage()) {
          content = new SignalServiceContent(createCallMessage(message.getCallMessage()));
        }
      }

      return content;
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidMessageException(e);
    }
  }

  private byte[] decrypt(SignalServiceEnvelope envelope, byte[] ciphertext)
      throws InvalidVersionException, InvalidMessageException, InvalidKeyException,
             DuplicateMessageException, InvalidKeyIdException, UntrustedIdentityException,
             LegacyMessageException, NoSessionException
  {
    SignalProtocolAddress sourceAddress = new SignalProtocolAddress(envelope.getSource(), envelope.getSourceDevice());
    SessionCipher         sessionCipher = new SessionCipher(signalProtocolStore, sourceAddress);

    byte[] paddedMessage;

    if (envelope.isPreKeySignalMessage()) {
      paddedMessage = sessionCipher.decrypt(new PreKeySignalMessage(ciphertext));
    } else if (envelope.isSignalMessage()) {
      paddedMessage = sessionCipher.decrypt(new SignalMessage(ciphertext));
    } else {
      throw new InvalidMessageException("Unknown type: " + envelope.getType());
    }

    PushTransportDetails transportDetails = new PushTransportDetails(sessionCipher.getSessionVersion());
    return transportDetails.getStrippedPaddingMessageBody(paddedMessage);
  }

  private SignalServiceDataMessage createSignalServiceMessage(SignalServiceEnvelope envelope, DataMessage content) {
    SignalServiceGroup            groupInfo        = createGroupInfo(envelope, content);
    List<SignalServiceAttachment> attachments      = new LinkedList<>();
    boolean                       endSession       = ((content.getFlags() & DataMessage.Flags.END_SESSION_VALUE) != 0);
    boolean                       expirationUpdate = ((content.getFlags() & DataMessage.Flags.EXPIRATION_TIMER_UPDATE_VALUE) != 0);

    for (AttachmentPointer pointer : content.getAttachmentsList()) {
      attachments.add(new SignalServiceAttachmentPointer(pointer.getId(),
                                                         pointer.getContentType(),
                                                         pointer.getKey().toByteArray(),
                                                         envelope.getRelay(),
                                                         pointer.hasSize() ? Optional.of(pointer.getSize()) : Optional.<Integer>absent(),
                                                         pointer.hasThumbnail() ? Optional.of(pointer.getThumbnail().toByteArray()): Optional.<byte[]>absent(),
                                                         pointer.hasDigest() ? Optional.of(pointer.getDigest().toByteArray()) : Optional.<byte[]>absent(),
                                                         pointer.hasFileName() ? Optional.of(pointer.getFileName()) : Optional.<String>absent()));
    }

    return new SignalServiceDataMessage(envelope.getTimestamp(), groupInfo, attachments,
                                        content.getBody(), endSession, content.getExpireTimer(),
                                        expirationUpdate);
  }

  private SignalServiceSyncMessage createSynchronizeMessage(SignalServiceEnvelope envelope, SyncMessage content) {
    if (content.hasSent()) {
      SyncMessage.Sent sentContent = content.getSent();
      return SignalServiceSyncMessage.forSentTranscript(new SentTranscriptMessage(sentContent.getDestination(),
                                                                                  sentContent.getTimestamp(),
                                                                                  createSignalServiceMessage(envelope, sentContent.getMessage()),
                                                                                  sentContent.getExpirationStartTimestamp()));
    }

    if (content.hasRequest()) {
      return SignalServiceSyncMessage.forRequest(new RequestMessage(content.getRequest()));
    }

    if (content.getReadList().size() > 0) {
      List<ReadMessage> readMessages = new LinkedList<>();

      for (SyncMessage.Read read : content.getReadList()) {
        readMessages.add(new ReadMessage(read.getSender(), read.getTimestamp()));
      }

      return SignalServiceSyncMessage.forRead(readMessages);
    }

    return SignalServiceSyncMessage.empty();
  }

  private SignalServiceCallMessage createCallMessage(CallMessage content) {
    if (content.hasOffer()) {
      CallMessage.Offer offerContent = content.getOffer();
      return SignalServiceCallMessage.forOffer(new OfferMessage(offerContent.getId(), offerContent.getDescription()));
    } else if (content.hasAnswer()) {
      CallMessage.Answer answerContent = content.getAnswer();
      return SignalServiceCallMessage.forAnswer(new AnswerMessage(answerContent.getId(), answerContent.getDescription()));
    } else if (content.getIceUpdateCount() > 0) {
      List<IceUpdateMessage> iceUpdates = new LinkedList<>();

      for (CallMessage.IceUpdate iceUpdate : content.getIceUpdateList()) {
        iceUpdates.add(new IceUpdateMessage(iceUpdate.getId(), iceUpdate.getSdpMid(), iceUpdate.getSdpMLineIndex(), iceUpdate.getSdp()));
      }

      return SignalServiceCallMessage.forIceUpdates(iceUpdates);
    } else if (content.hasHangup()) {
      CallMessage.Hangup hangup = content.getHangup();
      return SignalServiceCallMessage.forHangup(new HangupMessage(hangup.getId()));
    } else if (content.hasBusy()) {
      CallMessage.Busy busy = content.getBusy();
      return SignalServiceCallMessage.forBusy(new BusyMessage(busy.getId()));
    }

    return SignalServiceCallMessage.empty();
  }

  private SignalServiceGroup createGroupInfo(SignalServiceEnvelope envelope, DataMessage content) {
    if (!content.hasGroup()) return null;

    SignalServiceGroup.Type type;

    switch (content.getGroup().getType()) {
      case DELIVER:      type = SignalServiceGroup.Type.DELIVER;      break;
      case UPDATE:       type = SignalServiceGroup.Type.UPDATE;       break;
      case QUIT:         type = SignalServiceGroup.Type.QUIT;         break;
      case REQUEST_INFO: type = SignalServiceGroup.Type.REQUEST_INFO; break;
      default:           type = SignalServiceGroup.Type.UNKNOWN;      break;
    }

    if (content.getGroup().getType() != DELIVER) {
      String                      name    = null;
      List<String>                members = null;
      SignalServiceAttachmentPointer avatar  = null;

      if (content.getGroup().hasName()) {
        name = content.getGroup().getName();
      }

      if (content.getGroup().getMembersCount() > 0) {
        members = content.getGroup().getMembersList();
      }

      if (content.getGroup().hasAvatar()) {
        AttachmentPointer pointer = content.getGroup().getAvatar();

        avatar = new SignalServiceAttachmentPointer(pointer.getId(),
                                                    pointer.getContentType(),
                                                    pointer.getKey().toByteArray(),
                                                    envelope.getRelay(),
                                                    pointer.hasDigest() ? Optional.of(pointer.getDigest().toByteArray()) : Optional.<byte[]>absent(),
                                                    Optional.<String>absent());
      }

      return new SignalServiceGroup(type, content.getGroup().getId().toByteArray(), name, members, avatar);
    }

    return new SignalServiceGroup(content.getGroup().getId().toByteArray());
  }


}

