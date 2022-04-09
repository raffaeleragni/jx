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

import java.util.function.Function;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.compile;

@FunctionalInterface
public interface NameTransformer extends Function<String, String> {
  final NameTransformer NONE = x -> x;
  final NameTransformer SNAKE = new CaseSplitter('_');
  final NameTransformer KEBAB = new CaseSplitter('-');

  @Override
  default String apply(String t) {
    return transform(t);
  }

  String transform(String from);
}

class CaseSplitter implements NameTransformer {

  final Pattern pattern = compile("(?<=[a-z0-9])[A-Z]");
  final char separator;

  public CaseSplitter(char separator) {
    this.separator = separator;
  }

  @Override
  public String transform(String from) {
    if (from == null)
      return null;
    return pattern.matcher(from)
      .replaceAll(match -> separator + match.group().toLowerCase())
      .toLowerCase();
  }
}