/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;

/**
 * Represents a received SignalServiceAttachment "handle."  This
 * is a pointer to the actual attachment content, which needs to be
 * retrieved using {@link SignalServiceMessageReceiver#retrieveAttachment(SignalServiceAttachmentPointer, java.io.File, int)}
 *
 * @author Moxie Marlinspike
 */
public class SignalServiceAttachmentPointer extends SignalServiceAttachment {

  private final long              id;
  private final byte[]            key;
  private final Optional<String>  relay;
  private final Optional<Integer> size;
  private final Optional<byte[]>  preview;
  private final Optional<byte[]>  digest;
  private final Optional<String>  fileName;

  public SignalServiceAttachmentPointer(long id, String contentType, byte[] key, String relay, Optional<byte[]> digest, Optional<String> fileName) {
    this(id, contentType, key, relay, Optional.<Integer>absent(), Optional.<byte[]>absent(), digest, fileName);
  }

  public SignalServiceAttachmentPointer(long id, String contentType, byte[] key, String relay,
                                        Optional<Integer> size, Optional<byte[]> preview,
                                        Optional<byte[]> digest, Optional<String> fileName)
  {
    super(contentType);
    this.id       = id;
    this.key      = key;
    this.relay    = Optional.fromNullable(relay);
    this.size     = size;
    this.preview  = preview;
    this.digest   = digest;
    this.fileName = fileName;
  }

  public long getId() {
    return id;
  }

  public byte[] getKey() {
    return key;
  }

  @Override
  public boolean isStream() {
    return false;
  }

  @Override
  public boolean isPointer() {
    return true;
  }

  public Optional<String> getRelay() {
    return relay;
  }

  public Optional<Integer> getSize() {
    return size;
  }

  public Optional<String> getFileName() {
    return fileName;
  }

  public Optional<byte[]> getPreview() {
    return preview;
  }

  public Optional<byte[]> getDigest() {
    return digest;
  }
}
