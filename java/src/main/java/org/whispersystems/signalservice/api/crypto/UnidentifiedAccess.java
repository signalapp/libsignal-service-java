package org.whispersystems.signalservice.api.crypto;


import org.signal.libsignal.metadata.certificate.InvalidCertificateException;
import org.signal.libsignal.metadata.certificate.SenderCertificate;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.GCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.KeyParameter;
import org.whispersystems.libsignal.util.ByteUtil;

public class UnidentifiedAccess {

  private final byte[]            unidentifiedAccessKey;
  private final SenderCertificate unidentifiedCertificate;

  public UnidentifiedAccess(byte[] unidentifiedAccessKey, byte[] unidentifiedCertificate)
      throws InvalidCertificateException
  {
    this.unidentifiedAccessKey   = unidentifiedAccessKey;
    this.unidentifiedCertificate = new SenderCertificate(unidentifiedCertificate);
  }

  public byte[] getUnidentifiedAccessKey() {
    return unidentifiedAccessKey;
  }

  public SenderCertificate getUnidentifiedCertificate() {
    return unidentifiedCertificate;
  }

  public static byte[] deriveAccessKeyFrom(byte[] profileKey) {
    try {
      byte[]         nonce  = new byte[12];
      byte[]         input  = new byte[16];
      GCMBlockCipher cipher = new GCMBlockCipher(new AESFastEngine());
      cipher.init(true, new AEADParameters(new KeyParameter(profileKey), 128, nonce));

      byte[] ciphertext = new byte[cipher.getUpdateOutputSize(input.length)];
      cipher.processBytes(input, 0, input.length, ciphertext, 0);

      byte[] tag = new byte[cipher.getOutputSize(0)];
      cipher.doFinal(tag, 0);

      byte[] combined = ByteUtil.combine(ciphertext, tag);
      return ByteUtil.trim(combined, 16);
    } catch (InvalidCipherTextException e) {
      throw new AssertionError(e);
    }
  }
}
