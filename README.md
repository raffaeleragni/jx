# JX: Extending the JDK

[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=bugs)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=coverage)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)


Required: latest GA JDK version.

Importing library:
```maven
<dependency>
  <groupId>com.github.raffaeleragni</groupId>
  <artifactId>jx</artifactId>
  <version>0.16</version>
</dependency>
```

## Sync and network patterns

Useful for condition blockers or for some testing, or to find the next available port.
```java
var port = Network.findAvailablePortFrom(3000);
// port will be the first non-occupied port available after and including 3000
```

```java
Sync.waitForNotNull(SINGLETON::getSlowInitState);
Sync.waitForNot(() -> Network.isPortAvailable(3000));
```

## HTTP `Client`

A shortcut for the internal JDK HttpClient.
```java
var client = new Client();
var resp1 = client.getString("http://localhost:8080/");
var resp2 = client.postString("http://localhost:8080/", "{}");
```

## HTTP `Server`

A minimal http server using the internal JDK implemenation. Only HTTP 1.1 and no encryption supported.

```java
var server = new Server(8080);
// or bind only to localhost
var server = new Server("localhost", 8080);
server.map("/", ctx -> "hello world");
server.map("/echo", Context::request);

// Or using more of the features
record Rec(String id, String value) {}
var store = new FSKV<Rec>(Path.of("..."), Rec.class);
server.map("/", ctx -> {
  var rec = JSONReader.toRecord(Rec.class, ctx.request());
  store.put(rec.id(), rec);
});
```

## `FSKV`

A basic key-value storage for saving into files. Uses internal JX JSON implementation.

```java
public record Rec(String uuid, String name) {}
var store = new FSKV<>(Path.of(System.getProperty("java.io.tmpdir"), "storetest"), TestRecordForFSKV.class);
var rec = new Rec(UUID.randomUUID().toString(), "test");
store.put(rec.uuid(), rec);
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


## `Injection`

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

var injection = new Injection();
injection.addImplementation(Service.class, ServiceImpl.class);
var manager = injection.createNew(ServiceManager.class);
```

## `Jdbc`

`Jdbc` helper class for taking advantage of the utils explained below. Examples:

```java
public record Rec(int id, Instant timestamp, String value) {}

var mapper = Jdbc.recordMapperOf(Rec.class);
var result = new LinkedList<Rec>();
jdbc.streamed("select * from table", st -> {}, rs -> result.add(mapper.map(rs)));

var result = jdbc.streamRecords(Table.class, "select * from test where name = ?", "test1").toList();
```

## `Records`

`Records` helper class. Examples:

```java
record MyRecord(int a, String b);

var map = Map.of("a", 1, "b", "x");
var record = Records.fromMap(MyRecord.class, map);
```

## General

The following ones are mostly required internally, but they are also publicly available.

#### `ClassMap`

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

#### `Exceptions`

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
