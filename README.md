# signal-service-java

A Java library for communicating via Signal.

## Implementing the Signal Protocol interfaces

The Signal encryption protocol is a stateful protocol, so libsignal-service users
need to implement the storage interface `SignalProtocolStore`, which handles load/store
of your key and session information to durable media.

## Creating keys

`````java
IdentityKeyPair    identityKey        = KeyHelper.generateIdentityKeyPair();
List<PreKeyRecord> oneTimePreKeys     = KeyHelper.generatePreKeys(0, 100);
PreKeyRecord       lastResortKey      = KeyHelper.generateLastResortPreKey();
SignedPreKeyRecord signedPreKeyRecord = KeyHelper.generateSignedPreKey(identityKey, signedPreKeyId);
`````

The above are then stored locally so that they're available for load via the `SignalProtocolStore`.

## Registering

At install time, clients need to register with the Signal server.

`````java
private final String     URL         = "https://my.signal.server.com";
private final TrustStore TRUST_STORE = new MyTrustStoreImpl();
private final String     USERNAME    = "+14151231234";
private final String     PASSWORD    = generateRandomPassword();
private final String     USER_AGENT  = "[FILL_IN]";

SignalServiceAccountManager accountManager = new SignalServiceAccountManager(URL, TRUST_STORE,
                                                                             USERNAME, PASSWORD, USER_AGENT);

accountManager.requestSmsVerificationCode();
accountManager.verifyAccountWithCode(receivedSmsVerificationCode, generateRandomSignalingKey(),
                                     generateRandomInstallId(), false);
accountManager.setGcmId(Optional.of(GoogleCloudMessaging.getInstance(this).register(REGISTRATION_ID)));
accountManager.setPreKeys(identityKey.getPublicKey(), lastResortKey, signedPreKeyRecord, oneTimePreKeys);
`````

## Sending text messages

`````java
SignalServiceMessageSender messageSender = new SignalServiceMessageSender(URL, TRUST_STORE, USERNAME, PASSWORD,
                                                                          new MySignalProtocolStore(),
                                                                          USER_AGENT, Optional.absent());

messageSender.sendMessage(new SignalServiceAddress("+14159998888"),
                          SignalServiceDataMessage.newBuilder()
                                                  .withBody("Hello, world!")
                                                  .build());
`````

## Sending media messages

`````java
SignalServiceMessageSender messageSender = new SignalServiceMessageSender(URL, TRUST_STORE, USERNAME, PASSWORD,
                                                                          new MySignalProtocolStore(),
                                                                          USER_AGENT, Optional.absent());

File                    myAttachment     = new File("/path/to/my.attachment");
FileInputStream         attachmentStream = new FileInputStream(myAttachment);
SignalServiceAttachment attachment       = SignalServiceAttachment.newStreamBuilder()
                                                                  .withStream(attachmentStream)
                                                                  .withContentType("image/png")
                                                                  .withLength(myAttachment.length())
                                                                  .build();

messageSender.sendMessage(new SignalServiceAddress("+14159998888"),
                          SignalServiceDataMessage.newBuilder()
                                                  .withBody("An attachment!")
                                                  .withAttachment(attachment)
                                                  .build());

`````

## Receiving messages

`````java
SignalServiceMessageReceiver messageReceiver = new SignalServiceMessageReceiver(URL, TRUST_STORE, USERNAME,
                                                                                PASSWORD, mySignalingKey,
                                                                                USER_AGENT);
SignalServiceMessagePipe     messagePipe     = null;

try {
  messagePipe = messageReceiver.createMessagePipe();

  while (listeningForMessages) {
    SignalServiceEnvelope envelope = messagePipe.read(timeout, timeoutTimeUnit);
    SignalServiceCipher   cipher   = new SignalServiceCipher(new SignalServiceAddress(USERNAME),
                                                             new MySignalProtocolStore());
    SignalServiceContent  message  = cipher.decrypt(envelope);

    System.out.println("Received message: " + message.getDataMessage().get().getBody().get());
  }

} finally {
  if (messagePipe != null)
    messagePipe.shutdown();
}
`````

# Legal things

## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.
The form and manner of this distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

## License

Copyright 2013-2016 Open Whisper Systems

Licensed under the AGPLv3: https://www.gnu.org/licenses/agpl-3.0.html

