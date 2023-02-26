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

import com.github.raffaeleragni.jx.exceptions.Exceptions.Wrapper;
import com.github.raffaeleragni.jx.exceptions.Exceptions.WrapperR;

import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;

import java.io.IOException;
import java.net.SocketException;
import java.util.function.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import static com.github.raffaeleragni.jx.exceptions.Exceptions.absorb;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExceptionsTest {
  @Test
  void testNoException_ThrowsNothing_AllSignatures() {
    assertDoesNotThrow(() ->
      unchecked(() -> {}, RuntimeException::new)
    );
    assertDoesNotThrow(() ->
      unchecked(() -> 1, RuntimeException::new)
    );
    assertDoesNotThrow(() ->
      unchecked(() -> {})
    );
    assertDoesNotThrow(() ->
      unchecked(() -> 1)
    );
    assertDoesNotThrow(() ->
      absorb(() -> {})
    );
    assertDoesNotThrow(() ->
      absorb(() -> 1)
    );
  }

  @Test
  void testAbsorbsAllExceptions() {
    var caller = (Wrapper) () -> {throw new RuntimeException();};
    var wrapper = (WrapperR<Integer>) () -> {throw new RuntimeException();};
    assertDoesNotThrow(() -> absorb(caller));
    assertDoesNotThrow(() -> absorb(wrapper));
  }

  @Test
  void testAbsorbWithCallback() {
    var ex = new RuntimeException();
    var call1 = mock(Consumer.class);
    var call2 = mock(Consumer.class);
    var caller = (Wrapper) () -> {throw ex;};
    var wrapper = (WrapperR<Integer>) () -> {throw ex;};
    assertDoesNotThrow(() -> absorb(caller, call1));
    assertDoesNotThrow(() -> absorb(wrapper, call2));

    verify(call1).accept(ex);
    verify(call2).accept(ex);
  }

  @Test
  void testCheckedException_IsThrown_AsProvidedException() {
    Wrapper w = () -> {throw new IOException();};
    var result = assertThrows(RuntimeException.class, () ->
      unchecked(w, IllegalStateException::new)
    );

    assertThat(result.getClass(), is(IllegalStateException.class));
    assertThat(result.getCause().getClass(), is(IOException.class));
  }

  @Test
  void testRuntimeException_IsUsedAsDefault_WhenShortSignature() {
    var result = assertThrows(RuntimeException.class, () ->
      unchecked(() -> {throw new SocketException();})
    );

    assertThat(result.getClass(), is(RuntimeException.class));
    assertThat(result.getCause().getClass(), is(SocketException.class));
  }

  @Test
  void testRuntimeExceptions_AreNotWrapped() {
    var result = assertThrows(IllegalArgumentException.class, () ->
      unchecked(() -> {throw new IllegalArgumentException();})
    );

    assertThat(result.getClass(), is(IllegalArgumentException.class));

    Wrapper w = () -> {throw new IllegalArgumentException();};
    result = assertThrows(IllegalArgumentException.class, () ->
      unchecked(w)
    );

    assertThat(result.getClass(), is(IllegalArgumentException.class));
  }
}
