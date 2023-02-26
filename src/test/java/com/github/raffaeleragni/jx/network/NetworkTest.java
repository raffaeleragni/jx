/*
 * Copyright 2023 Raffaele Ragni.
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
package com.github.raffaeleragni.jx.network;

import java.net.*;
import org.junit.jupiter.api.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class NetworkTest {
  @Test
  void testPortAvailable() {
    var result = Network.isPortAvailable(5000);
    assertThat(result, is(true));
  }

  @Test
  void testPortUnavailable() throws Exception {
    try (var ignored = new ServerSocket(5000)) { //NOSONAR
      var result = Network.isPortAvailable(5000);
      assertThat(result, is(false));
    }
  }

  @Test
  void testFindNextPortWhenFree() {
    var result = Network.findAvailablePortFrom(5000);
    assertThat(result, is(5000));
    result = Network.findAvailablePortFrom(4000);
    assertThat(result, is(4000));
  }

  @Test
  void testFindNextPortWhenNextOneOnlyIsOccupied() throws Exception {
    try (var ignored = new ServerSocket(5000)) { //NOSONAR
      var result = Network.findAvailablePortFrom(5000);
      assertThat(result, is(5001));
    }
  }

  @Test
  void testFindNextPortWhenSuccessivelyOccupied() throws Exception {
    try (var ignored1 = new ServerSocket(5000);
         var ignored2 = new ServerSocket(5001);
         var ignored3 = new ServerSocket(5002);
         var ignored4 = new ServerSocket(5003)) { //NOSONAR

      var result = Network.findAvailablePortFrom(5000);
      assertThat(result, is(5004));
    }
  }
}
