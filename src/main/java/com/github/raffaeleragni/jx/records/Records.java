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
package com.github.raffaeleragni.jx.records;

import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;
import com.github.raffaeleragni.jx.naming.NameTransformer;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class Records {

  private Records() {
  }

  public static Map<String, Object> toMap(Object rec) {
    Objects.requireNonNull(rec);
    if (!isRecord(rec))
      throw recordRequiredException();

    var result = new HashMap<String, Object>();
    for (var field: getRecordFields(rec)) {
      var value = getFieldValue(field, rec);
      if (isRecord(value))
        result.put(field.getName(), toMap(value));
      else
        result.put(field.getName(), value);
    }

    return result;
  }

  public static <T> T fromMap(Class<T> clazz, Map<String, Object> map) {
    if (map == null)
      return null;
    return fromLookupLambda(clazz, map::get);
  }

  public static <T> T fromLookupLambda(Class<T> clazz, Function<String, Object> fetch) {
    if (fetch == null)
      return null;
    if (!clazz.isRecord())
      throw recordRequiredException();

    var params = new LinkedList<Object>();
    var types = new LinkedList<Class<?>>();
    for (var field: getDeclaredFields(clazz)) {
      var type = field.getType();
      var value = getValueWithNameCases(fetch, field);
      types.add(type);
      if (type.isRecord() && value instanceof Map m)  // NOSONAR
        params.add(fromMap(type, m));
      else if (type.isEnum())
        params.add(fromEnum(type, value));
      else
        params.add(value);
    }

    var constructor = unchecked(() -> clazz.getDeclaredConstructor(types.toArray(new Class[]{})));

    return unchecked(() -> constructor.newInstance(params.toArray()));
  }

  private static Object fromEnum(Class<?> type, Object value) {
    return Arrays.stream(type.getEnumConstants())
      .filter(e -> e.toString().equals(value.toString()))
      .findAny()
      .orElse(null);
  }

  private static Object getValueWithNameCases(Function<String, Object> fetch, RecordComponent e) {
    var value = fetch.apply(e.getName());
    if (value != null)
      return value;

    var snakeName = NameTransformer.SNAKE.apply(e.getName());
    value = fetch.apply(snakeName.toLowerCase());
    if (value != null)
      return value;

    value = fetch.apply(snakeName.toUpperCase());
    if (value != null)
      return value;

    var kebabName = NameTransformer.KEBAB.apply(e.getName());
    value = fetch.apply(kebabName.toLowerCase());
    if (value != null)
      return value;

    value = fetch.apply(kebabName.toUpperCase());

    return value;
  }

  private static boolean isRecord(Object value) {
    return value.getClass().isRecord();
  }

  private static RecordComponent[] getRecordFields(Object rec) {
    return getDeclaredFields(rec.getClass());
  }

  private static Object getFieldValue(RecordComponent field, Object rec) {
    return unchecked(() -> field.getAccessor().invoke(rec));
  }

  private static RecordComponent[] getDeclaredFields(Class<?> clazz) {
    return clazz.getRecordComponents();
  }

  public static class RecordRequired extends RuntimeException {}

  private static RuntimeException recordRequiredException() {
    return new RecordRequired();
  }
}
