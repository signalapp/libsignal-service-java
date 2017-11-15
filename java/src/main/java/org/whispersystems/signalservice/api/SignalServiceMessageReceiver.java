/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api;

import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.signalservice.api.crypto.AttachmentCipherInputStream;
import org.whispersystems.signalservice.api.crypto.ProfileCipherInputStream;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment.ProgressListener;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.profiles.SignalServiceProfile;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;
import org.whispersystems.signalservice.internal.push.PushServiceSocket;
import org.whispersystems.signalservice.internal.push.SignalServiceEnvelopeEntity;
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl;
import org.whispersystems.signalservice.internal.util.StaticCredentialsProvider;
import org.whispersystems.signalservice.internal.websocket.WebSocketConnection;
import org.whispersystems.signalservice.internal.websocket.WebSocketEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * The primary interface for receiving Signal Service messages.
 *
 * @author Moxie Marlinspike
 */
public class SignalServiceMessageReceiver {

  private final PushServiceSocket          socket;
  private final SignalServiceConfiguration urls;
  private final CredentialsProvider        credentialsProvider;
  private final String                     userAgent;
  private final ConnectivityListener       connectivityListener;

  /**
   * Construct a SignalServiceMessageReceiver.
   *
   * @param urls The URL of the Signal Service.
   * @param user The Signal Service username (eg. phone number).
   * @param password The Signal Service user password.
   * @param signalingKey The 52 byte signaling key assigned to this user at registration.
   */
  public SignalServiceMessageReceiver(SignalServiceConfiguration urls,
                                      String user, String password,
                                      String signalingKey, String userAgent,
                                      ConnectivityListener listener)
  {
    this(urls, new StaticCredentialsProvider(user, password, signalingKey), userAgent, listener);
  }

  /**
   * Construct a SignalServiceMessageReceiver.
   *
   * @param urls The URL of the Signal Service.
   * @param credentials The Signal Service user's credentials.
   */
  public SignalServiceMessageReceiver(SignalServiceConfiguration urls, CredentialsProvider credentials, String userAgent, ConnectivityListener listener)
  {
    this.urls                 = urls;
    this.credentialsProvider  = credentials;
    this.socket               = new PushServiceSocket(urls, credentials, userAgent);
    this.userAgent            = userAgent;
    this.connectivityListener = listener;
  }

  /**
   * Retrieves a SignalServiceAttachment.
   *
   * @param pointer The {@link SignalServiceAttachmentPointer}
   *                received in a {@link SignalServiceDataMessage}.
   * @param destination The download destination for this attachment.
   *
   * @return An InputStream that streams the plaintext attachment contents.
   * @throws IOException
   * @throws InvalidMessageException
   */
  public InputStream retrieveAttachment(SignalServiceAttachmentPointer pointer, File destination, int maxSizeBytes)
      throws IOException, InvalidMessageException
  {
    return retrieveAttachment(pointer, destination, maxSizeBytes, null);
  }

  public SignalServiceProfile retrieveProfile(SignalServiceAddress address)
    throws IOException
  {
    return socket.retrieveProfile(address);
  }

  public InputStream retrieveProfileAvatar(String path, File destination, byte[] profileKey, int maxSizeBytes)
    throws IOException
  {
    socket.retrieveProfileAvatar(path, destination, maxSizeBytes);
    return new ProfileCipherInputStream(new FileInputStream(destination), profileKey);
  }

  /**
   * Retrieves a SignalServiceAttachment.
   *
   * @param pointer The {@link SignalServiceAttachmentPointer}
   *                received in a {@link SignalServiceDataMessage}.
   * @param destination The download destination for this attachment.
   * @param listener An optional listener (may be null) to receive callbacks on download progress.
   *
   * @return An InputStream that streams the plaintext attachment contents.
   * @throws IOException
   * @throws InvalidMessageException
   */
  public InputStream retrieveAttachment(SignalServiceAttachmentPointer pointer, File destination, int maxSizeBytes, ProgressListener listener)
      throws IOException, InvalidMessageException
  {
    if (!pointer.getDigest().isPresent()) throw new InvalidMessageException("No attachment digest!");

    socket.retrieveAttachment(pointer.getRelay().orNull(), pointer.getId(), destination, maxSizeBytes, listener);
    return AttachmentCipherInputStream.createFor(destination, pointer.getSize().or(0), pointer.getKey(), pointer.getDigest().get());
  }

  /**
   * Creates a pipe for receiving SignalService messages.
   *
   * Callers must call {@link SignalServiceMessagePipe#shutdown()} when finished with the pipe.
   *
   * @return A SignalServiceMessagePipe for receiving Signal Service messages.
   */
  public SignalServiceMessagePipe createMessagePipe() {
    WebSocketConnection webSocket = new WebSocketConnection(urls.getSignalServiceUrls()[0].getUrl(),
                                                            urls.getSignalServiceUrls()[0].getTrustStore(),
                                                            credentialsProvider, userAgent, connectivityListener);

    return new SignalServiceMessagePipe(webSocket, credentialsProvider);
  }

  public List<SignalServiceEnvelope> retrieveMessages() throws IOException {
    return retrieveMessages(new NullMessageReceivedCallback());
  }

  public List<SignalServiceEnvelope> retrieveMessages(MessageReceivedCallback callback)
      throws IOException
  {
    List<SignalServiceEnvelope>       results  = new LinkedList<>();
    List<SignalServiceEnvelopeEntity> entities = socket.getMessages();

    for (SignalServiceEnvelopeEntity entity : entities) {
      SignalServiceEnvelope envelope =  new SignalServiceEnvelope(entity.getType(), entity.getSource(),
                                                                  entity.getSourceDevice(), entity.getRelay(),
                                                                  entity.getTimestamp(), entity.getMessage(),
                                                                  entity.getContent());

      callback.onMessage(envelope);
      results.add(envelope);

      socket.acknowledgeMessage(entity.getSource(), entity.getTimestamp());
    }

    return results;
  }


  public interface MessageReceivedCallback {
    public void onMessage(SignalServiceEnvelope envelope);
  }

  public static class NullMessageReceivedCallback implements MessageReceivedCallback {
    @Override
    public void onMessage(SignalServiceEnvelope envelope) {}
  }

}
