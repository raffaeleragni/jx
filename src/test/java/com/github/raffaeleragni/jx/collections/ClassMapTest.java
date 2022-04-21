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
package com.github.raffaeleragni.jx.collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassMapTest {
  ClassMap map;

  @BeforeEach
  void setup() {
    map = new ClassMap();
  }

  @Test
  void testNewMap_IsEmpty() {
    assertThat(map.size(), is(0));
    assertThat(map.isEmpty(), is(true));
  }

  @Test
  void testMapWithValue_IsNotEmpty() {
    map.put(Object.class, new Object());
    assertThat(map.size(), is(1));
    assertThat(map.isEmpty(), is(false));
  }

  @Test
  void testMapWithTwoValues_HasSize2() {
    map.put(Integer.class, 1);
    map.put(String.class, "1");
    assertThat(map.size(), is(2));
    assertThat(map.isEmpty(), is(false));
  }

  @Test
  void testMapWithTwoValues_RemoveOne_HasSize1() {
    map.put(Integer.class, 1);
    map.put(String.class, "1");
    map.removeKey(String.class);
    assertThat(map.size(), is(1));
    assertThat(map.isEmpty(), is(false));
  }

  @Test
  void testMapWithTwoValues_RemoveTwoe_IsEmpty() {
    map.put(Integer.class, 1);
    map.put(String.class, "1");
    map.removeKey(Integer.class);
    map.removeKey(String.class);
    assertThat(map.size(), is(0));
    assertThat(map.isEmpty(), is(true));
  }

  @Test
  void testMapContainsOnlyAddedKey() {
    map.put(Integer.class, 1);
    assertThat(map.containsKey(Integer.class), is(true));
    assertThat(map.containsKey(String.class), is(false));
  }

  @Test
  void testMapNotContainsNotInsertedKey() {
    assertThat(map.containsKey(Integer.class), is(false));
    assertThat(map.containsKey(String.class), is(false));
  }

  @Test
  void testClearingTheMap_MakesItEmpty() {
    map.put(Integer.class, 1);
    map.clear();
    assertThat(map.size(), is(0));
    assertThat(map.isEmpty(), is(true));
  }

  @Test
  void testGetStoredItems() {
    map.put(Integer.class, 1);
    map.put(String.class, "1");
    assertThat(map.get(Integer.class), is(1));
    assertThat(map.get(String.class), is("1"));
  }

  @Test
  void testAddItemUsingImplicitClassType() {
    map.add(1);
    map.add(1L);
    map.add("1");
    assertThat(map.get(Integer.class), is(1));
    assertThat(map.get(Long.class), is(1L));
    assertThat(map.get(String.class), is("1"));
  }

  @Test
  void testNullValuesOrKeys_WillNotBeAccepted() {
    assertThrows(RuntimeException.class, () -> map.put(null, null));
    assertThrows(RuntimeException.class, () -> map.put(String.class, null));
    assertThrows(RuntimeException.class, () -> map.add(null));
    assertThrows(RuntimeException.class, () -> map.get(null));
    assertThrows(RuntimeException.class, () -> map.removeKey(null));
    assertThrows(RuntimeException.class, () -> map.removeValue(null));
    assertThrows(RuntimeException.class, () -> map.containsKey(null));
    assertThrows(RuntimeException.class, () -> map.containsValue(null));
  }

  @Test
  void testContainsValue() {
    map.add(1);
    map.add(1L);
    map.add("1");
    assertThat(map.containsValue(1), is(true));
    assertThat(map.containsValue(1L), is(true));
    assertThat(map.containsValue("1"), is(true));
    assertThat(map.containsValue(1d), is(false));
    assertThat(map.containsValue(3), is(false));
  }

  @Test
  void testRemoveValue() {
    map.add(1);
    map.removeValue(1);
    assertThat(map.size(), is(0));
    assertThat(map.isEmpty(), is(true));
  }
}
