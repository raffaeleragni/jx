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

import com.github.raffaeleragni.jx.json.Comp1;
import com.github.raffaeleragni.jx.json.Composed;
import static com.github.raffaeleragni.jx.json.JSONBuilder.toJSON;
import com.github.raffaeleragni.jx.json.My;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JSONBuilderTest {
  JSONBuilder jb;

  @BeforeEach
  void setup() {
    jb = new JSONBuilder();
  }

  @Test
  void testEmpty() {
    jb.beginObject();
    jb.endObject();
    assertThat(jb.toString(), is("{}"));
  }

  @Test
  void testObjectProperty() {
    verifyProperty("property\"/</", "{\"property\\\"/<\\/\":{}}");
  }

  @Test
  void testObjectPropertyMoreEscapes() {
    verifyProperty("property\b\t\n\f\r", "{\"property\\b\\t\\n\\f\\r\":{}}");
  }

  @Test
  void testObjectPropertyLowChars() {
    verifyProperty("property\u0010", "{\"property\\u0010\":{}}");
  }

  @Test
  void testObjectPropertyhighChars() {
    verifyProperty("property\u0080\u00a1\u2000\u2101", "{\"property\\u0080\u00a1\\u2000\u2101\":{}}");
  }

  void verifyProperty(String name, String result) {
    jb.beginObject();

    jb.property(name);
    jb.beginObject();
    jb.endObject();

    jb.endObject();

    assertThat(jb.toString(), is(result));
  }

  @Test
  void testDouble() {
    jb.beginObject();

    jb.property("text");
    jb.beginObject();
    jb.endObject();

    jb.property("text2");
    jb.beginObject();
    jb.endObject();

    jb.endObject();
    assertThat(jb.toString(), is("{\"text\":{},\"text2\":{}}"));
  }

  @Test
  void testValues() {
    jb.beginObject();

    jb.property("text");
    jb.value("asd");

    jb.property("text2");
    jb.value(null);

    jb.property("int");
    jb.value(1);

    jb.endObject();
    assertThat(jb.toString(), is("{\"text\":\"asd\",\"text2\":null,\"int\":1}"));
  }

  @Test
  void testArray() {
    jb.beginArray();

    jb.value(1);
    jb.value(3);

    jb.beginObject();
    jb.property("text");
    jb.value("asd");
    jb.endObject();

    jb.endArray();

    assertThat(jb.toString(), is("[1,3,{\"text\":\"asd\"}]"));
  }

  @Test
  void testBolean() {
    jb.value(true);
    assertThat(jb.toString(), is("true"));
  }

  @Test
  void testDate() {
    var now = Instant.now();
    var snow = now.toString();
    jb.value(now);
    assertThat(jb.toString(), is('"' + snow + '"'));
  }

  @Test
  void testList() {
    var list = new LinkedList<Object>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.add(null);
    jb.value(list);
    assertThat(jb.toString(), is("[1,2,3]"));
  }

  @Test
  void testMap() {
    var map = new HashMap<String, Object>();
    map.put("test", "asd");
    map.put("test2", null);
    jb.value(map);
    assertThat(jb.toString(), is("{\"test\":\"asd\"}"));
  }

  @Test
  void testNativeArray() {
    jb.value(new int[]{3, 2, 1});
    assertThat(jb.toString(), is("[3,2,1]"));
  }

  @Test
  void testNativeCharArray() {
    jb.value(new char[]{'a', 'b', 'c'});
    assertThat(jb.toString(), is("[\"a\",\"b\",\"c\"]"));
  }

  @Test
  void testObjectArray() {
    assertThat(toJSON(new Character[]{'a', 'b', 'c', null}), is("[\"a\",\"b\",\"c\"]"));
  }

  @Test
  void testRecord() {
    var rec = new My(1, "c", Map.of("a", "b"), null);
    assertThat(toJSON(rec), is("{\"x\":1,\"a\":\"c\",\"map\":{\"a\":\"b\"}}"));
  }

  @Test
  void testMultiRecord() {
    var rec = new Composed(2, new Comp1(1, "a"));
    assertThat(toJSON(rec), is("{\"id\":2,\"cp\":{\"id\":1,\"name\":\"a\"}}"));
  }

  @Test
  void testOptional() {
    var json = toJSON(Optional.empty());
    assertThat(json, is("null"));
  }

  @Test
  void testPrependCommaToArray() {
    jb.beginArray();

    jb.beginArray();
    jb.endArray();

    jb.beginArray();
    jb.endArray();

    jb.value(1);

    jb.endArray();

    var result = jb.toString();
    assertThat(result, is("[[],[],1]"));
  }

  @Test
  void testTestNullsInArray() {
    jb.value(new Integer[]{1, null, 2});

    var result = jb.toString();
    assertThat(result, is("[1,2]"));
  }

  @ParameterizedTest
  @CsvSource({
    "false,false",
    "false,true",
    "true,true"
  })
  void testComma(boolean comma, boolean prop) {
    jb.comma = comma;
    jb.prop = prop;
    jb.value(1);

    var result = jb.toString();
    assertThat(result, is("1"));
  }
}

record My(int x, String a, Map<String, String> map, String another) {}

record Comp1(int id, String name) {}
record Composed(int id, Comp1 cp) {}
