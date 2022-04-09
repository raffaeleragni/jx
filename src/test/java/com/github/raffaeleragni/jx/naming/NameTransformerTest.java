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
package com.github.raffaeleragni.jx.naming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NameTransformerTest {

  @Test
  void testNullInputs_ReturnNullOutputs() {
    assertThat(NameTransformer.NONE.transform(null), is(nullValue()));
    assertThat(NameTransformer.SNAKE.transform(null), is(nullValue()));
    assertThat(NameTransformer.KEBAB.transform(null), is(nullValue()));
  }

  @ParameterizedTest
  @CsvSource(value = {
    ",",
    "a,a",
    "aA,aA",
    "wordWithAnotherWord,wordWithAnotherWord",
    "snake_case_words,snake_case_words",
    "kebab-case-words,kebab-case-words",
  })
  void testTransformerNone_DoesNotAlterInputs(String from, String to) {
    assertTransformed(NameTransformer.NONE, from, to);
  }

  @ParameterizedTest
  @CsvSource(value = {
    "a,a",
    "A,a",
    "aA,a_a",
    "wordWithAnotherWord,word_with_another_word",
    "word2Numbers,word2_numbers",
    "word2numbers,word2numbers"
  })
  void testTramsformerSnake_ConvertsFromCamelCaseToSnake(String from, String to) {
    assertTransformed(NameTransformer.SNAKE, from, to);
  }

  @ParameterizedTest
  @CsvSource(value = {
    "a,a",
    "A,a",
    "aA,a-a",
    "wordWithAnotherWord,word-with-another-word",
    "word2Numbers,word2-numbers",
    "word2numbers,word2numbers"
  })
  void testTransformerKebab_ConvertsFromCamelCaseToKebab(String from, String to) {
    assertTransformed(NameTransformer.KEBAB, from, to);
  }

  private void assertTransformed(NameTransformer transformer, String from, String to) {
    assertThat(transformer.transform(from), is(to));
    assertThat(transformer.apply(from), is(to));
  }
}
