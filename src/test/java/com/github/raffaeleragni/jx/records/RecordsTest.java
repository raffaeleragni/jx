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

import static com.github.raffaeleragni.jx.records.Records.fromMap;
import static com.github.raffaeleragni.jx.records.RecordsTest.RecordEnum.NUM1;
import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecordsTest {
  public record Sample(int id, String name) {}
  public record Nested(boolean visible, Sample sample) {}
  public record SampleMoreNames(int id, String nameDifferent) {}
  public enum RecordEnum {NUM1, NUM2}
  public record RecordWithEnum(RecordEnum enu){}

  @Test
  void testToMap_WithSomethingThatIsNotAMap_ThrowsException() {
    var o = new Object();
    assertThrows(Records.RecordRequired.class, () -> {
      Records.toMap(o);
    });
  }

  @Test
  void testToMap_WithASimpleRecord() {
    var rec = new Sample(1, "test");
    var map = Records.toMap(rec);
    assertThat(map, is(Map.of("id", 1, "name", "test")));
  }

  @Test
  void testToMap_WithANestedRecord() {
    var rec = new Sample(1, "test");
    var nest = new Nested(true, rec);
    var map = Records.toMap(nest);
    assertThat(map, is(Map.of("visible", true, "sample", Map.of("id", 1, "name", "test"))));
  }

  @Test
  void testFromMap_WithNullInput_OutputsNull() {
    var rec = fromMap(Sample.class, null);
    assertThat(rec, is(nullValue()));
  }

  @Test
  void testFromMap_WithSomethingThatIsNotARecord() {
    var map = new HashMap<String, Object>();
    assertThrows(Records.RecordRequired.class, () -> fromMap(Object.class, map));
  }

  @Test
  void testFromMap_WithASimpleRecord() {
    var rec = Records.fromMap(Sample.class, Map.of("id", 1, "name", "test"));
    var expected = new Sample(1, "test");
    assertThat(rec, is(expected));
  }

  @Test
  void testFromMap_WithANestedRecord() {
    var map = Map.of("visible", true, "sample", Map.of("id", 1, "name", "test"));
    var rec = new Sample(1, "test");
    var expected = new Nested(true, rec);

    assertThat(Records.fromMap(Nested.class, map), is(expected));
  }

  @Test
  void testFromMap_WithANestedRecord_ButInputWithoutANestedMap_ThrowsIllegalArgument() {
    Map<String, Object> map = Map.of("visible", true, "sample", "value");

    assertThrows(RuntimeException.class, () -> Records.fromMap(Nested.class, map));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "nameDifferent",
    "name_different",
    "NAME_DIFFERENT",
    "name-different",
    "NAME-DIFFERENT"})
  void testFromMap_WithDifferentNamingConventionsOnKeys(String name) {
    var rec = Records.fromMap(SampleMoreNames.class, Map.of("id", 1, name, "test"));
    var expected = new SampleMoreNames(1, "test");
    assertThat(rec, is(expected));
  }

  @Test
  void testFromLookupLambda_WithNullInput() {
    var result = Records.fromLookupLambda(SampleMoreNames.class, null);
    assertThat(result, is(nullValue()));
  }

  @Test
  void testFromMap_WithARecordHavingEnums() {
    var result = Records.fromMap(RecordWithEnum.class, Map.of("enu", NUM1));
    assertThat(result.enu(), is(NUM1));
  }
}
