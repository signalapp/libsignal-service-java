package org.whispersystems.signalservice.internal.websocket;

import com.google.protobuf.InvalidProtocolBufferException;

import org.whispersystems.libsignal.logging.Log;
import org.whispersystems.libsignal.util.Pair;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.internal.util.BlacklistingTrustManager;
import org.whispersystems.signalservice.internal.util.Util;
import org.whispersystems.signalservice.internal.util.concurrent.SettableFuture;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static org.whispersystems.signalservice.internal.websocket.WebSocketProtos.WebSocketMessage;
import static org.whispersystems.signalservice.internal.websocket.WebSocketProtos.WebSocketRequestMessage;
import static org.whispersystems.signalservice.internal.websocket.WebSocketProtos.WebSocketResponseMessage;

public class WebSocketConnection extends WebSocketListener {

  private static final String TAG                       = WebSocketConnection.class.getSimpleName();
  private static final int    KEEPALIVE_TIMEOUT_SECONDS = 55;

  private final LinkedList<WebSocketRequestMessage>              incomingRequests = new LinkedList<>();
  private final Map<Long, SettableFuture<Pair<Integer, String>>> outgoingRequests = new HashMap<>();

  private final String              wsUri;
  private final TrustStore          trustStore;
  private final CredentialsProvider credentialsProvider;
  private final String              userAgent;

  private WebSocket           client;
  private KeepAliveSender     keepAliveSender;
  private int                 attempts;
  private boolean             connected;

  public WebSocketConnection(String httpUri, TrustStore trustStore, CredentialsProvider credentialsProvider, String userAgent) {
    this.trustStore          = trustStore;
    this.credentialsProvider = credentialsProvider;
    this.userAgent           = userAgent;
    this.attempts            = 0;
    this.connected           = false;
    this.wsUri               = httpUri.replace("https://", "wss://")
                                      .replace("http://", "ws://") + "/v1/websocket/?login=%s&password=%s";
  }

  public synchronized void connect() {
    Log.w(TAG, "WSC connect()...");

    if (client == null) {
      String                                   filledUri     = String.format(wsUri, credentialsProvider.getUser(), credentialsProvider.getPassword());
      Pair<SSLSocketFactory, X509TrustManager> socketFactory = createTlsSocketFactory(trustStore);

      OkHttpClient okHttpClient = new OkHttpClient.Builder()
                                                  .sslSocketFactory(socketFactory.first(), socketFactory.second())
                                                  .readTimeout(KEEPALIVE_TIMEOUT_SECONDS + 10, TimeUnit.SECONDS)
                                                  .connectTimeout(KEEPALIVE_TIMEOUT_SECONDS + 10, TimeUnit.SECONDS)
                                                  .build();

      Request.Builder requestBuilder = new Request.Builder().url(filledUri);

      if (userAgent != null) {
        requestBuilder.addHeader("X-Signal-Agent", userAgent);
      }

      this.connected = false;
      this.client    = okHttpClient.newWebSocket(requestBuilder.build(), this);
    }
  }

  public synchronized void disconnect() {
    Log.w(TAG, "WSC disconnect()...");

    if (client != null) {
      client.close(1000, "OK");
      client    = null;
      connected = false;
    }

    if (keepAliveSender != null) {
      keepAliveSender.shutdown();
      keepAliveSender = null;
    }
  }

  public synchronized WebSocketRequestMessage readRequest(long timeoutMillis)
      throws TimeoutException, IOException
  {
    if (client == null) {
      throw new IOException("Connection closed!");
    }

    long startTime = System.currentTimeMillis();

    while (client != null && incomingRequests.isEmpty() && elapsedTime(startTime) < timeoutMillis) {
      Util.wait(this, Math.max(1, timeoutMillis - elapsedTime(startTime)));
    }

    if      (incomingRequests.isEmpty() && client == null) throw new IOException("Connection closed!");
    else if (incomingRequests.isEmpty())                   throw new TimeoutException("Timeout exceeded");
    else                                                   return incomingRequests.removeFirst();
  }

  public synchronized Future<Pair<Integer, String>> sendRequest(WebSocketRequestMessage request) throws IOException {
    if (client == null || !connected) throw new IOException("No connection!");

    WebSocketMessage message = WebSocketMessage.newBuilder()
                                               .setType(WebSocketMessage.Type.REQUEST)
                                               .setRequest(request)
                                               .build();

    SettableFuture<Pair<Integer, String>> future = new SettableFuture<>();
    outgoingRequests.put(request.getId(), future);

    if (!client.send(ByteString.of(message.toByteArray()))) {
      throw new IOException("Write failed!");
    }

    return future;
  }

