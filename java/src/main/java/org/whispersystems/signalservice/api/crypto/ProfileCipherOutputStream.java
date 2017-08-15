package org.whispersystems.signalservice.api.crypto;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.GCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

public class ProfileCipherOutputStream extends DigestingOutputStream {

  private final GCMBlockCipher cipher;

  public ProfileCipherOutputStream(OutputStream out, byte[] key) throws IOException {
    super(out);
    this.cipher = new GCMBlockCipher(new AESFastEngine());

    byte[] nonce  = generateNonce();
    this.cipher.init(true, new AEADParameters(new KeyParameter(key), 128, nonce));

    super.write(nonce, 0, nonce.length);
  }

  @Override
  public void write(byte[] buffer) throws IOException {
    write(buffer, 0, buffer.length);
  }

  @Override
  public void write(byte[] buffer, int offset, int length) throws IOException {
    byte[] output = new byte[cipher.getUpdateOutputSize(length)];
    int encrypted = cipher.processBytes(buffer, offset, length, output, 0);

    super.write(output, 0, encrypted);
  }

  @Override
  public void write(int b) throws IOException {
    byte[] output = new byte[cipher.getUpdateOutputSize(1)];
    int encrypted = cipher.processByte((byte)b, output, 0);

    super.write(output, 0, encrypted);
  }

  @Override
  public void flush() throws IOException {
    try {
      byte[] output = new byte[cipher.getOutputSize(0)];
      int encrypted = cipher.doFinal(output, 0);

      super.write(output, 0, encrypted);
      super.flush();
    } catch (InvalidCipherTextException e) {
      throw new AssertionError(e);
    }
  }

  private byte[] generateNonce() {
    byte[] nonce = new byte[12];
    new SecureRandom().nextBytes(nonce);
    return nonce;
  }

  public static long getCiphertextLength(long plaintextLength) {
    return 12 + 16 + plaintextLength;
  }
}
