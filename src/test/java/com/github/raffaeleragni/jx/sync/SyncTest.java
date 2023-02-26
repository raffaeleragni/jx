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
package com.github.raffaeleragni.jx.sync;

import java.util.function.*;
import org.junit.jupiter.api.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class SyncTest {

  @Test
  void testNoWaitIfAlreadyTrue() {
    var result = Sync.waitFor(() -> true);
    assertThat(result, is(true));

    result = Sync.waitForNot(() -> false);
    assertThat(result, is(true));
  }

  @Test
  void testFinallyReturnsFalseIfIsAlwaysFalse() {
    var result = Sync.waitForNotNull(() -> null, 1);
    assertThat(result, is(false));

    result = Sync.waitFor(() -> false, 1);
    assertThat(result, is(false));
  }

  @Test
  void testWaitForNotNull() {
    var result = Sync.waitForNotNull(() -> true);
    assertThat(result, is(true));
  }

  @Test
  void testReturnsTrueWhenItFlips() {
    var result = Sync.waitFor(FLIP_AFTER_FIRST_GET);
    assertThat(result, is(true));
  }

  @Test
  void testReturnsWhenFlippingAfter100ms() {
    var result = Sync.waitFor(FLIP_AFTER_100MS);
    assertThat(result, is(true));
  }

  private static BooleanSupplier FLIP_AFTER_FIRST_GET = new BooleanSupplier() {
    boolean value = false;
    @Override
    public boolean getAsBoolean() {
      var result = value;
      value = true;
      return result;
    }
  };

  private static BooleanSupplier FLIP_AFTER_100MS = new BooleanSupplier() {
    long ms = -1;
    boolean value = false;
    @Override
    public boolean getAsBoolean() {
      var result = value;
      if (ms == -1)
        ms = System.currentTimeMillis();
      if (System.currentTimeMillis() - ms > 100)
        value = true;
      return result;
    }
  };
}
