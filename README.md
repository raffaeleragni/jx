# JX: Java Extended

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)

Required: latest GA JDK version.

Importing library:
```maven
<dependency>
  <groupId>com.github.raffaeleragni</groupId>
  <artifactId>jx</artifactId>
  <version>0.10</version>
</dependency>
```

## JSON

### `JSONReader`

Based on a reader interface, it will parse a JSON into an object, array or hashmap. Optionally there is also a shortcut for using the `Records` of JX.
As it is based on a reader interface, it can read streams of data.

```java
record TestRecord(int id, String value) {}
var s = """ {"id": 1, "value": "val"} """;
var rec = new JSONReader(new StringReader(s)).toRecord(TestRecord.class);
// Shorthands for strings are also available
var rec = JSONReader.toRecord(TestRecord.class, s);
```

### `JSONBuilder`

Based on a `StringBuilder`, will output a JSON. As it is based on `StringBuilder` it will use minimal overhead. Can serialize records directly using the `Records` of JX.

```java
record TestRecord(int id, String value) {}
var rec = new TestRecord(1, "val");
var s = JSONBuilder.toJSON(rec);
```

output of `s`:

```json
{"id":1,"value":"val"}
```


## Injection

Simple injection via constructor. Example:

```java
interface Service {}
class ServiceImpl implements Service {}
class ServiceManager {
  Service service;
  ServiceManager(Service service) {
    this.service = service;
  }
}
...

var injection = new Injection()
injection.addImplementation(Service.class, ServiceImpl.class)
var manager = injection.createNew(ServiceManager.class);
```

## Jdbc

`Jdbc` helper class for taking advantage of the utils explained below. Examples:

```java
public record Rec(int id, Instant timestamp, String value) {}

var mapper = Jdbc.recordMapperOf(Rec.class);
var result = new LinkedList<Rec>();
jdbc.streamed("select * from table", st -> {}, rs -> result.add(mapper.map(rs)));

var result = jdbc.streamRecords(Table.class, "select * from test where name = ?", "test1").toList();
```

## Records

`Records` helper class. Examples:

```java
record MyRecord(int a, String b);

var map = Map.of("a", 1, "b", "x");
var record = Records.fromMap(MyRecord.class, map);
```

## General

`ClassMap` a simple hash map that handles class types as keys.

```java
var map = new ClassMap();
map.put(Integer.class, 1);
map.put("2");

map.get(Integer.class) --> 1
map.get(String.class) --> "1"
```

`NameTransformer.<type>.transform(from)` transforms name cases from camel case. Example:

```java
var output = NameTransformer.SNAKE.transform("iActuallyNeedSnakeCase");
```
`output` will be `"i_actually_need_snake_case"`


`Exceptions.unckeched(...)` to wrap checked exceptions into unchecked ones. Example:

```java
try {
  unchecked(() -> {
    ...
    throw new IOException("file not found");
  });
} catch (RuntimeException e) { ... }
```

Alternatively with supplier for custom ones:

```java
try {
  unchecked(() -> {
    ...
    throw new IOException("file not found");
  }, ApplicationException::new);
} catch (ApplicationException e) { ... }
```
