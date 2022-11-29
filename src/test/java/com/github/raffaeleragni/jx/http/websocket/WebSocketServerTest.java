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

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebSocketServerTest {
  Server server;
  boolean opened;
  String lastReceivedText;
  Throwable error;

  @BeforeEach
  void setup() {
    server = new Server(9999, in -> in);
  }

  @AfterEach
  void teardown() throws IOException {
    server.close();
  }

  @Test
  void testWrongRequestToConnect() throws Exception {
    connectWrongly();
    assertThat(opened, is(false));
  }

  @Test
  void testConnect() {
    connectProperly();
    assertThat(opened, is(true));
  }

  @Test
  void testConnectTwice() {
    connectProperly();
    assertThat(opened, is(true));

    opened = false;

    connectProperly();
    assertThat(opened, is(true));
  }

  @Test
  void testClosesBeforeConnecting() throws IOException {
    server.close();
    assertThrows(CompletionException.class, () -> connectProperly());
  }

  @Test
  void testOnDanglingSocketServerIsStillUp() throws Exception {
    assertDoesNotThrow(() -> {
      try (var sock = new Socket("localhost", 9999)) {
        // leave a closed socket behind
      }
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "HEAD / HTTP/1.1",
    "GET / HTTP/1.1",
    ""
  })
  void testWrongConnectionsAndThenSuccess(String content) throws Exception {
    assertZeroResponseWithString(content);
  }

  @Test
  void testCorrectSendWithHeader() throws Exception {
    assertNonZeroResponseWithString(
    """
    GET / HTTP/1.1
    Sec-WebSocket-Key: test
    """);
  }

  private void assertZeroResponseWithString(String content) throws Exception {
    try (var sock = new Socket("localhost", 9999)) {
      try (var out = sock.getOutputStream()) {
        try (var in = sock.getInputStream()) {
          out.write((content+"\r\n\r\n").getBytes("UTF-8"));
          out.flush();
          Thread.sleep(100);//NOSONAR
          assertThat(in.available(), is(0));
        }
      }
    }
  }

  private void assertNonZeroResponseWithString(String content) throws Exception {
    try (var sock = new Socket("localhost", 9999)) {
      try (var out = sock.getOutputStream()) {
        try (var in = sock.getInputStream()) {
          out.write((content+"\r\n\r\n").getBytes("UTF-8"));
          out.flush();
          Thread.sleep(100);//NOSONAR
          assertThat(in.available(), is(not(0)));
        }
      }
    }
  }

  private void connectWrongly() {
    var req = HttpRequest
      .newBuilder(URI.create("http://localhost:9999/"))
      .timeout(Duration.ofMillis(10))
      .POST(HttpRequest.BodyPublishers.ofString("{}"))
      .build();
    assertThrows(HttpTimeoutException.class, () ->
      HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(10))
        .build()
        .send(req, HttpResponse.BodyHandlers.ofString()).body()
    );
  }

  private WebSocket connectProperly() {
    return HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:9999/"), new WebSocket.Listener() {
      StringBuilder builder = new StringBuilder();

      @Override
      public void onOpen(WebSocket webSocket) {
        opened = true;
        WebSocket.Listener.super.onOpen(webSocket);
      }

      @Override
      public void onError(WebSocket webSocket, Throwable error) {
        WebSocketServerTest.this.error = error;
        WebSocket.Listener.super.onError(webSocket, error);
      }

      @Override
      public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        builder.append(data);
        if (!last)
          return WebSocket.Listener.super.onText(webSocket, data, last);

        WebSocketServerTest.this.lastReceivedText = builder.toString();
        builder = new StringBuilder();

        return WebSocket.Listener.super.onText(webSocket, data, last);
      }
    }).join();
  }
}
