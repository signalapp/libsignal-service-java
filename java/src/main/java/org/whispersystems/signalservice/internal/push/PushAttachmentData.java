/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.internal.push;

import org.whispersystems.signalservice.api.messages.SignalServiceAttachment.ProgressListener;

import java.io.InputStream;

public class PushAttachmentData {

  private final String           contentType;
  private final InputStream      data;
  private final long             dataSize;
  private final byte[]           key;
  private final ProgressListener listener;

  public PushAttachmentData(String contentType, InputStream data, long dataSize,
                            ProgressListener listener, byte[] key)
  {
    this.contentType = contentType;
    this.data        = data;
    this.dataSize    = dataSize;
    this.key         = key;
    this.listener    = listener;
  }

  public String getContentType() {
    return contentType;
  }

  public InputStream getData() {
    return data;
  }

  public long getDataSize() {
    return dataSize;
  }

  public byte[] getKey() {
    return key;
  }

  public ProgressListener getListener() {
    return listener;
  }
}
