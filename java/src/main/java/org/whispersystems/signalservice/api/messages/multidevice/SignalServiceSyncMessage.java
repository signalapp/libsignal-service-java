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
  private final Optional<SignalServiceAttachment> contacts;
  private final Optional<SignalServiceAttachment> groups;
  private final Optional<BlockedListMessage>      blockedList;
  private final Optional<RequestMessage>          request;
  private final Optional<List<ReadMessage>>       reads;

  private SignalServiceSyncMessage(Optional<SentTranscriptMessage>   sent,
                                   Optional<SignalServiceAttachment> contacts,
                                   Optional<SignalServiceAttachment> groups,
                                   Optional<BlockedListMessage>      blockedList,
                                   Optional<RequestMessage>          request,
                                   Optional<List<ReadMessage>>       reads)
  {
    this.sent        = sent;
    this.contacts    = contacts;
    this.groups      = groups;
    this.blockedList = blockedList;
    this.request     = request;
    this.reads       = reads;
  }

  public static SignalServiceSyncMessage forSentTranscript(SentTranscriptMessage sent) {
    return new SignalServiceSyncMessage(Optional.of(sent),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent());
  }

  public static SignalServiceSyncMessage forContacts(SignalServiceAttachment contacts) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.of(contacts),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent());
  }

  public static SignalServiceSyncMessage forGroups(SignalServiceAttachment groups) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.of(groups),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent());
  }

  public static SignalServiceSyncMessage forRequest(RequestMessage request) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.of(request),
                                        Optional.<List<ReadMessage>>absent());
  }

  public static SignalServiceSyncMessage forRead(List<ReadMessage> reads) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.of(reads));
  }

  public static SignalServiceSyncMessage forRead(ReadMessage read) {
    List<ReadMessage> reads = new LinkedList<>();
    reads.add(read);

    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.of(reads));
  }

  public static SignalServiceSyncMessage forBlocked(BlockedListMessage blocked) {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.of(blocked),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent());
  }

  public static SignalServiceSyncMessage empty() {
    return new SignalServiceSyncMessage(Optional.<SentTranscriptMessage>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<SignalServiceAttachment>absent(),
                                        Optional.<BlockedListMessage>absent(),
                                        Optional.<RequestMessage>absent(),
                                        Optional.<List<ReadMessage>>absent());
  }

  public Optional<SentTranscriptMessage> getSent() {
    return sent;
  }

  public Optional<SignalServiceAttachment> getGroups() {
    return groups;
  }

  public Optional<SignalServiceAttachment> getContacts() {
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

}
