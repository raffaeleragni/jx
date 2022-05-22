/*
 * Copyright 2022 Raf.
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
package com.github.raffaeleragni.jx.injection;

import com.github.raffaeleragni.jx.collections.ClassMap;
import static com.github.raffaeleragni.jx.exceptions.Exceptions.unchecked;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Injection {
  public static class AtLeastOneConstructorNecessary extends RuntimeException {}
  public static class OnlyOneConstructorAllowed extends RuntimeException {}

  private final ClassMap instances = new ClassMap();
  private final Map<Class, Class> implementations = new HashMap<>();  // NOSONAR

  public <T> void addInstance(Class<T> clazz, T object) {
    instances.put(clazz, object);
  }

  public <I, T extends I> void addImplementation(Class<I> interf, Class<T> clazz) {
    implementations.put(interf, clazz);
  }

  public <T> T createNew(Class<T> clazz) {
    return unchecked(() -> newAndInjectE(clazz));
  }

  private <T> T newAndInjectE(Class<T> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    var constructor = findConstructor(clazz);
    var parameters = findMatchingParameters(constructor);
    return constructor.newInstance(parameters);
  }

  private <T> Constructor<T> findConstructor(Class<T> clazz) throws SecurityException {
    allowOnlyOneConstructor(clazz);
    var constructor = clazz.getDeclaredConstructors()[0];
    constructor.setAccessible(true);  // NOSONAR
    return (Constructor<T>) constructor;
  }

  private <T> void allowOnlyOneConstructor(Class<T> clazz) throws OnlyOneConstructorAllowed, AtLeastOneConstructorNecessary {
    var constructors = clazz.getDeclaredConstructors();
    if (constructors.length == 0)
      throw new AtLeastOneConstructorNecessary();
    if (constructors.length > 1)
      throw new OnlyOneConstructorAllowed();
  }

  private <T> Object[] findMatchingParameters(Constructor<T> constructor) {
    var parameterTypes = constructor.getParameterTypes();
    Object[] parameters = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++)
      parameters[i] = lookupValueOrType(parameterTypes[i]);
    return parameters;
  }

  private <T> T lookupValueOrType(Class<T> type) {
    if (instances.containsKey(type))
      return (T) instances.get(type);
    if (implementations.containsKey(type))
      return (T) createNew(implementations.get(type));
    return createNew(type);
  }
}
