/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api;


import com.google.protobuf.ByteString;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.calls.TurnServerInfo;
import org.whispersystems.signalservice.api.messages.multidevice.DeviceInfo;
import org.whispersystems.signalservice.api.push.ContactTokenDetails;
import org.whispersystems.signalservice.api.push.SignedPreKeyEntity;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.internal.crypto.ProvisioningCipher;
import org.whispersystems.signalservice.internal.push.PushServiceSocket;
import org.whispersystems.signalservice.internal.push.SignalServiceUrl;
import org.whispersystems.signalservice.internal.util.Base64;
import org.whispersystems.signalservice.internal.util.StaticCredentialsProvider;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.whispersystems.signalservice.internal.push.ProvisioningProtos.ProvisionMessage;

/**
 * The main interface for creating, registering, and
 * managing a Signal Service account.
 *
 * @author Moxie Marlinspike
 */
public class SignalServiceAccountManager {

  private final PushServiceSocket pushServiceSocket;
  private final String            user;
  private final String            userAgent;

  /**
   * Construct a SignalServiceAccountManager.
   *
   * @param urls The URL for the Signal Service.
   * @param user A Signal Service phone number.
   * @param password A Signal Service password.
   * @param userAgent A string which identifies the client software.
   */
  public SignalServiceAccountManager(SignalServiceUrl[] urls,
                                     String user, String password,
                                     String userAgent)
  {
    this.pushServiceSocket = new PushServiceSocket(urls, new StaticCredentialsProvider(user, password, null), userAgent);
    this.user              = user;
    this.userAgent         = userAgent;
  }

  /**
   * Register/Unregister a Google Cloud Messaging registration ID.
   *
   * @param gcmRegistrationId The GCM id to register.  A call with an absent value will unregister.
   * @throws IOException
   */
  public void setGcmId(Optional<String> gcmRegistrationId) throws IOException {
    if (gcmRegistrationId.isPresent()) {
      this.pushServiceSocket.registerGcmId(gcmRegistrationId.get());
    } else {
      this.pushServiceSocket.unregisterGcmId();
    }
  }

  /**
   * Request an SMS verification code.  On success, the server will send
   * an SMS verification code to this Signal user.
   *
   * @throws IOException
   */
  public void requestSmsVerificationCode() throws IOException {
    this.pushServiceSocket.createAccount(false);
  }

  /**
   * Request a Voice verification code.  On success, the server will
   * make a voice call to this Signal user.
   *
    * @throws IOException
   */
  public void requestVoiceVerificationCode() throws IOException {
    this.pushServiceSocket.createAccount(true);
  }

  /**
   * Verify a Signal Service account with a received SMS or voice verification code.
   *
   * @param verificationCode The verification code received via SMS or Voice
   *                         (see {@link #requestSmsVerificationCode} and
   *                         {@link #requestVoiceVerificationCode}).
   * @param signalingKey 52 random bytes.  A 32 byte AES key and a 20 byte Hmac256 key,
   *                     concatenated.
   * @param signalProtocolRegistrationId A random 14-bit number that identifies this Signal install.
   *                                     This value should remain consistent across registrations for the
   *                                     same install, but probabilistically differ across registrations
   *                                     for separate installs.
   * @param voice A boolean that indicates whether the client supports secure voice (RedPhone) calls.
   *
   * @throws IOException
   */
  public void verifyAccountWithCode(String verificationCode, String signalingKey, int signalProtocolRegistrationId, boolean voice, boolean video, boolean fetchesMessages)
      throws IOException
  {
    this.pushServiceSocket.verifyAccountCode(verificationCode, signalingKey,
                                             signalProtocolRegistrationId,
                                             voice, video, fetchesMessages);
  }

  /**
   * Verify a Signal Service account with a signed token from a trusted source.
   *
   * @param verificationToken The signed token provided by a trusted server.

   * @param signalingKey 52 random bytes.  A 32 byte AES key and a 20 byte Hmac256 key,
   *                     concatenated.
   * @param signalProtocolRegistrationId A random 14-bit number that identifies this Signal install.
   *                                     This value should remain consistent across registrations for the
   *                                     same install, but probabilistically differ across registrations
   *                                     for separate installs.
   * @param voice A boolean that indicates whether the client supports secure voice (RedPhone) calls.
   *
   * @throws IOException
   */
  public void verifyAccountWithToken(String verificationToken, String signalingKey, int signalProtocolRegistrationId, boolean voice, boolean video, boolean fetchesMessages)
      throws IOException
  {
    this.pushServiceSocket.verifyAccountToken(verificationToken, signalingKey, signalProtocolRegistrationId, voice, video, fetchesMessages);
  }

  /**
   * Refresh account attributes with server.
   *
   * @param signalingKey 52 random bytes.  A 32 byte AES key and a 20 byte Hmac256 key, concatenated.
   * @param signalProtocolRegistrationId A random 14-bit number that identifies this Signal install.
   *                                     This value should remain consistent across registrations for the same
   *                                     install, but probabilistically differ across registrations for
   *                                     separate installs.
   * @param voice A boolean that indicates whether the client supports secure voice (RedPhone)
   *
   * @throws IOException
   */
  public void setAccountAttributes(String signalingKey, int signalProtocolRegistrationId, boolean voice, boolean video, boolean fetchesMessages)
      throws IOException
  {
    this.pushServiceSocket.setAccountAttributes(signalingKey, signalProtocolRegistrationId, voice, video, fetchesMessages);
  }

