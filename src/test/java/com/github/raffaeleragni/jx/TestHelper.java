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
package com.github.raffaeleragni.jx;

import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.function.Consumer;

public final class TestHelper {
  private TestHelper() {}

  public static boolean isPortOccupied(int port) {
    try (var sock = new Socket("127.0.0.1", port)) {
      return true;
    } catch (IOException | RuntimeException ex) {
      return false;
    }
  }

  public static Connection memoryDB(String name) {
    return unchecked(() ->
      DriverManager.getConnection("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "")
    );
  }

  public static int sql(Connection connection, String sql) {
    return unchecked(() -> {
      try ( var st = connection.prepareStatement(sql)) {
        return st.executeUpdate();
      }
    });
  }

  public static void sql(Connection connection, String sql, Consumer<ResultSet> consumer) {
    unchecked(() -> {
      try ( var st = connection.prepareStatement(sql)) {
        try ( var rs = st.executeQuery()) {
          while (rs.next()) {
            consumer.accept(rs);
          }
        }
      }
    });
  }
}
