/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api;

import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.signalservice.api.crypto.AttachmentCipherInputStream;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment.ProgressListener;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.internal.push.PushServiceSocket;
import org.whispersystems.signalservice.internal.push.SignalServiceEnvelopeEntity;
import org.whispersystems.signalservice.internal.util.StaticCredentialsProvider;
import org.whispersystems.signalservice.internal.websocket.WebSocketConnection;

import java.io.File;
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

  private final PushServiceSocket   socket;
  private final TrustStore          trustStore;
  private final String              url;
  private final CredentialsProvider credentialsProvider;
  private final String              userAgent;

  /**
   * Construct a SignalServiceMessageReceiver.
   *
   * @param url The URL of the Signal Service.
   * @param trustStore The {@link org.whispersystems.signalservice.api.push.TrustStore} containing
   *                   the server's TLS signing certificate.
   * @param user The Signal Service username (eg. phone number).
   * @param password The Signal Service user password.
   * @param deviceId A integer which is provided by the server while linking.
   * @param signalingKey The 52 byte signaling key assigned to this user at registration.
   */
  public SignalServiceMessageReceiver(String url, TrustStore trustStore,
                                      String user, String password, int deviceId,
                                      String signalingKey, String userAgent)
  {
    this(url, trustStore, new StaticCredentialsProvider(user, password, signalingKey, deviceId), userAgent);
  }
  
  /**
   * Construct a SignalServiceMessageReceiver.
   *
   * @param url The URL of the Signal Service.
   * @param trustStore The {@link org.whispersystems.signalservice.api.push.TrustStore} containing
   *                   the server's TLS signing certificate.
   * @param user The Signal Service username (eg. phone number).
   * @param password The Signal Service user password.
   * @param signalingKey The 52 byte signaling key assigned to this user at registration.
   */
  public SignalServiceMessageReceiver(String url, TrustStore trustStore,
                                      String user, String password,
                                      String signalingKey, String userAgent)
  {
    this(url, trustStore, new StaticCredentialsProvider(user, password, signalingKey, SignalServiceAddress.DEFAULT_DEVICE_ID), userAgent);
  }

  /**
   * Construct a SignalServiceMessageReceiver.
   *
   * @param url The URL of the Signal Service.
   * @param trustStore The {@link org.whispersystems.signalservice.api.push.TrustStore} containing
   *                   the server's TLS signing certificate.
   * @param credentials The Signal Service user's credentials.
   */
  public SignalServiceMessageReceiver(String url, TrustStore trustStore,
                                      CredentialsProvider credentials, String userAgent)
  {
    this.url                 = url;
    this.trustStore          = trustStore;
    this.credentialsProvider = credentials;
    this.socket              = new PushServiceSocket(url, trustStore, credentials, userAgent);
    this.userAgent           = userAgent;
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
  public InputStream retrieveAttachment(SignalServiceAttachmentPointer pointer, File destination)
      throws IOException, InvalidMessageException
  {
    return retrieveAttachment(pointer, destination, null);
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
  public InputStream retrieveAttachment(SignalServiceAttachmentPointer pointer, File destination, ProgressListener listener)
      throws IOException, InvalidMessageException
  {
    socket.retrieveAttachment(pointer.getRelay().orNull(), pointer.getId(), destination, listener);
    return new AttachmentCipherInputStream(destination, pointer.getKey());
  }

  /**
   * Creates a pipe for receiving SignalService messages.
   *
   * Callers must call {@link SignalServiceMessagePipe#shutdown()} when finished with the pipe.
   *
   * @return A SignalServiceMessagePipe for receiving Signal Service messages.
   */
  public SignalServiceMessagePipe createMessagePipe() {
    WebSocketConnection webSocket = new WebSocketConnection(url, trustStore, credentialsProvider, userAgent);
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
