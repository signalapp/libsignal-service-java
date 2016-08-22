/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a decrypted Signal Service data message.
 */
public class SignalServiceDataMessage {

  private final long                                    timestamp;
  private final Optional<List<SignalServiceAttachment>> attachments;
  private final Optional<String>                        body;
  private final Optional<SignalServiceGroup>            group;
  private final boolean                                 endSession;
  private final boolean                                 expirationUpdate;
  private final int                                     expiresInSeconds;

  /**
   * Construct a SignalServiceDataMessage with a body and no attachments.
   *
   * @param timestamp The sent timestamp.
   * @param body The message contents.
   */
  public SignalServiceDataMessage(long timestamp, String body) {
    this(timestamp, body, 0);
  }

  /**
   * Construct an expiring SignalServiceDataMessage with a body and no attachments.
   *
   * @param timestamp The sent timestamp.
   * @param body The message contents.
   * @param expiresInSeconds The number of seconds in which the message should expire after having been seen.
   */
  public SignalServiceDataMessage(long timestamp, String body, int expiresInSeconds) {
    this(timestamp, (List<SignalServiceAttachment>)null, body, expiresInSeconds);
  }


  public SignalServiceDataMessage(final long timestamp, final SignalServiceAttachment attachment, final String body) {
    this(timestamp, new LinkedList<SignalServiceAttachment>() {{add(attachment);}}, body);
  }

  /**
   * Construct a SignalServiceDataMessage with a body and list of attachments.
   *
   * @param timestamp The sent timestamp.
   * @param attachments The attachments.
   * @param body The message contents.
   */
  public SignalServiceDataMessage(long timestamp, List<SignalServiceAttachment> attachments, String body) {
    this(timestamp, attachments, body, 0);
  }

  /**
   * Construct an expiring SignalServiceDataMessage with a body and list of attachments.
   *
   * @param timestamp The sent timestamp.
   * @param attachments The attachments.
   * @param body The message contents.
   * @param expiresInSeconds The number of seconds in which the message should expire after having been seen.
   */
  public SignalServiceDataMessage(long timestamp, List<SignalServiceAttachment> attachments, String body, int expiresInSeconds) {
    this(timestamp, null, attachments, body, expiresInSeconds);
  }

  /**
   * Construct a SignalServiceDataMessage group message with attachments and body.
   *
   * @param timestamp The sent timestamp.
   * @param group The group information.
   * @param attachments The attachments.
   * @param body The message contents.
   */
  public SignalServiceDataMessage(long timestamp, SignalServiceGroup group, List<SignalServiceAttachment> attachments, String body) {
    this(timestamp, group, attachments, body, 0);
  }


  /**
   * Construct an expiring SignalServiceDataMessage group message with attachments and body.
   *
   * @param timestamp The sent timestamp.
   * @param group The group information.
   * @param attachments The attachments.
   * @param body The message contents.
   * @param expiresInSeconds The number of seconds in which a message should disappear after having been seen.
   */
  public SignalServiceDataMessage(long timestamp, SignalServiceGroup group, List<SignalServiceAttachment> attachments, String body, int expiresInSeconds) {
    this(timestamp, group, attachments, body, false, expiresInSeconds, false);
  }

  /**
   * Construct a SignalServiceDataMessage.
   *
   * @param timestamp The sent timestamp.
   * @param group The group information (or null if none).
   * @param attachments The attachments (or null if none).
   * @param body The message contents.
   * @param endSession Flag indicating whether this message should close a session.
   * @param expiresInSeconds Number of seconds in which the message should disappear after being seen.
   */
  public SignalServiceDataMessage(long timestamp, SignalServiceGroup group,
                                  List<SignalServiceAttachment> attachments,
                                  String body, boolean endSession, int expiresInSeconds,
                                  boolean expirationUpdate)
  {
    this.timestamp        = timestamp;
    this.body             = Optional.fromNullable(body);
    this.group            = Optional.fromNullable(group);
    this.endSession       = endSession;
    this.expiresInSeconds = expiresInSeconds;
    this.expirationUpdate = expirationUpdate;

    if (attachments != null && !attachments.isEmpty()) {
      this.attachments = Optional.of(attachments);
    } else {
      this.attachments = Optional.absent();
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * @return The message timestamp.
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @return The message attachments (if any).
   */
  public Optional<List<SignalServiceAttachment>> getAttachments() {
    return attachments;
  }

  /**
   * @return The message body (if any).
   */
  public Optional<String> getBody() {
    return body;
  }

  /**
   * @return The message group info (if any).
   */
  public Optional<SignalServiceGroup> getGroupInfo() {
    return group;
  }

  public boolean isEndSession() {
    return endSession;
  }

  public boolean isExpirationUpdate() {
    return expirationUpdate;
  }

  public boolean isGroupUpdate() {
    return group.isPresent() && group.get().getType() != SignalServiceGroup.Type.DELIVER;
  }

  public int getExpiresInSeconds() {
    return expiresInSeconds;
  }

  public static class Builder {

    private List<SignalServiceAttachment> attachments = new LinkedList<>();
    private long               timestamp;
    private SignalServiceGroup group;
    private String             body;
    private boolean            endSession;
    private int                expiresInSeconds;
    private boolean            expirationUpdate;

    private Builder() {}

    public Builder withTimestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder asGroupMessage(SignalServiceGroup group) {
      this.group = group;
      return this;
    }

    public Builder withAttachment(SignalServiceAttachment attachment) {
      this.attachments.add(attachment);
      return this;
    }

    public Builder withAttachments(List<SignalServiceAttachment> attachments) {
      this.attachments.addAll(attachments);
      return this;
    }

    public Builder withBody(String body) {
      this.body = body;
      return this;
    }

    public Builder asEndSessionMessage() {
      return asEndSessionMessage(true);
    }

    public Builder asEndSessionMessage(boolean endSession) {
      this.endSession = endSession;
      return this;
    }

    public Builder asExpirationUpdate() {
      return asExpirationUpdate(true);
    }

    public Builder asExpirationUpdate(boolean expirationUpdate) {
      this.expirationUpdate = expirationUpdate;
      return this;
    }

    public Builder withExpiration(int expiresInSeconds) {
      this.expiresInSeconds = expiresInSeconds;
      return this;
    }

    public SignalServiceDataMessage build() {
      if (timestamp == 0) timestamp = System.currentTimeMillis();
      return new SignalServiceDataMessage(timestamp, group, attachments, body, endSession,
                                          expiresInSeconds, expirationUpdate);
    }
  }
}
