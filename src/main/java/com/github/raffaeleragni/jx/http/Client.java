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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.*;

import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;

public class Client {

  public interface Response<T> {
    int status();
    T body();
  }

  final HttpClient httpClient;

  public Client() {
    this(Executors.newCachedThreadPool());
  }

  public Client(ExecutorService executor) {
    httpClient = HttpClient.newBuilder()
      .executor(executor)
      .build();
  }

  public Response<String> get(String url) {
    return unchecked(() -> get(new URI(url)));
  }

  public Response<String> get(URI url) {
    return get(url, emptyMap(), identity());
  }

  public Response<String> get(String url, Map<String, List<String>> headers) {
    return unchecked(() -> get(new URI(url), headers));
  }

  public Response<String> get(URI url, Map<String, List<String>> headers) {
    return get(url, headers, identity());
  }

  public <T> Response<T> get(String url, Function<String, T> converter) {
    return unchecked(() -> get(new URI(url), converter));
  }

  public <T> Response<T> get(URI url, Function<String, T> converter) {
    return get(url, emptyMap(), converter);
  }

  public <T> Response<T> get(String url, Map<String, List<String>> headers, Function<String, T> converter) {
    return unchecked(() -> get(new URI(url), headers, converter));
  }

  public <T> Response<T> get(URI url, Map<String, List<String>> headers, Function<String, T> converter) {
    var request = HttpRequest.newBuilder(url).GET();
    headers.entrySet().forEach(e ->
      e.getValue().forEach(v -> request.header(e.getKey(), v))
    );
    var response = unchecked(() -> httpClient.send(request.build(), BodyHandlers.ofString()));
    return new Response<T>() {
      @Override
      public int status() {
        return response.statusCode();
      }

      @Override
      public T body() {
        return converter.apply(response.body());
      }
    };
  }

  public String getString(String url) {
    return unchecked(() -> get(new URI(url)).body());
  }

  public String getString(URI url) {
    return unchecked(() -> get(url).body());
  }

  public String postString(String url, String body) {
    return unchecked(() -> Client.this.postString(new URI(url), body));
  }

  public String postString(URI url, String body) {
    var request = HttpRequest.newBuilder(url).POST(BodyPublishers.ofString(body)).build();
    return unchecked(() -> httpClient.send(request, BodyHandlers.ofString()).body());
  }
}
