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
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Scanner;
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

  final ServerSocket serverSocket;
  final ExecutorService executor;

  public Server(int port) {
    this(port, Executors.newCachedThreadPool());
  }

  public Server(int port, ExecutorService executor) {
    this.executor = executor;
    serverSocket = unchecked(() -> new ServerSocket(port));
    Executors.newSingleThreadExecutor().submit(this::keepAccepting);
  }

  private void keepAccepting() {
    while (!serverSocket.isClosed()) {
      try {
        var s = serverSocket.accept();
        executor.submit(() -> acceptedSocket(s));
      }catch (IOException ex) {
        logError(ex);
      }
    }
  }

  @Override
  public void close() throws IOException {
    serverSocket.close();
  }

  private void acceptedSocket(Socket socket) {
    new SessionHandler(socket).run();
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
  final MessageDigest SHA1;
  boolean upgraded = false;

  public SessionHandler(Socket socket) {
    this.socket = socket;
    input = unchecked(() -> socket.getInputStream());
    output = unchecked(() -> socket.getOutputStream());
    SHA1 = unchecked(() -> MessageDigest.getInstance("SHA-1"));
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
    while (!socket.isClosed() && !upgraded)
      upgrade();
    while (!socket.isClosed())
      handleSession();
  }

  private void upgrade() {
    try (var scanner = new Scanner(new NonCloseableInputStream(input), "UTF-8")) {
      var request = scanner.useDelimiter("\\r\\n\\r\\n").next();
      if (!findGET(request))
        return;
      handshakeRespond(findKey(request));
      upgraded = true;
    }
  }

  private static boolean findGET(String request) {
    return Pattern.compile("^GET").matcher(request).find();
  }

  private String findKey(String request) {
    var pattern = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(request);
    pattern.find();
    var key = pattern.group(1);
    return key;
  }

  private void handshakeRespond(String key) {
    byte[] response =  unchecked(() ->
      ( "HTTP/1.1 101 Switching Protocols\r\n"
      + "Connection: Upgrade\r\n"
      + "Upgrade: websocket\r\n"
      + "Sec-WebSocket-Accept: %s\r\n".formatted(sha1base64(key + WEBSOCKET_UUID))
      + "\r\n")
      .getBytes("UTF-8")
    );
    unchecked(() -> output.write(response, 0, response.length));
  }

  private String sha1base64(String s) {
    return unchecked(() -> Base64.getEncoder().encodeToString(SHA1.digest(s.getBytes("UTF-8"))));
  }

  private void handleSession() {
  }
}

class NonCloseableInputStream extends FilterInputStream {

  NonCloseableInputStream(InputStream input) {
    super(input);
  }

  @Override
  public void close() throws IOException {
    // Prevent closing to the underlying stream when the consumer closes.
    // This will allow to consume the input stream again with a different wrapper.
  }
}
