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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
  void testGET() {
    server.map("/", ctx -> "OK");
    server.map("/path", ctx -> "path");
    assertThat(client.getString("http://localhost:8080/"), is("OK"));
    assertThat(client.getString("http://localhost:8080/path"), is("path"));
  }

  @Test
  void testPOST() {
    server.map("/", ctx -> "OK");
    server.map("/path", ctx -> "path");
    assertThat(client.postString("http://localhost:8080/", "{}"), is("OK"));
    assertThat(client.postString("http://localhost:8080/path", "{}"), is("path"));
  }
}
