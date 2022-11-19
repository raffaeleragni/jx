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
package com.github.raffaeleragni.jx.http;

import static com.github.raffaeleragni.jx.TestHelper.isPortOccupied;
import com.github.raffaeleragni.jx.http.Server.Context;
import com.github.raffaeleragni.jx.http.Server.Context.Status;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import java.util.Random;
import java.util.function.Consumer;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerTest {
  int port;
  Server server;

  @BeforeEach
  void setup() {
    port = new Random(System.currentTimeMillis()).nextInt(50_000)+1024;
    while (isPortOccupied(port))
      port++;

    server = new Server(port);
  }

  @Test
  void testPortOccupied() {
    assertThat(isPortOccupied(port), is(true));
  }

  @Test
  void testStop() {
    server.terminate();
    assertThat(isPortOccupied(port), is(false));
  }

  @Test
  void testGet() throws Exception {
    server.map("/path", c -> "hello");
    assertThat(get("/path"), is("hello"));
  }

  @Test
  void testEmptyGet() throws Exception {
    server.map("/path", c -> "");
    assertThat(get("/path"), is(""));
  }

  @Test
  void testNoResponse() throws Exception {
    server.map("/path", c -> {});
    assertThat(getStatus("/path"), is(204));
    server.map("/path2", c -> null);
    assertThat(getStatus("/path2"), is(204));
  }

  @Test
  void testStatusThrow() throws Exception {
    server.map("/path1", (Consumer<Context>) c -> {throw new Status(200);});
    assertThat(getStatus("/path1"), is(200));
    server.map("/path2", (Consumer<Context>) c -> {throw new RuntimeException();});
    assertThat(getStatus("/path2"), is(500));
  }

  @Test
  void testPaths() throws Exception {
    server.map("/path", c -> {
      assertThat(c.method(), is("GET"));
      assertThat(c.mappedPath(), is("/path"));
      assertThat(c.extraPath(), is("extra"));
      return "";
    });
    assertThat(get("/path/extra"), is(""));

    server.map("/path2", c -> {
      assertThat(c.method(), is("GET"));
      assertThat(c.mappedPath(), is("/path2"));
      assertThat(c.extraPath(), is("extra/paths"));
      return "";
    });
    assertThat(get("/path2/extra/paths/"), is(""));
  }

  @Test
  void testDoubleResponse() throws Exception {
    server.map("/path", c -> {
      c.response("hello");
      c.response("hello2");
    });
    assertThat(get("/path"), is("hello"));
  }

  @Test
  void testRequestBody() throws Exception {
    server.map("/path", c -> {
      assertThat(c.request(), is("body"));
    });
    post("/path", "body");
  }

  private String get(String path) throws IOException, URISyntaxException, InterruptedException {
    var req = HttpRequest.newBuilder(new URI("http://localhost:"+port+path)).GET().build();
    return HttpClient.newHttpClient().send(req, ofString()).body();
  }

  private int getStatus(String path) throws IOException, URISyntaxException, InterruptedException {
    var req = HttpRequest.newBuilder(new URI("http://localhost:"+port+path)).GET().build();
    return HttpClient.newHttpClient().send(req, ofString()).statusCode();
  }

  private String post(String path, String body) throws IOException, URISyntaxException, InterruptedException {
    var req = HttpRequest.newBuilder(new URI("http://localhost:"+port+path)).POST(BodyPublishers.ofString(body)).build();
    return HttpClient.newHttpClient().send(req, ofString()).body();
  }
}
