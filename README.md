# JX: Java Extended

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=raffaeleragni_jx&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=raffaeleragni_jx)

Importing library:
```maven
<dependency>
  <groupId>com.github.raffaeleragni</groupId>
  <artifactId>jx</artifactId>
  <version>0.1</version>
</dependency>
```

## 0.1

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
