/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api;


import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.multidevice.DeviceInfo;
import org.whispersystems.signalservice.api.push.ContactTokenDetails;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.SignedPreKeyEntity;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.internal.crypto.ProvisioningCipher;
import org.whispersystems.signalservice.internal.push.ProvisioningProtos.ProvisionMessage;
import org.whispersystems.signalservice.internal.push.ProvisioningSocket;
import org.whispersystems.signalservice.internal.push.PushServiceSocket;
import org.whispersystems.signalservice.internal.util.Base64;
import org.whispersystems.signalservice.internal.util.DynamicCredentialsProvider;
import org.whispersystems.signalservice.internal.util.Util;

import com.google.protobuf.ByteString;

/**
 * The main interface for creating, registering, and
 * managing a Signal Service account.
 *
 * @author Moxie Marlinspike
 */
public class SignalServiceAccountManager {

  private final DynamicCredentialsProvider credentialsProvider;
  private final PushServiceSocket          pushServiceSocket;
  private final ProvisioningSocket         provisioningSocket;

  /**
   * Construct a SignalServiceAccountManager.
   *
   * @param url The URL for the Signal Service.
   * @param trustStore The {@link org.whispersystems.signalservice.api.push.TrustStore} for the SignalService server's TLS certificate.
   * @param user A Signal Service phone number.
   * @param password A Signal Service password.
   * @param deviceId A integer which is provided by the server while linking.
   * @param userAgent A string which identifies the client software.
   */
  public SignalServiceAccountManager(String url, TrustStore trustStore,
                                     String user, String password, int deviceId,
                                     String userAgent)
  {
    this.credentialsProvider = new DynamicCredentialsProvider(user, password, null, deviceId);
    this.provisioningSocket  = new ProvisioningSocket(url, trustStore, userAgent);
    this.pushServiceSocket   = new PushServiceSocket(url, trustStore, credentialsProvider, userAgent);
  }
  
