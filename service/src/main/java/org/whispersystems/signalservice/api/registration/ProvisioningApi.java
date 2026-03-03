package org.whispersystems.signalservice.api.registration;

import org.signal.core.models.AccountEntropyPool;
import org.signal.core.models.MasterKey;
import org.signal.core.models.ServiceId;
import org.signal.core.models.backup.MediaRootBackupKey;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.ecc.ECPrivateKey;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.util.ByteUtil;
import org.signal.libsignal.zkgroup.InvalidInputException;
import org.signal.libsignal.zkgroup.profiles.ProfileKey;
import org.whispersystems.signalservice.api.account.AccountAttributes;
import org.whispersystems.signalservice.api.account.PreKeyCollection;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.internal.push.ProvisionMessage;
import org.whispersystems.signalservice.internal.push.ProvisioningSocket;
import org.whispersystems.signalservice.internal.push.PushServiceSocket;
import org.whispersystems.signalservice.internal.util.DynamicCredentialsProvider;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ProvisioningApi {
  private final PushServiceSocket  pushServiceSocket;
  private final ProvisioningSocket provisioningSocket;
  private final CredentialsProvider credentials;

  public ProvisioningApi(PushServiceSocket pushServiceSocket, ProvisioningSocket provisioningSocket, CredentialsProvider credentials) {
    this.pushServiceSocket  = pushServiceSocket;
    this.provisioningSocket = provisioningSocket;
    this.credentials        = credentials;
  }

  /**
   * Request a UUID from the server for linking as a new device.
   * Called by the new device.
   * @return The UUID, Base64 encoded
   */
  public String getNewDeviceUuid() throws TimeoutException, IOException {
    return provisioningSocket.getProvisioningUuid().address;
  }

  /**
   * Gets info from the primary device to finish the registration as a new device.<br>
   * @param tempIdentity A temporary identity. Must be the same as the one given to the already verified device.
   * @return Contains the account's permanent IdentityKeyPair, and it's number along with the provisioning code required to finish the registration.
   */
  public NewDeviceRegistrationReturn getNewDeviceRegistration(IdentityKeyPair tempIdentity) throws TimeoutException, IOException {
    ProvisionMessage msg = provisioningSocket.getProvisioningMessage(tempIdentity);

    final String        number = msg.number;
    final ServiceId.ACI aci    = ServiceId.ACI.parseOrThrow(msg.aci, msg.aciBinary);
    final ServiceId.PNI pni    = ServiceId.PNI.parseOrThrow(msg.pni, msg.pniBinary);

    if (credentials instanceof DynamicCredentialsProvider) {
      ((DynamicCredentialsProvider) credentials).setE164(number);
    }
    // Not setting Uuid here, as that causes a 400 Bad Request
    // when calling the finishNewDeviceRegistration endpoint
    // credentialsProvider.setUuid(uuid);

    final IdentityKeyPair aciIdentity = getIdentityKeyPair(msg.aciIdentityKeyPublic.toByteArray(), msg.aciIdentityKeyPrivate.toByteArray());
    final IdentityKeyPair pniIdentity = msg.pniIdentityKeyPublic != null && msg.pniIdentityKeyPrivate != null
                                        ? getIdentityKeyPair(msg.pniIdentityKeyPublic.toByteArray(), msg.pniIdentityKeyPrivate.toByteArray())
                                        : null;

    final ProfileKey profileKey;
    try {
      profileKey = msg.profileKey != null ? new ProfileKey(msg.profileKey.toByteArray()) : null;
    } catch (InvalidInputException e) {
      throw new IOException("Failed to decrypt profile key", e);
    }

    final MasterKey masterKey;
    try {
      masterKey = msg.masterKey != null ? new MasterKey(msg.masterKey.toByteArray()) : null;
    } catch (AssertionError e) {
      throw new IOException("Failed to decrypt master key", e);
    }

    final AccountEntropyPool accountEntropyPool = msg.accountEntropyPool != null ? new AccountEntropyPool(msg.accountEntropyPool) : null;
    final MediaRootBackupKey mediaRootBackupKey = msg.mediaRootBackupKey != null && msg.mediaRootBackupKey.size() == 32 ? new MediaRootBackupKey(msg.mediaRootBackupKey.toByteArray()) : null;

    final String  provisioningCode = msg.provisioningCode;
    final boolean readReceipts     = msg.readReceipts != null && msg.readReceipts;

    return new NewDeviceRegistrationReturn(
        provisioningCode,
        aciIdentity, pniIdentity,
        number,
        aci, pni,
        profileKey,
        masterKey,
        accountEntropyPool,
        mediaRootBackupKey,
        readReceipts
    );
  }

  private IdentityKeyPair getIdentityKeyPair(byte[] publicKeyBytes, byte[] privateKeyBytes) throws IOException {
    if (publicKeyBytes.length == 32) {
      // The public key is missing the type specifier, probably from iOS
      // Signal-Desktop handles this by ignoring the sent public key and regenerating it from the private key
      final int DJB_TYPE = 0x05;
      byte[] type = {DJB_TYPE};
      publicKeyBytes = ByteUtil.combine(type, publicKeyBytes);
    }
    final ECPublicKey  publicKey;
    final ECPrivateKey privateKey;
    try {
      publicKey = new ECPublicKey(publicKeyBytes);
      privateKey = new ECPrivateKey(privateKeyBytes);
    } catch (InvalidKeyException e) {
      throw new IOException("Failed to decrypt key", e);
    }
    return new IdentityKeyPair(new IdentityKey(publicKey), privateKey);
  }

  /**
   * Finishes a registration as a new device. Called by the new device.<br>
   * This method blocks until the already verified device has verified this device.
   * @param provisioningCode The provisioning code from the getNewDeviceRegistration method
   * @return The deviceId given by the server.
   */
  public int finishNewDeviceRegistration(String provisioningCode,
                                         AccountAttributes accountAttributes,
                                         PreKeyCollection aciPreKeys, PreKeyCollection pniPreKeys) throws IOException {
    int deviceId = this.pushServiceSocket.finishNewDeviceRegistration(provisioningCode, accountAttributes, aciPreKeys, pniPreKeys);
    if (credentials instanceof DynamicCredentialsProvider) {
      ((DynamicCredentialsProvider) credentials).setDeviceId(deviceId);
    }
    return deviceId;
  }

  /**
   * Helper class for holding the returns of getNewDeviceRegistration()
   */
  public static class NewDeviceRegistrationReturn {
    private final String             provisioningCode;
    private final IdentityKeyPair    aciIdentity;
    private final IdentityKeyPair    pniIdentity;
    private final String        number;
    private final ServiceId.ACI aci;
    private final ServiceId.PNI pni;
    private final ProfileKey    profileKey;
    private final MasterKey          masterKey;
    private final AccountEntropyPool accountEntropyPool;
    private final MediaRootBackupKey mediaRootBackupKey;
    private final boolean            readReceipts;

    NewDeviceRegistrationReturn(String provisioningCode, IdentityKeyPair aciIdentity, IdentityKeyPair pniIdentity, String number, ServiceId.ACI aci, ServiceId.PNI pni, ProfileKey profileKey, MasterKey masterKey, AccountEntropyPool accountEntropyPool, MediaRootBackupKey mediaRootBackupKey, boolean readReceipts) {
      this.provisioningCode   = provisioningCode;
      this.aciIdentity        = aciIdentity;
      this.pniIdentity        = pniIdentity;
      this.number             = number;
      this.aci                = aci;
      this.pni                = pni;
      this.profileKey         = profileKey;
      this.masterKey          = masterKey;
      this.accountEntropyPool = accountEntropyPool;
      this.mediaRootBackupKey = mediaRootBackupKey;
      this.readReceipts       = readReceipts;
    }

    /**
     * @return The provisioning code to finish the new device registration
     */
    public String getProvisioningCode() {
      return provisioningCode;
    }

    /**
     * @return The account's permanent IdentityKeyPair
     */
    public IdentityKeyPair getAciIdentity() {
      return aciIdentity;
    }

    public IdentityKeyPair getPniIdentity() {
      return pniIdentity;
    }

    /**
     * @return The account's number
     */
    public String getNumber() {
      return number;
    }

    /**
     * @return The account's uuid
     */
    public ServiceId.ACI getAci() {
      return aci;
    }

    public ServiceId.PNI getPni() {
      return pni;
    }

    /**
     * @return The account's profile key or null
     */
    public ProfileKey getProfileKey() {
      return profileKey;
    }

    /**
     * @return The account's master key or null
     */
    public MasterKey getMasterKey() {
      return masterKey;
    }

    public AccountEntropyPool getAccountEntropyPool() {
      return accountEntropyPool;
    }

    public MediaRootBackupKey getMediaRootBackupKey() {
      return mediaRootBackupKey;
    }

    /**
     * @return The account's read receipts setting
     */
    public boolean isReadReceipts() {
      return readReceipts;
    }
  }
}
