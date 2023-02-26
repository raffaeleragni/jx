/*
 * Copyright 2020 Raffaele Ragni <raffaele.ragni@gmail.com>.
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
package com.github.raffaeleragni.jx.exceptions;

import java.util.*;
import java.util.function.*;

public final class Exceptions {

  private Exceptions() {}
  private static final Function<Throwable, ? extends RuntimeException> DEFAULT_EXCEPTION_TRANSFORMER = RuntimeException::new;

  public static void absorb(Wrapper w) {
    absorb(w, e -> {});
  }

  public static <T> Optional<T> absorb(WrapperR<T> w) {
    return absorb(w, e -> {});
  }

  public static void absorb(Wrapper w, Consumer<Throwable> exConsumer) {
    try {
      w.run();
    } catch (Exception ex) {
      exConsumer.accept(ex);
    }
  }

  public static <T> Optional<T> absorb(WrapperR<T> w, Consumer<Throwable> exConsumer) {
    try {
      return Optional.of(w.run());
    } catch (Exception ex) {
      exConsumer.accept(ex);
      return Optional.empty();
    }
  }

  public static void unchecked(Wrapper wrapper) {
    unchecked(wrapper, DEFAULT_EXCEPTION_TRANSFORMER);
  }

  public static <T> T unchecked(WrapperR<T> wrapper) {
    return unchecked(wrapper, DEFAULT_EXCEPTION_TRANSFORMER);
  }

  public static void unchecked(Wrapper w, Function<Throwable, ? extends RuntimeException> s) {
    try {
      w.run();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw s.apply(ex);
    }
  }

  public static <T> T unchecked(WrapperR<T> w, Function<Throwable, ? extends RuntimeException> s) {
    try {
      return w.run();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw s.apply(ex);
    }
  }

  public interface Wrapper {
    void run() throws Exception;  // NOSONAR
  }

  public interface WrapperR<T> {
    T run() throws Exception;  // NOSONAR
  }
}
