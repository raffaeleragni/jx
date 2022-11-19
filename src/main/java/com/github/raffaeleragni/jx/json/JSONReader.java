/*
 * Copyright 2022 Raffaele Ragni <raffaele.ragni@gmail.com>.
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

package com.github.raffaeleragni.jx.json;

import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;
import com.github.raffaeleragni.jx.records.Records;
import java.io.Reader;
import java.io.StringReader;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Character.isWhitespace;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * JSON reader, it wraps around an actual reader and uses constant memory within
 * reasonable limits.
 * Some string buffering may be involved to interpret literals.
 *
 * toObject() will return an object representation by mapping:
 *   "..." -> String
 *   [...] -> List
 *   {...} -> Map where keys are String
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class JSONReader implements AutoCloseable {

  final Reader reader;
  public JSONReader(final Reader reader) {
    this.reader = reader;
  }

  @Override
  public void close() {
    unchecked(reader::close);
  }

  public static Object toObject(final String string) {
    return new JSONReader(new StringReader(string)).toObject();
  }

  public static <T> T toRecord(Class<T> clazz, final String string) {
    return new JSONReader(new StringReader(string)).toRecord(clazz);
  }

  public static <T> List<T> toRecordList(Class<T> clazz, final String string) {
    return new JSONReader(new StringReader(string)).toRecordList(clazz);
  }

  public Object toObject() {
    try {
      return readItem(null);
    } finally {
      unchecked(reader::close);
    }
  }

  public <T> T toRecord(Class<T> clazz) {
    try {
      if (isNotRecord(clazz))
        throw recordRequiredException();

      if (!isObjectStart(nextNonWhitespaceChar()))
        throw invalidJSONException();

      return Records.fromMap(clazz, toMap());
    } finally {
      unchecked(reader::close);
    }
  }

  public <T> List<T> toRecordList(Class<T> clazz) {
    try {
      if (isNotRecord(clazz))
        throw recordRequiredException();

      var ch = nextNonWhitespaceChar();
      if (!isArrayStart(ch))
        return null;//NOSONAR

      var list = new LinkedList<T>();
      walkArray(o -> {
        var map = (Map<String, Object>) o;
        if (isEmptyMap(map))
          return;
        list.add(Records.fromMap(clazz, map));
      });

      return list;
    } finally {
      unchecked(reader::close);
    }
  }

  private Object readItem(Integer prev) {
    var ch = prev != null ? prev : nextNonWhitespaceChar();
    return switch (ch) {
      case '"' -> readString();
      case '[' -> toArray();
      case '{' -> toMap();
      default -> readLiteral(ch);
    };
  }

  private int nextNonWhitespaceChar() {
    var ch = nextChar();
    while (isWhitespace(ch))
      ch = nextChar();
    return ch;
  }

  private int nextChar() {
    return unchecked(() -> reader.read()); //NOSONAR
  }

  private Object readLiteral(int ch) {
    var s = nextLiteralString(ch);
    if ("null".equalsIgnoreCase(s))
      return null;
    if ("true".equalsIgnoreCase(s))
      return TRUE;
    if ("false".equalsIgnoreCase(s))
      return FALSE;

    try {
      return Integer.valueOf(s);
    } catch (NumberFormatException e) {} //NOSONAR

    try {
      return Long.valueOf(s);
    } catch (NumberFormatException e) {} //NOSONAR

    try {
      return new BigDecimal(s);
    } catch (NumberFormatException e) {} //NOSONAR

    return null;
  }

  private String nextLiteralString(int ch) {
    var builder = new StringBuilder();
    while (literalContinues(ch)) {
      builder.append((char) ch);
      ch = nextNonWhitespaceChar();
    }
    return builder.toString();
  }

  private static boolean literalContinues(int ch) {
    if (isEndOfStream(ch))
      return false;
    if (isComma(ch))
      return false;
    if (isObjectEnd(ch))
      return false;
    return !isArrayEnd(ch);
  }

  private String readString() {
    var builder = new StringBuilder();
    var prev = 0;
    var ch = nextChar();
    while (stringContinues(ch, prev)) {
      if (!isEscape(ch))
        builder.append((char) ch);
      prev = ch;
      ch = nextChar();
    }
    if (!isQuotes(ch))
      throw invalidJSONException();
    return builder.toString();
  }

  private static boolean stringContinues(int ch, int prev) {
    if (isEndOfStream(ch))
      return false;
    return isNormalEscapedCharacter(ch, prev);
  }

  private static boolean isNormalEscapedCharacter(int ch, int prev) {
    if (isEscape(prev))
      return true;
    return !isQuotes(ch);
  }

  private List toArray() { //NOSONAR
    var list = new LinkedList<Object>();
    walkArray(list::add);
    return list;
  }

  private Map<String, Object> toMap() {
    var map = new HashMap<String, Object>();
    walkObject(map::put);
    return map;
  }

  private void walkArray(Consumer<Object> fn) {
    var ch = nextChar();
    while (!isEndOfStream(ch) && ch != ']') {
      fn.accept(readItem(ch));
      ch = nextNonWhitespaceChar();
      if (ch == ',')
        ch = nextNonWhitespaceChar();
    }
  }

  private void walkObject(BiConsumer<String, Object> fn) {
    var ch = nextNonWhitespaceChar();
    while (!isEndOfStream(ch) && !isObjectEnd(ch)) { //NOSONAR
      // Need a property to start. All properties are quoted.
      if (!isQuotes(ch))
        throw invalidJSONException();

      var prop = readString();
      ch = nextNonWhitespaceChar();
      if (!isPropertySeparator(ch))
        throw invalidJSONException();

      fn.accept(prop, readItem(null));

      ch = nextNonWhitespaceChar();
      if (isComma(ch))
        ch = nextNonWhitespaceChar();
    }
  }

  static boolean isNotRecord(Class<?> clazz) {
    return clazz == null || !clazz.isRecord();
  }

  static boolean isEmptyMap(Map<String, Object> map) {
    return map == null || map.isEmpty();
  }

  public static class NotAJSON extends RuntimeException {}
  public static class NotARecord extends RuntimeException {}

  private static RuntimeException invalidJSONException() {
    return new NotAJSON();
  }

  private static RuntimeException recordRequiredException() {
    return new NotARecord();
  }

  private static boolean isEndOfStream(int ch) {
    return ch == -1;
  }

  private static boolean isQuotes(int ch) {
    return ch == '"';
  }

  private static boolean isEscape(int ch) {
    return ch == '\\';
  }

  private static boolean isComma(int ch) {
    return ch == ',';
  }

  private static boolean isObjectStart(int ch) {
    return ch == '{';
  }

  private static boolean isArrayStart(int ch) {
    return ch == '[';
  }

  private static boolean isObjectEnd(int ch) {
    return ch == '}';
  }

  private static boolean isArrayEnd(int ch) {
    return ch == ']';
  }

  private static boolean isPropertySeparator(int ch) {
    return ch == ':';
  }
}