  /**
   * Construct a SignalServiceAccountManager.
   *
   * @param url The URL for the Signal Service.
   * @param trustStore The {@link org.whispersystems.signalservice.api.push.TrustStore} for the SignalService server's TLS certificate.
   * @param user A Signal Service phone number.
   * @param password A Signal Service password.
   * @param userAgent A string which identifies the client software.
   */
  public SignalServiceAccountManager(String url, TrustStore trustStore,
                                     String user, String password,
                                     String userAgent)
  {
    this(url, trustStore, user, password, SignalServiceAddress.DEFAULT_DEVICE_ID, userAgent);
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
   *                              This value should remain consistent across registrations for the
   *                              same install, but probabilistically differ across registrations
   *                              for separate installs.
   * @param voice A boolean that indicates whether the client supports secure voice (RedPhone) calls.
   * @param fetchesMessages A boolean that indicates whether the client fetches messages instead of relying on GCM
   *
   * @throws IOException
   */
  public void verifyAccountWithCode(String verificationCode, String signalingKey, int signalProtocolRegistrationId, 
      boolean voice, boolean fetchesMessages)
      throws IOException
  {
    this.pushServiceSocket.verifyAccountCode(verificationCode, signalingKey,
                                             signalProtocolRegistrationId, voice, fetchesMessages);
  }

  /**
   * Verify a Signal Service account with a signed token from a trusted source.
   *
   * @param verificationToken The signed token provided by a trusted server.

   * @param signalingKey 52 random bytes.  A 32 byte AES key and a 20 byte Hmac256 key,
   *                     concatenated.
   * @param axolotlRegistrationId A random 14-bit number that identifies this Signal install.
   *                              This value should remain consistent across registrations for the
   *                              same install, but probabilistically differ across registrations
   *                              for separate installs.
   * @param voice A boolean that indicates whether the client supports secure voice (RedPhone) calls.
   *
   * @throws IOException
   */
  public void verifyAccountWithToken(String verificationToken, String signalingKey, int axolotlRegistrationId, boolean voice)
      throws IOException
  {
    this.pushServiceSocket.verifyAccountToken(verificationToken, signalingKey, axolotlRegistrationId, voice);
  }

  /**
   * Refresh account attributes with server.
   *
   * @param signalingKey 52 random bytes.  A 32 byte AES key and a 20 byte Hmac256 key, concatenated.
   * @param signalProtocolRegistrationId A random 14-bit number that identifies this Signal install.
   *                              This value should remain consistent across registrations for the same
   *                              install, but probabilistically differ across registrations for
   *                              separate installs.
   * @param voice A boolean that indicates whether the client supports secure voice (RedPhone)
   *
   * @throws IOException
   */
  public void setAccountAttributes(String signalingKey, int signalProtocolRegistrationId, boolean voice)
      throws IOException
  {
    this.pushServiceSocket.setAccountAttributes(signalingKey, signalProtocolRegistrationId, voice);
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
  
  /**
   * Request a UUID from the server for linking as a new device.
   * Called by the new device.
   * @return The UUID, Base64 encoded
   * @throws TimeoutException
   * @throws IOException
   */
  public String getNewDeviceUuid() throws TimeoutException, IOException {
    return provisioningSocket.getProvisioningUuid().getUuid();
  }

  /**
   * Request a Code for verification of a new device.
   * Called by an already verified device.
   * @return An verification code. String of 6 digits
   * @throws IOException
   */
  public String getNewDeviceVerificationCode() throws IOException {
    return this.pushServiceSocket.getNewDeviceVerificationCode();
  }
  
  /**
   * Finishes a registration as a new device. Called by the new device.<br>
   * This method blocks until the already verified device has verified this device.
   * @param tempIdentity A temporary identity. Must be the same as the one given to the already verified device.
   * @param signalingKey 52 random bytes.  A 32 byte AES key and a 20 byte Hmac256 key, concatenated.
   * @param supportsSms A boolean which indicates whether this device can receive SMS to the account's number.
   * @param fetchesMessages A boolean which indicates whether this device fetches messages.
   * @param registrationId A random integer generated at install time.
   * @param deviceName A name for this device, not its user agent.
   * @return Contains the account's permanent IdentityKeyPair and it's number along the deviceId given by the server.
   * @throws TimeoutException
   * @throws IOException
   * @throws InvalidKeyException
   */
  public NewDeviceRegistrationReturn finishNewDeviceRegistration(IdentityKeyPair tempIdentity, String signalingKey, boolean supportsSms, boolean fetchesMessages, int registrationId, String deviceName) throws TimeoutException, IOException, InvalidKeyException {
    ProvisionMessage msg = provisioningSocket.getProvisioningMessage(tempIdentity);
    credentialsProvider.setUser(msg.getNumber());
    String provisioningCode = msg.getProvisioningCode();
    ECPublicKey publicKey = Curve.decodePoint(msg.getIdentityKeyPublic().toByteArray(), 0);
    ECPrivateKey privateKey = Curve.decodePrivatePoint(msg.getIdentityKeyPrivate().toByteArray());
    IdentityKeyPair identity = new IdentityKeyPair(new IdentityKey(publicKey), privateKey);
    int deviceId = this.pushServiceSocket.finishNewDeviceRegistration(provisioningCode, signalingKey, supportsSms, fetchesMessages, registrationId, deviceName);
    credentialsProvider.setDeviceId(deviceId);
    return new NewDeviceRegistrationReturn(identity, deviceId, msg.getNumber());
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
                                                 .setNumber(credentialsProvider.getUser())
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
  
  /**
   * Helper class for holding the returns of finishNewDeviceRegistration()
   */
  public class NewDeviceRegistrationReturn {
    private final IdentityKeyPair identity;
    private final int deviceId;
    private final String number;
    
    NewDeviceRegistrationReturn(IdentityKeyPair identity, int deviceId, String number) {
      this.identity = identity;
      this.deviceId = deviceId;
      this.number = number;
    }

    /**
     * @return The account's permanent IdentityKeyPair
     */
    public IdentityKeyPair getIdentity() {
      return identity;
    }

    /**
     * @return The deviceId for this device given by the server
     */
    public int getDeviceId() {
      return deviceId;
    }

    /**
     * @return The account's number
     */
    public String getNumber() {
      return number;
    }
  }

}
