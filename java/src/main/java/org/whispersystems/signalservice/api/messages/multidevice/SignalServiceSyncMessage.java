/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages.multidevice;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;

import java.util.LinkedList;
import java.util.List;

public class SignalServiceSyncMessage {

  private final Optional<SentTranscriptMessage>   sent;
  private final Optional<ContactsMessage>         contacts;
  private final Optional<SignalServiceAttachment> groups;
  private final Optional<BlockedListMessage>      blockedList;
  private final Optional<RequestMessage>          request;
  private final Optional<List<ReadMessage>>       reads;
  private final Optional<VerifiedMessage>         verified;
  private final Optional<ConfigurationMessage>    configuration;

  private SignalServiceSyncMessage(Optional<SentTranscriptMessage>   sent,
                                   Optional<ContactsMessage>         contacts,
                                   Optional<SignalServiceAttachment> groups,
                                   Optional<BlockedListMessage>      blockedList,
                                   Optional<RequestMessage>          request,
                                   Optional<List<ReadMessage>>       reads,
                                   Optional<VerifiedMessage>         verified,
                                   Optional<ConfigurationMessage>    configuration)
  {
    this.sent          = sent;
    this.contacts      = contacts;
    this.groups        = groups;
    this.blockedList   = blockedList;
    this.request       = request;
    this.reads         = reads;
    this.verified      = verified;
    this.configuration = configuration;
  }

  public static SignalServiceSyncMessage forSentTranscript(SentTranscriptMessage sent) {
    return new SignalServiceSyncMessage(Optional.of(sent),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent(),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.<ConfigurationMessage>absent());
  }

  public static SignalServiceSyncMessage forContacts(ContactsMessage contacts) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.of(contacts),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent(),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.<ConfigurationMessage>absent());
  }

  public static SignalServiceSyncMessage forGroups(SignalServiceAttachment groups) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.of(groups),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent(),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.<ConfigurationMessage>absent());
  }

  public static SignalServiceSyncMessage forRequest(RequestMessage request) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.of(request),
                                        Optional.<List<ReadMessage>>absent(),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.<ConfigurationMessage>absent());
  }

  public static SignalServiceSyncMessage forRead(List<ReadMessage> reads) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.of(reads),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.<ConfigurationMessage>absent());
  }

  public static SignalServiceSyncMessage forRead(ReadMessage read) {
    List<ReadMessage> reads = new LinkedList<>();
    reads.add(read);

    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.of(reads),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.<ConfigurationMessage>absent());
  }

  public static SignalServiceSyncMessage forVerified(VerifiedMessage verifiedMessage) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent(),
                                        Optional.of(verifiedMessage),
                                        Optional.<ConfigurationMessage>absent());
  }

  public static SignalServiceSyncMessage forBlocked(BlockedListMessage blocked) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.of(blocked),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent(),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.<ConfigurationMessage>absent());
  }

  public static SignalServiceSyncMessage forConfiguration(ConfigurationMessage configuration) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent(),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.of(configuration));
  }

  public static SignalServiceSyncMessage empty() {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<ContactsMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent(),
                                        Optional.<VerifiedMessage>absent(),
                                        Optional.<ConfigurationMessage>absent());
  }

  public Optional<SentTranscriptMessage> getSent() {
    return sent;
  }

  public Optional<SignalServiceAttachment> getGroups() {
    return groups;
  }

  public Optional<ContactsMessage> getContacts() {
    return contacts;
  }

  public Optional<RequestMessage> getRequest() {
    return request;
  }

  public Optional<List<ReadMessage>> getRead() {
    return reads;
  }

  public Optional<BlockedListMessage> getBlockedList() {
    return blockedList;
  }

  public Optional<VerifiedMessage> getVerified() {
    return verified;
  }

  public Optional<ConfigurationMessage> getConfiguration() {
    return configuration;
  }

}