  public synchronized void sendResponse(WebSocketResponseMessage response) throws IOException {
    if (client == null) {
      throw new IOException("Connection closed!");
    }

    WebSocketMessage message = WebSocketMessage.newBuilder()
                                               .setType(WebSocketMessage.Type.RESPONSE)
                                               .setResponse(response)
                                               .build();

    if (!client.send(ByteString.of(message.toByteArray()))) {
      throw new IOException("Write failed!");
    }
  }

  private synchronized void sendKeepAlive() throws IOException {
    if (keepAliveSender != null && client != null) {
      byte[] message = WebSocketMessage.newBuilder()
                                       .setType(WebSocketMessage.Type.REQUEST)
                                       .setRequest(WebSocketRequestMessage.newBuilder()
                                                                          .setId(System.currentTimeMillis())
                                                                          .setPath("/v1/keepalive")
                                                                          .setVerb("GET")
                                                                          .build()).build()
                                       .toByteArray();

      if (!client.send(ByteString.of(message))) {
        throw new IOException("Write failed!");
      }
    }
  }

  @Override
  public synchronized void onOpen(WebSocket webSocket, Response response) {
    if (client != null && keepAliveSender == null) {
      Log.w(TAG, "onConnected()");
      attempts        = 0;
      connected       = true;
      keepAliveSender = new KeepAliveSender();
      keepAliveSender.start();

    }
  }

  @Override
  public synchronized void onMessage(WebSocket webSocket, ByteString payload) {
    Log.w(TAG, "WSC onMessage()");
    try {
      WebSocketMessage message = WebSocketMessage.parseFrom(payload.toByteArray());

      Log.w(TAG, "Message Type: " + message.getType().getNumber());

      if (message.getType().getNumber() == WebSocketMessage.Type.REQUEST_VALUE)  {
        incomingRequests.add(message.getRequest());
      } else if (message.getType().getNumber() == WebSocketMessage.Type.RESPONSE_VALUE) {
        SettableFuture<Pair<Integer, String>> listener = outgoingRequests.get(message.getResponse().getId());
        if (listener != null) listener.set(new Pair<>(message.getResponse().getStatus(),
                                                      new String(message.getResponse().getBody().toByteArray())));
      }

      notifyAll();
    } catch (InvalidProtocolBufferException e) {
      Log.w(TAG, e);
    }
  }

  @Override
  public synchronized void onClosed(WebSocket webSocket, int code, String reason) {
    Log.w(TAG, "onClose()...");
    this.connected = false;

    Iterator<Map.Entry<Long, SettableFuture<Pair<Integer, String>>>> iterator = outgoingRequests.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<Long, SettableFuture<Pair<Integer, String>>> entry = iterator.next();
      entry.getValue().setException(new IOException("Closed: " + code + ", " + reason));
      iterator.remove();
    }

    if (keepAliveSender != null) {
      keepAliveSender.shutdown();
      keepAliveSender = null;
    }

    Util.wait(this, Math.min(++attempts * 200, TimeUnit.SECONDS.toMillis(15)));

    if (client != null) {
      client.close(1000, "OK");
      client    = null;
      connected = false;
      connect();
    }

    notifyAll();
  }

  @Override
  public synchronized void onFailure(WebSocket webSocket, Throwable t, Response response) {
    Log.w(TAG, "onFailure()");
    Log.w(TAG, t);

    if (client != null) {
      onClosed(webSocket, 1000, "OK");
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    Log.w(TAG, "onMessage(text)! " + text);
  }

  @Override
  public synchronized void onClosing(WebSocket webSocket, int code, String reason) {
    Log.w(TAG, "onClosing()!...");
    webSocket.close(1000, "OK");
  }

  private long elapsedTime(long startTime) {
    return System.currentTimeMillis() - startTime;
  }

  private Pair<SSLSocketFactory, X509TrustManager> createTlsSocketFactory(TrustStore trustStore) {
    try {
      SSLContext     context       = SSLContext.getInstance("TLS");
      TrustManager[] trustManagers = BlacklistingTrustManager.createFor(trustStore);
      context.init(null, trustManagers, null);

      return new Pair<>(context.getSocketFactory(), (X509TrustManager)trustManagers[0]);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new AssertionError(e);
    }
  }

  private class KeepAliveSender extends Thread {

    private AtomicBoolean stop = new AtomicBoolean(false);

    public void run() {
      while (!stop.get()) {
        try {
          Thread.sleep(TimeUnit.SECONDS.toMillis(KEEPALIVE_TIMEOUT_SECONDS));

          Log.w(TAG, "Sending keep alive...");
          sendKeepAlive();
        } catch (Throwable e) {
          Log.w(TAG, e);
        }
      }
    }

    public void shutdown() {
      stop.set(true);
    }
  }

}