  /**
   * Register an identity key, last resort key, signed prekey, and list of one time prekeys
   * with the server.
   *
   * @param identityKey The client's long-term identity keypair.
   * @param lastResortKey The client's "last resort" prekey.
   * @param signedPreKey The client's signed prekey.
   * @param oneTimePreKeys The client's list of one-time prekeys.
   *
   * @throws IOException
   */
  public void setPreKeys(IdentityKey identityKey, PreKeyRecord lastResortKey,
                         SignedPreKeyRecord signedPreKey, List<PreKeyRecord> oneTimePreKeys)
      throws IOException
  {
    this.pushServiceSocket.registerPreKeys(identityKey, lastResortKey, signedPreKey, oneTimePreKeys);
  }

  /**
   * @return The server's count of currently available (eg. unused) prekeys for this user.
   * @throws IOException
   */
  public int getPreKeysCount() throws IOException {
    return this.pushServiceSocket.getAvailablePreKeys();
  }

  /**
   * Set the client's signed prekey.
   *
   * @param signedPreKey The client's new signed prekey.
   * @throws IOException
   */
  public void setSignedPreKey(SignedPreKeyRecord signedPreKey) throws IOException {
    this.pushServiceSocket.setCurrentSignedPreKey(signedPreKey);
  }

  /**
   * @return The server's view of the client's current signed prekey.
   * @throws IOException
   */
  public SignedPreKeyEntity getSignedPreKey() throws IOException {
    return this.pushServiceSocket.getCurrentSignedPreKey();
  }

  /**
   * Checks whether a contact is currently registered with the server.
   *
   * @param e164number The contact to check.
   * @return An optional ContactTokenDetails, present if registered, absent if not.
   * @throws IOException
   */
  public Optional<ContactTokenDetails> getContact(String e164number) throws IOException {
    String              contactToken        = createDirectoryServerToken(e164number, true);
    ContactTokenDetails contactTokenDetails = this.pushServiceSocket.getContactTokenDetails(contactToken);

    if (contactTokenDetails != null) {
      contactTokenDetails.setNumber(e164number);
    }

    return Optional.fromNullable(contactTokenDetails);
  }

  /**
   * Checks which contacts in a set are registered with the server.
   *
   * @param e164numbers The contacts to check.
   * @return A list of ContactTokenDetails for the registered users.
   * @throws IOException
   */
  public List<ContactTokenDetails> getContacts(Set<String> e164numbers)
      throws IOException
  {
    Map<String, String>       contactTokensMap = createDirectoryServerTokenMap(e164numbers);
    List<ContactTokenDetails> activeTokens     = this.pushServiceSocket.retrieveDirectory(contactTokensMap.keySet());

    for (ContactTokenDetails activeToken : activeTokens) {
      activeToken.setNumber(contactTokensMap.get(activeToken.getToken()));
    }

    return activeTokens;
  }

  public String getAccountVerificationToken() throws IOException {
    return this.pushServiceSocket.getAccountVerificationToken();
  }

  public String getNewDeviceVerificationCode() throws IOException {
    return this.pushServiceSocket.getNewDeviceVerificationCode();
  }

  public void addDevice(String deviceIdentifier,
                        ECPublicKey deviceKey,
                        IdentityKeyPair identityKeyPair,
                        String code)
      throws InvalidKeyException, IOException
  {
    ProvisioningCipher cipher  = new ProvisioningCipher(deviceKey);
    ProvisionMessage   message = ProvisionMessage.newBuilder()
                                                 .setIdentityKeyPublic(ByteString.copyFrom(identityKeyPair.getPublicKey().serialize()))
                                                 .setIdentityKeyPrivate(ByteString.copyFrom(identityKeyPair.getPrivateKey().serialize()))
                                                 .setNumber(user)
                                                 .setProvisioningCode(code)
                                                 .build();

    byte[] ciphertext = cipher.encrypt(message);
    this.pushServiceSocket.sendProvisioningMessage(deviceIdentifier, ciphertext);
  }

  public List<DeviceInfo> getDevices() throws IOException {
    return this.pushServiceSocket.getDevices();
  }

  public void removeDevice(long deviceId) throws IOException {
    this.pushServiceSocket.removeDevice(deviceId);
  }

  public TurnServerInfo getTurnServerInfo() throws IOException {
    return this.pushServiceSocket.getTurnServerInfo();
  }

  public void setSoTimeoutMillis(long soTimeoutMillis) {
    this.pushServiceSocket.setSoTimeoutMillis(soTimeoutMillis);
  }

  public void cancelInFlightRequests() {
    this.pushServiceSocket.cancelInFlightRequests();
  }

  private String createDirectoryServerToken(String e164number, boolean urlSafe) {
    try {
      MessageDigest digest  = MessageDigest.getInstance("SHA1");
      byte[]        token   = Util.trim(digest.digest(e164number.getBytes()), 10);
      String        encoded = Base64.encodeBytesWithoutPadding(token);

      if (urlSafe) return encoded.replace('+', '-').replace('/', '_');
      else         return encoded;
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  private Map<String, String> createDirectoryServerTokenMap(Collection<String> e164numbers) {
    Map<String,String> tokenMap = new HashMap<>(e164numbers.size());

    for (String number : e164numbers) {
      tokenMap.put(createDirectoryServerToken(number, false), number);
    }

    return tokenMap;
  }

}
