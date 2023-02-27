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

import com.github.raffaeleragni.jx.http.Server.Context.Status;
import java.net.*;
import java.util.*;
import java.util.function.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.function.Function.identity;
import static org.hamcrest.CoreMatchers.hasItem;

class ClientTest {
  Client client;
  Server server;

  @BeforeEach
  void setup() {
    server = new Server(8080);
    client = new Client();
  }

  @AfterEach
  void teardown() {
    server.terminate();
  }

  @Test
  void testGET() throws Exception {
    server.map("/", ctx -> "OK");
    server.map("/path", ctx -> "path");
    assertThat(client.getString("http://localhost:8080/"), is("OK"));
    assertThat(client.getString("http://localhost:8080/path"), is("path"));
    assertThat(client.getString(new URI("http://localhost:8080/")), is("OK"));
    assertThat(client.getString(new URI("http://localhost:8080/path")), is("path"));
  }

  @Test
  void testReturnCodes() throws Exception {
    server.map("/", (Consumer) ctx -> {throw new Status(401);});

    assertThat(client.get("http://localhost:8080/").status(), is(401));
    assertThat(client.get(new URI("http://localhost:8080/")).status(), is(401));
  }

  @Test
  void testConverter() throws Exception {
    server.map("/", ctx -> "1");

    assertThat(client.get("http://localhost:8080/", Integer::valueOf).body(), is(1));
    assertThat(client.get(new URI("http://localhost:8080/"), Integer::valueOf).body(), is(1));
  }

  @Test
  void testtestHeadersGET() throws Exception {
    server.map("/", ctx -> {
      assertThat(ctx.header("content-type"), hasItem("text/plain"));
      assertThat(ctx.header("header1"), hasItem("Value1-1"));
      assertThat(ctx.header("HEADER1"), hasItem("Value1-2"));
      return "1";
    });

    var headers = Map.of(
      "Content-Type", List.of("text/plain"),
      "Header1", List.of("Value1-1", "Value1-2"),
      "Header2", List.<String>of()
    );

    assertThat(client.get("http://localhost:8080/", headers).body(), is("1"));
    assertThat(client.get("http://localhost:8080/", headers, identity()).body(), is("1"));
    assertThat(client.get(new URI("http://localhost:8080/"), headers).body(), is("1"));
    assertThat(client.get(new URI("http://localhost:8080/"), headers, identity()).body(), is("1"));
  }

  @Test
  void testPOST() {
    server.map("/", ctx -> "OK");
    server.map("/path", ctx -> "path");
    assertThat(client.postString("http://localhost:8080/", "{}"), is("OK"));
    assertThat(client.postString("http://localhost:8080/path", "{}"), is("path"));
  }
}
