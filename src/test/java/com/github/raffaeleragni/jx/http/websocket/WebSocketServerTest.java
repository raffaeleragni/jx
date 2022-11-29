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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebSocketServerTest {
  Server server;
  WebSocket ws;
  boolean opened;
  String lastReceivedText;
  Throwable error;

  @BeforeEach
  void setup() {
    server = new Server(9999);
    ws = connectClient();
  }

  @Test
  void testMethod() {
    assertThat(opened, is(true));
  }

  private WebSocket connectClient() {
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
