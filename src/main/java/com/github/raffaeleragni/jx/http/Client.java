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

import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

  final HttpClient httpClient;

  public Client() {
    this(Executors.newCachedThreadPool());
  }

  public Client(ExecutorService executor) {
    httpClient = HttpClient.newBuilder()
      .executor(executor)
      .build();
  }

  public String getString(String url) {
    return unchecked(() -> getString(new URI(url)));
  }

  public String getString(URI url) {
    var reqest = HttpRequest.newBuilder(url).GET().build();
    return unchecked(() -> httpClient.send(reqest, BodyHandlers.ofString()).body());
  }

  public String postString(String url, String body) {
    return unchecked(() -> Client.this.postString(new URI(url), body));
  }

  public String postString(URI url, String body) {
    var reqest = HttpRequest.newBuilder(url).POST(BodyPublishers.ofString(body)).build();
    return unchecked(() -> httpClient.send(reqest, BodyHandlers.ofString()).body());
  }
}
