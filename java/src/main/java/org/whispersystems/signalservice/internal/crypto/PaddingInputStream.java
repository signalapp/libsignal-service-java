package org.whispersystems.signalservice.internal.crypto;


import org.whispersystems.signalservice.internal.util.Util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PaddingInputStream extends FilterInputStream {

  private static final String TAG = PaddingInputStream.class.getSimpleName();

  private static final int MB = 1024 * 1024;
  private static final int KB = 1024;

  private long paddingRemaining;

  public PaddingInputStream(InputStream inputStream, long plaintextLength) {
    super(inputStream);
    this.paddingRemaining = getPaddedSize(plaintextLength) - plaintextLength;
  }

  @Override
  public int read() throws IOException {
    int result = super.read();
    if (result != -1) return result;

    if (paddingRemaining > 0) {
      paddingRemaining--;
      return 0x00;
    }

    return -1;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    int result = super.read(buffer, offset, length);
    if (result != -1) return result;

    if (paddingRemaining > 0) {
      length = Math.min(length, Util.toIntExact(paddingRemaining));
      paddingRemaining -= length;
      return length;
    }

    return -1;
  }

  @Override
  public int read(byte[] buffer) throws IOException {
    return read(buffer, 0, buffer.length);
  }

  @Override
  public int available() throws IOException {
    return super.available() + Util.toIntExact(paddingRemaining);
  }

  public static long getPaddedSize(long size) {
    return size;
//    if      (size >= 10 * MB)  return getRoundedUp(size, 10 * MB);
//    else if (size >= 5 * MB)   return getRoundedUp(size, 5 * MB);
//    else if (size >= MB)       return getRoundedUp(size, MB);
//    else if (size >= 500 * KB) return getRoundedUp(size, 500 * KB);
//    else if (size >= 50 * KB)  return getRoundedUp(size, 50 * KB);
//    else if (size >= KB)       return getRoundedUp(size, KB);
//    else                       return KB - size;
  }

  private static long getRoundedUp(long size, long interval) {
    long multiplier = (long)Math.ceil(((double)size) / ((double)interval));
    return interval * multiplier;
  }

}
