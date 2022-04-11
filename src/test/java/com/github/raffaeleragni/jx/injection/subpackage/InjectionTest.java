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
package com.github.raffaeleragni.jx.injection.subpackage;

import com.github.raffaeleragni.jx.injection.Injection;
import com.github.raffaeleragni.jx.injection.Injection.AtLeastOneConstructorNecessary;
import com.github.raffaeleragni.jx.injection.Injection.OnlyOneConstructorAllowed;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InjectionTest {
  Injection injection;

  @BeforeEach
  void setup() {
    injection = new Injection();
  }

  @Test
  void testInjectingValues() {
    injection.addInstance(String.class, "string");
    var service = injection.createNew(Service.class);

    assertThat(service.string, is("string"));
  }

  @Test
  void testCreatingAndInjecting() {
    injection.addInstance(String.class, "string");
    var service = injection.createNew(ServiceWithDependency.class);

    assertThat(service.service.getString(), is("string"));
  }

  @Test
  void testCreatingAndInjectingThroughInterface() {
    injection.addInstance(String.class, "string");
    injection.addImplementation(ServiceInterface.class, Service.class);
    var service = injection.createNew(ServiceWithDependencyAsInterface.class);

    assertThat(service.service.getString(), is("string"));
  }

  @Test
  void testOnlyOneConstructorAllowed() {
    assertThrows(OnlyOneConstructorAllowed.class, () -> injection.createNew(TwoConstructors.class));
  }

  @Test
  void testAtLeastOneConstructorRequired() {
    assertThrows(AtLeastOneConstructorNecessary.class, () -> injection.createNew(ServiceInterface.class));
  }
}

interface ServiceInterface {
  String getString();
}

class Service implements ServiceInterface{
  String string;

  Service(String string) {
    this.string = string;
  }

  @Override
  public String getString() {
    return string;
  }
}

class ServiceWithDependency {
  Service service;

  ServiceWithDependency(Service service) {
    this.service = service;
  }
}

class ServiceWithDependencyAsInterface {
  ServiceInterface service;

  ServiceWithDependencyAsInterface(ServiceInterface service) {
    this.service = service;
  }
}

class TwoConstructors {

  TwoConstructors() {
  }

  TwoConstructors(String string) {
  }
}
