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

import java.util.HashMap;
import java.util.Map;
import static java.util.Objects.requireNonNull;

public class ClassMap {
  private final Map<Class, Object> classes = new HashMap<>();  // NOSONAR

  public boolean isEmpty() {
    return classes.isEmpty();
  }

  public int size() {
    return classes.size();
  }

  public <T> void add(T value) {
    requireNonNull(value);
    classes.put(value.getClass(), value);
  }

  public <T> void put(Class<T> key, T value) {
    requireNonNull(key);
    requireNonNull(value);
    classes.put(key, value);
  }

  public <T> T get(Class<T> key) {
    requireNonNull(key);
    return (T) classes.get(key);
  }

  public <T> void removeKey(Class<T> key) {
    requireNonNull(key);
    classes.remove(key);
  }

  public <T> void removeValue(T value) {
    requireNonNull(value);
    classes.remove(value.getClass());
  }

  public <T> boolean containsKey(Class<T> key) {
    requireNonNull(key);
    return classes.containsKey(key);
  }

  public <T> boolean containsValue(T value) {
    requireNonNull(value);
    return classes.containsValue(value);
  }

  public void clear() {
    classes.clear();
  }
}
