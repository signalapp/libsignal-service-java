package org.whispersystems.signalservice.api.crypto;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileCipher {

  public static final int NAME_PADDED_LENGTH = 26;

  private final byte[] key;

  public ProfileCipher(byte[] key) {
    this.key = key;
  }

  public byte[] encrypt(byte[] input, int paddedLength) {
    try {
      byte[] inputPadded = new byte[paddedLength];

      if (input.length > inputPadded.length) {
        throw new IllegalArgumentException("Input is too long: " + new String(input));
      }

      System.arraycopy(input, 0, inputPadded, 0, input.length);

      ByteArrayOutputStream     baos = new ByteArrayOutputStream();
      ProfileCipherOutputStream out  = new ProfileCipherOutputStream(baos, key);
      out.write(inputPadded);
      out.flush();
      out.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public byte[] decrypt(byte[] input) throws InvalidCiphertextException {
    try {
      ByteArrayInputStream     bais = new ByteArrayInputStream(input);
      ProfileCipherInputStream in   = new ProfileCipherInputStream(bais, key);

      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[]                buffer = new byte[4096];
      int                   read   = 0;

      while ((read = in.read(buffer)) != -1) {
        result.write(buffer, 0, read);
      }

      return result.toByteArray();
    } catch (IOException e) {
      throw new InvalidCiphertextException(e);
    }
  }

  public static class InvalidCiphertextException extends Exception {
    public InvalidCiphertextException(Exception nested) {
      super(nested);
    }
  }

}
