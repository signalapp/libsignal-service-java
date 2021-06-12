/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.internal.crypto;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.kdf.HKDFv3;
import org.whispersystems.signalservice.internal.util.Util;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.whispersystems.signalservice.internal.push.ProvisioningProtos.ProvisionEnvelope;
import static org.whispersystems.signalservice.internal.push.ProvisioningProtos.ProvisionMessage;


public class ProvisioningCipher {

  private static final String TAG = ProvisioningCipher.class.getSimpleName();

  private final ECPublicKey theirPublicKey;

  public ProvisioningCipher(ECPublicKey theirPublicKey) {
    this.theirPublicKey = theirPublicKey;
  }

  public byte[] encrypt(ProvisionMessage message) throws InvalidKeyException {
    ECKeyPair ourKeyPair    = Curve.generateKeyPair();
    byte[]    sharedSecret  = Curve.calculateAgreement(theirPublicKey, ourKeyPair.getPrivateKey());
    byte[]    derivedSecret = new HKDFv3().deriveSecrets(sharedSecret, "TextSecure Provisioning Message".getBytes(), 64);
    byte[][]  parts         = Util.split(derivedSecret, 32, 32);

    byte[] version    = {0x01};
    byte[] ciphertext = getCiphertext(parts[0], message.toByteArray());
    byte[] mac        = getMac(parts[1], Util.join(version, ciphertext));
    byte[] body       = Util.join(version, ciphertext, mac);

    return ProvisionEnvelope.newBuilder()
                            .setPublicKey(ByteString.copyFrom(ourKeyPair.getPublicKey().serialize()))
                            .setBody(ByteString.copyFrom(body))
                            .build()
                            .toByteArray();
  }

  private byte[] getCiphertext(byte[] key, byte[] message) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));

      return Util.join(cipher.getIV(), cipher.doFinal(message));
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | java.security.InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      throw new AssertionError(e);
    }
  }

  private byte[] getMac(byte[] key, byte[] message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));

      return mac.doFinal(message);
    } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }

  public ProvisionMessage decrypt(IdentityKeyPair tempIdentity, byte[] bytes) throws InvalidKeyException, InvalidProtocolBufferException {
    ProvisionEnvelope envelope      = ProvisionEnvelope.parseFrom(bytes);
    ECPublicKey       publicKey     = Curve.decodePoint(envelope.getPublicKey().toByteArray(), 0);
    byte[]            sharedSecret  = Curve.calculateAgreement(publicKey, tempIdentity.getPrivateKey());
    byte[]            derivedSecret = new HKDFv3().deriveSecrets(sharedSecret, "TextSecure Provisioning Message".getBytes(), 64);
    byte[][]          parts         = Util.split(derivedSecret, 32, 32);
    ByteString        joined        = envelope.getBody();
    if (joined.byteAt(0) != 0x01) {
      throw new RuntimeException("Bad version number on provision message!");
    }
    byte[] iv              = joined.substring(1, 16 + 1).toByteArray();
    byte[] ciphertext      = joined.substring(16 + 1, joined.size() - 32).toByteArray();
    byte[] ivAndCiphertext = joined.substring(0, joined.size() - 32).toByteArray();
    byte[] mac             = joined.substring(joined.size() - 32).toByteArray();

    verifyMac(parts[1], ivAndCiphertext, mac);

    return ProvisionMessage.parseFrom(decrypt(parts[0], iv, ciphertext));
  }

  private void verifyMac(byte[] key, byte[] message, byte[] theirMac) {
    byte[] ourMac = getMac(key, message);
    if (!Arrays.equals(ourMac, theirMac)) {
      throw new RuntimeException("Invalid MAC on provision message!");
    }
  }

  private byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
      return cipher.doFinal(ciphertext);
    } catch (java.security.InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
      throw new AssertionError(e);
    }
  }

}
