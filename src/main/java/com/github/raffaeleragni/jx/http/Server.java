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
import com.sun.net.httpserver.HttpExchange; // NOSONAR
import com.sun.net.httpserver.HttpServer; // NOSONAR
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
  private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
  private final HttpServer instance;

  public Server(int port) {
    this(port, Executors.newCachedThreadPool());
  }

  public Server(int port, ExecutorService executor) {
    instance = unchecked(() -> {
      var s = HttpServer.create();
      s.bind(new InetSocketAddress(port), 0);
      s.setExecutor(executor);
      s.start();
      return s;
    });
    Runtime.getRuntime().addShutdownHook(new Thread(this::terminate));
  }

  public void terminate() {
    instance.stop(0);
  }

  public Server map(String path, Function<Server.Context, String> fn) {
    map(path, c -> {
      var resp = fn.apply(c);
      if (validResponse(resp))
        c.response(resp);
    });
    return this;
  }

  private boolean validResponse(String resp) {
    return resp != null && !resp.isEmpty();
  }

  public void map(String path, Consumer<Server.Context> fn) {
    instance.createContext(path, exchange -> wrapNewExchange(path, exchange, fn));
  }

  private void wrapNewExchange(String originalMappedPath, HttpExchange exchange, Consumer<Server.Context> fn) {
    var ctx = new ServerContextImpl(originalMappedPath, exchange);
    try {
      fn.accept(ctx);
      if (!ctx.responseSent)
        closeWithStatus(exchange, 204);
    } catch (Context.Status e) {
      closeWithStatus(exchange, e.status());
    } catch (RuntimeException e) {
      closeWithStatus(exchange, 500);
      LOGGER.log(Level.SEVERE, e, e::getMessage);
    }
  }

  private void closeWithStatus(final HttpExchange exchange, int status) {
    unchecked(() -> {
      exchange.sendResponseHeaders(status, 0);
      exchange.getResponseBody().close();
    });
  }

  public interface Context {
    String method();
    String mappedPath();
    String extraPath();
    String request();
    void response(String response);

    public static final class Status extends RuntimeException {
      private final int value;

      public Status(final int value) {
        super();
        this.value = value;
      }

      public int status() {
        return value;
      }
    }
  }
}

class ServerContextImpl implements Server.Context {
  private static final int STATUS_OK = 200;

  final String method;
  final String mappedPath;
  final String extraPath;
  final HttpExchange exchange;
  boolean responseSent;

  ServerContextImpl(String mappedPath, HttpExchange exchange) {
    this.exchange = exchange;
    this.mappedPath = mappedPath;
    this.method = exchange.getRequestMethod();
    this.extraPath = removeLeadingAndTralingSlashes(exchange
        .getRequestURI()
        .getPath()
        .substring(mappedPath.length()));
  }

  private String removeLeadingAndTralingSlashes(String extraPath) {
    if (extraPath.startsWith("/"))
      extraPath = extraPath.substring(1);
    if (extraPath.endsWith("/"))
      extraPath = extraPath.substring(0, extraPath.length() - 1);
    return extraPath;
  }

  @Override
  public String method() {
    return method;
  }

  @Override
  public String mappedPath() {
    return mappedPath;
  }

  @Override
  public String extraPath() {
    return extraPath;
  }

  @Override
  public void response(String response) {
    if (responseSent)
      return;
    consumeWriter(out ->
      unchecked(() -> {
        exchange.sendResponseHeaders(STATUS_OK, response.length());
        out.write(response);
        responseSent = true;
      })
    );
  }

  @Override
  public String request() {
    return consumeReader(in -> unchecked(() -> {
      var w = new StringWriter();
      in.transferTo(w);
      return w.toString();
    }));
  }

  private <T> T consumeReader(final Function<BufferedReader, T> reader) {
    return unchecked(() -> {
      try (var in = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), UTF_8))) {
        return reader.apply(in);
      }
    });
  }

  private void consumeWriter(final Consumer<BufferedWriter> writer) {
    unchecked(() -> {
      try (var out = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody(), UTF_8))) {
        writer.accept(out);
      }
    });
  }
}