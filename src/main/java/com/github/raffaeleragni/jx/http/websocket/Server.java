/*
 * Copyright 2022 Raffaele Ragni.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.raffaeleragni.jx.http.websocket;

import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * WebSocket server.
 * This only implements web sockets, HTTP is excluded.
 * The only HTTP code present is for the handshake.
 *
 * @author Raffaele Ragni
 */
public class Server implements Closeable {

  @FunctionalInterface
  public interface BufferPipe {
    byte[] process(byte[] input);
  }

  final ServerSocket serverSocket;
  final ExecutorService executor;
  final ExecutorService listenerExecutor;
  final Map<String, BufferPipe> bufferPipes;
  // Will use Socket object pointers for equals/hash
  final Set<SessionHandler> handlers = new HashSet<>();

  public Server(int port, Map<String, BufferPipe> bufferPipe) {
    this(null, port, bufferPipe);
  }

  public Server(InetAddress bind, int port, Map<String, BufferPipe> bufferPipe) {
    this(bind, port, bufferPipe, Executors.newCachedThreadPool());
  }

  public Server(InetAddress bind, int port, Map<String, BufferPipe> bufferPipe, ExecutorService executor) {
    this.executor = executor;
    this.bufferPipes = bufferPipe;
    serverSocket = unchecked(() -> new ServerSocket(port, 50, bind));
    listenerExecutor = Executors.newSingleThreadExecutor();
    listenerExecutor.submit(this::keepAccepting);
  }

  private void keepAccepting() {
    while (!serverSocket.isClosed()) {
      try {
        acceptedSocket(serverSocket.accept());
      } catch (IOException ex) {
        logError(ex);
      }
    }
  }

  private void acceptedSocket(Socket socket) {
    var handler = new SessionHandler(socket, bufferPipes);
    handlers.add(handler);
    executor.submit(handler);
  }

  @Override
  public void close() throws IOException {
    try (listenerExecutor; serverSocket) {
      for (var handler: handlers)
        handler.close();
    }
  }

  private void logError(Throwable ex) {
    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
  }
}

class SessionHandler implements Closeable, Runnable {

  private static final String WEBSOCKET_UUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  final Socket socket;
  final InputStream input;
  final OutputStream output;
  final MessageDigest sha1;
  final Map<String, Server.BufferPipe> bufferPipes;
  boolean upgraded = false;

  public SessionHandler(Socket socket, Map<String, Server.BufferPipe> bufferPipes) {
    this.socket = socket;
    this.bufferPipes = bufferPipes;
    input = unchecked(socket::getInputStream);
    output = unchecked(socket::getOutputStream);
    sha1 = unchecked(() -> MessageDigest.getInstance("SHA-1")); //NOSONAR SHA-1 is required for WebSocket even if outdated
  }

  @Override
  public void close() throws IOException {
    input.close();
    output.close();
    socket.close();
  }

  @Override
  public void run() {
    waitAndUpgradeToWebSocket();
  }

  private void waitAndUpgradeToWebSocket() {
    while (!upgraded)
      upgrade();
    while (!socket.isClosed())
      handleSession();
  }

  private void upgrade() {
    try (var scanner = new Scanner(new NonCloseableInputStream(input), UTF_8)) {
      var request = scanner.useDelimiter("\\r\\n\\r\\n").next();
      if (!findGET(request))
        return;
      findKey(request).ifPresent(key -> {
        handshakeRespond(key);
        upgraded = true;
      });
    }
  }

  private static boolean findGET(String request) {
    return Pattern.compile("^GET").matcher(request).find();
  }

  private Optional<String> findKey(String request) {
    var pattern = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(request);
    if (!pattern.find())
      return Optional.empty();
    return Optional.of(pattern.group(1));
  }

  private void handshakeRespond(String key) {
    byte[] response =  unchecked(() ->
      ( "HTTP/1.1 101 Switching Protocols\r\n"
      + "Connection: Upgrade\r\n"
      + "Upgrade: websocket\r\n"
      + "Sec-WebSocket-Accept: %s".formatted(sha1base64(key + WEBSOCKET_UUID))
      + "\r\n\r\n")
      .getBytes(UTF_8)
    );
    unchecked(() -> output.write(response, 0, response.length));
  }

  private String sha1base64(String s) {
    return unchecked(() -> Base64.getEncoder().encodeToString(sha1.digest(s.getBytes(UTF_8))));
  }

  private void handleSession() {
    // TODO
  }
}

class NonCloseableInputStream extends FilterInputStream { //NOSONAR Used for close prevention

  NonCloseableInputStream(InputStream input) {
    super(input);
  }

  @Override
  public void close() throws IOException {
    // Prevent closing to the underlying stream when the consumer closes.
    // This will allow to consume the input stream again with a different wrapper.
  }
}
