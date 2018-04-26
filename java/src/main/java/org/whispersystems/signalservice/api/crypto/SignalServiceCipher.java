/*
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.crypto;

import com.google.protobuf.InvalidProtocolBufferException;

import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKey;
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
import org.whispersystems.signalservice.api.messages.SignalServiceReceiptMessage;
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
import org.whispersystems.signalservice.api.messages.multidevice.VerifiedMessage;
import org.whispersystems.signalservice.api.messages.multidevice.VerifiedMessage.VerifiedState;
import org.whispersystems.signalservice.api.messages.shared.SharedContact;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.internal.push.OutgoingPushMessage;
import org.whispersystems.signalservice.internal.push.PushTransportDetails;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.AttachmentPointer;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.Content;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.DataMessage;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.Envelope.Type;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.ReceiptMessage;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.SyncMessage;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.Verified;
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

  @SuppressWarnings("unused")
  private static final String TAG = SignalServiceCipher.class.getSimpleName();

  private final SignalProtocolStore  signalProtocolStore;
  private final SignalServiceAddress localAddress;

  public SignalServiceCipher(SignalServiceAddress localAddress, SignalProtocolStore signalProtocolStore) {
    this.signalProtocolStore = signalProtocolStore;
    this.localAddress = localAddress;
  }

  public OutgoingPushMessage encrypt(SignalProtocolAddress destination, byte[] unpaddedMessage, boolean silent)
      throws UntrustedIdentityException
  {
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

    return new OutgoingPushMessage(type, destination.getDeviceId(), remoteRegistrationId, body, silent);
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
        } else if (message.hasReceiptMessage()) {
          content = new SignalServiceContent(createReceiptMessage(envelope, message.getReceiptMessage()));
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

  private SignalServiceDataMessage createSignalServiceMessage(SignalServiceEnvelope envelope, DataMessage content) throws InvalidMessageException {
    SignalServiceGroup             groupInfo        = createGroupInfo(envelope, content);
    List<SignalServiceAttachment>  attachments      = new LinkedList<>();
    boolean                        endSession       = ((content.getFlags() & DataMessage.Flags.END_SESSION_VALUE            ) != 0);
    boolean                        expirationUpdate = ((content.getFlags() & DataMessage.Flags.EXPIRATION_TIMER_UPDATE_VALUE) != 0);
    boolean                        profileKeyUpdate = ((content.getFlags() & DataMessage.Flags.PROFILE_KEY_UPDATE_VALUE     ) != 0);
    SignalServiceDataMessage.Quote quote            = createQuote(envelope, content);
    List<SharedContact>            sharedContacts   = createSharedContacts(envelope, content);

    for (AttachmentPointer pointer : content.getAttachmentsList()) {
      attachments.add(createAttachmentPointer(envelope, pointer));
    }

    if (content.hasTimestamp() && content.getTimestamp() != envelope.getTimestamp()) {
      throw new InvalidMessageException("Timestamps don't match: " + content.getTimestamp() + " vs " + envelope.getTimestamp());
    }

    return new SignalServiceDataMessage(envelope.getTimestamp(), groupInfo, attachments,
                                        content.getBody(), endSession, content.getExpireTimer(),
                                        expirationUpdate, content.hasProfileKey() ? content.getProfileKey().toByteArray() : null,
                                        profileKeyUpdate, quote, sharedContacts);
  }

  private SignalServiceSyncMessage createSynchronizeMessage(SignalServiceEnvelope envelope, SyncMessage content) throws InvalidMessageException {
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

    if (content.hasVerified()) {
      try {
        Verified    verified    = content.getVerified();
        String      destination = verified.getDestination();
        IdentityKey identityKey = new IdentityKey(verified.getIdentityKey().toByteArray(), 0);

        VerifiedState verifiedState;

        if (verified.getState() == Verified.State.DEFAULT) {
          verifiedState = VerifiedState.DEFAULT;
        } else if (verified.getState() == Verified.State.VERIFIED) {
          verifiedState = VerifiedState.VERIFIED;
        } else if (verified.getState() == Verified.State.UNVERIFIED) {
          verifiedState = VerifiedState.UNVERIFIED;
        } else {
          throw new InvalidMessageException("Unknown state: " + verified.getState().getNumber());
        }

        return SignalServiceSyncMessage.forVerified(new VerifiedMessage(destination, identityKey, verifiedState, System.currentTimeMillis()));
      } catch (InvalidKeyException e) {
        throw new InvalidMessageException(e);
      }
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

  private SignalServiceReceiptMessage createReceiptMessage(SignalServiceEnvelope envelope, ReceiptMessage content) {
    SignalServiceReceiptMessage.Type type;

    if      (content.getType() == ReceiptMessage.Type.DELIVERY) type = SignalServiceReceiptMessage.Type.DELIVERY;
    else if (content.getType() == ReceiptMessage.Type.READ)     type = SignalServiceReceiptMessage.Type.READ;
    else                                                        type = SignalServiceReceiptMessage.Type.UNKNOWN;

    return new SignalServiceReceiptMessage(type, content.getTimestampList(), envelope.getTimestamp());
  }

  private SignalServiceDataMessage.Quote createQuote(SignalServiceEnvelope envelope, DataMessage content) {
    if (!content.hasQuote()) return null;

    List<SignalServiceDataMessage.Quote.QuotedAttachment> attachments = new LinkedList<>();

    for (DataMessage.Quote.QuotedAttachment attachment : content.getQuote().getAttachmentsList()) {
      attachments.add(new SignalServiceDataMessage.Quote.QuotedAttachment(attachment.getContentType(),
                                                                          attachment.getFileName(),
                                                                          attachment.hasThumbnail() ? createAttachmentPointer(envelope, attachment.getThumbnail()) : null));
    }

    return new SignalServiceDataMessage.Quote(content.getQuote().getId(),
                                              new SignalServiceAddress(content.getQuote().getAuthor()),
                                              content.getQuote().getText(),
                                              attachments);
  }

  private List<SharedContact> createSharedContacts(SignalServiceEnvelope envelope, DataMessage content) {
    if (content.getContactCount() <= 0) return null;

    List<SharedContact> results = new LinkedList<>();

    for (DataMessage.Contact contact : content.getContactList()) {
      SharedContact.Builder builder = SharedContact.newBuilder()
                                                   .setName(SharedContact.Name.newBuilder()
                                                                              .setFamily(contact.getName().getFamilyName())
                                                                              .setGiven(contact.getName().getGivenName())
                                                                              .setMiddle(contact.getName().getMiddleName())
                                                                              .setPrefix(contact.getName().getPrefix())
                                                                              .setSuffix(contact.getName().getSuffix())
                                                                              .build());

      if (contact.getAddressCount() > 0) {
        for (DataMessage.Contact.PostalAddress address : contact.getAddressList()) {
          SharedContact.PostalAddress.Type type = SharedContact.PostalAddress.Type.HOME;

          switch (address.getType()) {
            case WORK:   type = SharedContact.PostalAddress.Type.WORK;   break;
            case HOME:   type = SharedContact.PostalAddress.Type.HOME;   break;
            case CUSTOM: type = SharedContact.PostalAddress.Type.CUSTOM; break;
          }

          builder.withAddress(SharedContact.PostalAddress.newBuilder()
                                                         .setCity(address.getCity())
                                                         .setCountry(address.getCountry())
                                                         .setLabel(address.getLabel())
                                                         .setNeighborhood(address.getNeighborhood())
                                                         .setPobox(address.getPobox())
                                                         .setPostcode(address.getPostcode())
                                                         .setRegion(address.getRegion())
                                                         .setStreet(address.getStreet())
                                                         .setType(type)
                                                         .build());
        }

        if (contact.getNumberCount() > 0) {
          for (DataMessage.Contact.Phone phone : contact.getNumberList()) {
            SharedContact.Phone.Type type = SharedContact.Phone.Type.HOME;

            switch (phone.getType()) {
              case HOME:   type = SharedContact.Phone.Type.HOME;   break;
              case WORK:   type = SharedContact.Phone.Type.WORK;   break;
              case MOBILE: type = SharedContact.Phone.Type.MOBILE; break;
              case CUSTOM: type = SharedContact.Phone.Type.CUSTOM; break;
            }

            builder.withPhone(SharedContact.Phone.newBuilder()
                                                 .setLabel(phone.getLabel())
                                                 .setType(type)
                                                 .setValue(phone.getValue())
                                                 .build());
          }

          if (contact.getEmailCount() > 0) {
            for (DataMessage.Contact.Email email : contact.getEmailList()) {
              SharedContact.Email.Type type = SharedContact.Email.Type.HOME;

              switch (email.getType()) {
                case HOME:   type = SharedContact.Email.Type.HOME;   break;
                case WORK:   type = SharedContact.Email.Type.WORK;   break;
                case MOBILE: type = SharedContact.Email.Type.MOBILE; break;
                case CUSTOM: type = SharedContact.Email.Type.CUSTOM; break;
              }

              builder.withEmail(SharedContact.Email.newBuilder()
                                                   .setLabel(email.getLabel())
                                                   .setType(type)
                                                   .setValue(email.getValue())
                                                   .build());
            }
          }

          if (contact.hasAvatar()) {
            builder.setAvatar(createAttachmentPointer(envelope, contact.getAvatar()));
          }
        }

      }


      results.add(builder.build());
    }

    return results;
  }

  private SignalServiceAttachmentPointer createAttachmentPointer(SignalServiceEnvelope envelope, AttachmentPointer pointer) {
    return new SignalServiceAttachmentPointer(pointer.getId(),
                                              pointer.getContentType(),
                                              pointer.getKey().toByteArray(),
                                              envelope.getRelay(),
                                              pointer.hasSize() ? Optional.of(pointer.getSize()) : Optional.<Integer>absent(),
                                              pointer.hasThumbnail() ? Optional.of(pointer.getThumbnail().toByteArray()): Optional.<byte[]>absent(),
                                              pointer.getWidth(), pointer.getHeight(),
                                              pointer.hasDigest() ? Optional.of(pointer.getDigest().toByteArray()) : Optional.<byte[]>absent(),
                                              pointer.hasFileName() ? Optional.of(pointer.getFileName()) : Optional.<String>absent(),
                                              (pointer.getFlags() & AttachmentPointer.Flags.VOICE_MESSAGE_VALUE) != 0);

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
                                                    Optional.of(pointer.getSize()),
                                                    Optional.<byte[]>absent(), 0, 0,
                                                    Optional.fromNullable(pointer.hasDigest() ? pointer.getDigest().toByteArray() : null),
                                                    Optional.<String>absent(),
                                                    false);
      }

      return new SignalServiceGroup(type, content.getGroup().getId().toByteArray(), name, members, avatar);
    }

    return new SignalServiceGroup(content.getGroup().getId().toByteArray());
  }


}

