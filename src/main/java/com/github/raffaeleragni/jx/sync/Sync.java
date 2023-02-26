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

import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;

public final class Sync {
  private static final long DEFAULT_WAIT_MS = 10_000;
  private static final int DEFAULT_INTERVAL_CHECK = 50;

  private Sync(){}

  public static boolean waitForNotNull(Supplier<?> getter) {
    return waitForNotNull(getter, DEFAULT_WAIT_MS);
  }

  public static boolean waitForNotNull(Supplier<?> getter, long maxWaitMillis) {
    return waitForNot(() -> getter.get() == null, maxWaitMillis);
  }

  public static boolean waitForNot(BooleanSupplier checker) {
    return waitForNot(checker, DEFAULT_WAIT_MS);
  }

  public static boolean waitForNot(BooleanSupplier checker, long maxWaitMillis) {
    return waitFor(() -> !checker.getAsBoolean(), maxWaitMillis);
  }

  public static boolean waitFor(BooleanSupplier condition) {
    return waitFor(condition, DEFAULT_WAIT_MS);
  }

  public static boolean waitFor(BooleanSupplier condition, long maxWaitMillis) {
    long maxCounts = maxWaitMillis / DEFAULT_INTERVAL_CHECK;
    long count = 0;
    var result = condition.getAsBoolean();
    while (!result && count < maxCounts) {
      sleep(DEFAULT_INTERVAL_CHECK);
      count++;
      result = condition.getAsBoolean();
    }
    return result;
  }

  private static void sleep(long ms) {
    unchecked(() -> Thread.sleep(ms)); //NOSONAR
  }
}
