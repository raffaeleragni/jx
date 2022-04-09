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
package com.github.raffaeleragni.jx.jdbc;

import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;
import com.github.raffaeleragni.jx.records.Records;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Jdbc {

  final Supplier<Connection> connectionSupplier;

  @FunctionalInterface
  public interface ExConsumer<T> {
    void accept(T t) throws Exception;  // NOSONAR
  }

  @FunctionalInterface
  public interface RecordMapper<T> {
    T map(ResultSet rs);
  }

  public Jdbc(Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = Objects.requireNonNull(connectionSupplier);
  }

  public boolean healthy() {
    return wrapConnectionR(c -> unchecked(() -> c.isValid(0)));
  }

  public int execute(String sql, ExConsumer<PreparedStatement> paramSetter) {
    Objects.requireNonNull(sql);
    Objects.requireNonNull(paramSetter);

    return wrapConnectionR(connection ->
      wrapStreamedStatementR(connection, sql, paramSetter, st ->
        unchecked(() -> st.executeUpdate())  // NOSONAR
      )
    );
  }

  public void streamed(String sql, ExConsumer<PreparedStatement> paramSetter, ExConsumer<ResultSet> resultGetter) {
    Objects.requireNonNull(sql);
    Objects.requireNonNull(paramSetter);
    Objects.requireNonNull(resultGetter);

    wrapConnection(connection ->
      wrapStreamedStatement(connection, sql, paramSetter, st ->
        streamResultSetResultsOnCallback(st, resultGetter::accept)
      )
    );
  }

  public static <T extends Record> RecordMapper<T> recordMapperOf(Class<T> clazz) {
    return new RecordMapperImpl<>(clazz);
  }

  private void streamResultSetResultsOnCallback(PreparedStatement st, ExConsumer<ResultSet> consumer) {
    unchecked(() -> {
      Objects.requireNonNull(consumer);
      try (var rs = st.executeQuery()) {
        while (rs.next())
          consumer.accept(rs);
      }
    });
  }

  private void wrapStreamedStatement(Connection connection, String sql, ExConsumer<PreparedStatement> paramSetter, Consumer<PreparedStatement> consumer) {
    unchecked(() -> {
      Objects.requireNonNull(consumer);
      try (var st = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
        paramSetter.accept(st);
        consumer.accept(st);
      }
    });
  }

  private <T> T wrapStreamedStatementR(Connection connection, String sql, ExConsumer<PreparedStatement> paramSetter, Function<PreparedStatement, T> function) {
    return unchecked(() -> {
      Objects.requireNonNull(function);
      try (var st = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
        paramSetter.accept(st);
        return function.apply(st);
      }
    });
  }

  private void wrapConnection(Consumer<Connection> consumer) {
    unchecked(() -> {
      Objects.requireNonNull(consumer);
      try (var connection = connectionSupplier.get()) {
        consumer.accept(connection);
      }
    });
  }

  private <T> T wrapConnectionR(Function<Connection, T> function) {
    return unchecked(() -> {
      Objects.requireNonNull(function);
      try (var connection = connectionSupplier.get()) {
        return function.apply(connection);
      }
    });
  }

  private static class RecordMapperImpl<T extends Record> implements RecordMapper<T> {
    private final Class<T> clazz;

    public RecordMapperImpl(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Override
    public T map(ResultSet rs) {
      return Records.fromLookupLambda(clazz, name -> unchecked(() -> rs.getObject(name)));
    }
  }
}
