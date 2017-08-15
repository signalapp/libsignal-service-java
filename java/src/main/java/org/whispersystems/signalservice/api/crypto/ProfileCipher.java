package org.whispersystems.signalservice.api.crypto;


import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.GCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.KeyParameter;
import org.whispersystems.libsignal.util.ByteUtil;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileCipher {

  public static final int NAME_PADDED_LENGTH = 26;

  private final byte[] key;

  public ProfileCipher(byte[] key) {
    this.key = key;
  }

  public byte[] encryptName(byte[] input, int paddedLength) {
    try {
      byte[] inputPadded = new byte[paddedLength];

      if (input.length > inputPadded.length) {
        throw new IllegalArgumentException("Input is too long: " + new String(input));
      }

      System.arraycopy(input, 0, inputPadded, 0, input.length);

      byte[] nonce = Util.getSecretBytes(12);

      GCMBlockCipher cipher = new GCMBlockCipher(new AESFastEngine());
      cipher.init(true, new AEADParameters(new KeyParameter(key), 128, nonce));

      byte[] ciphertext = new byte[cipher.getUpdateOutputSize(inputPadded.length)];
      cipher.processBytes(inputPadded, 0, inputPadded.length, ciphertext, 0);

      byte[] tag = new byte[cipher.getOutputSize(0)];
      cipher.doFinal(tag, 0);

      return ByteUtil.combine(nonce, ciphertext, tag);
    } catch (InvalidCipherTextException e) {
      throw new AssertionError(e);
    }
  }

  public byte[] decryptName(byte[] input) throws InvalidCiphertextException {
    try {
      if (input.length < 12 + 16 + 1) {
        throw new InvalidCiphertextException("Too short: " + input.length);
      }

      byte[] nonce = new byte[12];
      System.arraycopy(input, 0, nonce, 0, nonce.length);

      GCMBlockCipher cipher = new GCMBlockCipher(new AESFastEngine());
      cipher.init(false, new AEADParameters(new KeyParameter(key), 128, nonce));

      byte[] paddedPlaintextOne = new byte[cipher.getUpdateOutputSize(input.length - 12)];
      cipher.processBytes(input, 12, input.length - 12, paddedPlaintextOne, 0);

      byte[] paddedPlaintextTwo = new byte[cipher.getOutputSize(0)];
      cipher.doFinal(paddedPlaintextTwo, 0);

      byte[] paddedPlaintext = ByteUtil.combine(paddedPlaintextOne, paddedPlaintextTwo);
      int    plaintextLength = 0;

      for (int i=paddedPlaintext.length-1;i>=0;i--) {
        if (paddedPlaintext[i] != (byte)0x00) {
          plaintextLength = i + 1;
          break;
        }
      }

      byte[] plaintext = new byte[plaintextLength];
      System.arraycopy(paddedPlaintext, 0, plaintext, 0, plaintextLength);

      return plaintext;
    } catch (InvalidCipherTextException e) {
      throw new InvalidCiphertextException(e);
    }
  }

  public static class InvalidCiphertextException extends Exception {
    public InvalidCiphertextException(Exception nested) {
      super(nested);
    }

    public InvalidCiphertextException(String s) {
      super(s);
    }
  }

}
