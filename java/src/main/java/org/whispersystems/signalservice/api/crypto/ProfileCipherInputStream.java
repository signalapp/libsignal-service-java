package org.whispersystems.signalservice.api.crypto;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.GCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.pqc.math.ntru.util.Util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileCipherInputStream extends FilterInputStream {

  private final GCMBlockCipher cipher;

  private boolean finished = false;

  public ProfileCipherInputStream(InputStream in, byte[] key) throws IOException {
    super(in);
    this.cipher = new GCMBlockCipher(new AESFastEngine());

    byte[] nonce = Util.readFullLength(in, 12);
    this.cipher.init(false, new AEADParameters(new KeyParameter(key), 128, nonce));
  }

  @Override
  public int read() {
    throw new AssertionError("Not supported!");
  }

  @Override
  public int read(byte[] input) throws IOException {
    return read(input, 0, input.length);
  }

  @Override
  public int read(byte[] output, int outputOffset, int outputLength) throws IOException {
    if (finished) return -1;

    try {
      byte[] ciphertext = new byte[outputLength / 2];
      int    read       = in.read(ciphertext, 0, ciphertext.length);

      if (read == -1) {
        if (cipher.getOutputSize(0) > outputLength) {
          throw new AssertionError("Need: " + cipher.getOutputSize(0) + " but only have: " + outputLength);
        }

        finished = true;
        return cipher.doFinal(output, outputOffset);
      } else {
        if (cipher.getUpdateOutputSize(read) > outputLength) {
          throw new AssertionError("Need: " + cipher.getOutputSize(read) + " but only have: " + outputLength);
        }

        return cipher.processBytes(ciphertext, 0, read, output, outputOffset);
      }
    } catch (InvalidCipherTextException e) {
      throw new IOException(e);
    }
  }

}
