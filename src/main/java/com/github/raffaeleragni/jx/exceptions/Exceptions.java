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

import java.util.function.Function;

public final class Exceptions {

  private Exceptions() {}
  private static final Function<Throwable, ? extends RuntimeException> DEFAULT_EXCEPTION_TRANSFORMER = RuntimeException::new;
  
  public static void unchecked(Wrapper wrapper) {
    unchecked(wrapper, DEFAULT_EXCEPTION_TRANSFORMER);
  }
  
  public static <T> T unchecked(WrapperR<T> wrapper) {
    return unchecked(wrapper, DEFAULT_EXCEPTION_TRANSFORMER);
  }

  public static void unchecked(Wrapper w, Function<Throwable, ? extends RuntimeException> s) {
    try {
      w.run();
    } catch (Exception ex) {
      throw s.apply(ex);
    }
  }

  public static <T> T unchecked(WrapperR<T> w, Function<Throwable, ? extends RuntimeException> s) {
    try {
      return w.run();
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
