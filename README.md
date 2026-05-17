# JVM flags

Interactive website about JVM flags by Java version.

## Run locally

```bash
jwebserver -d docs -p 8080
./mvnw clean install -Psync-docs-json
```

## References

Flag names, categories, and version-specific availability in [`docs/json/`](docs/json/) are based on Oracle’s official HotSpot documentation for each release below.

### Java 8

- [The `java` command](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html) — standard, non-standard (`-X`), and advanced (`-XX`) options
- [Java SE HotSpot Virtual Machine Garbage Collection Tuning Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/)
- [Troubleshooting: Command-line options](https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/clopts.html)

### Java 11

- [The `java` command](https://docs.oracle.com/en/java/javase/11/docs/specs/man/java.html) — standard, extra, advanced runtime, JIT, serviceability, and GC options; unified logging (`-Xlog`)
- [Java Platform, Standard Edition HotSpot Virtual Machine Garbage Collection Tuning Guide](https://docs.oracle.com/en/java/javase/11/gctuning/)
- [Troubleshooting: Command-line options](https://docs.oracle.com/en/java/javase/11/troubleshoot/command-line-options1.html)

### Java 17

- [The `java` command](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html)
- [Java Platform, Standard Edition HotSpot Virtual Machine Garbage Collection Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [Troubleshooting: Command-line options](https://docs.oracle.com/en/java/javase/17/troubleshoot/command-line-options1.html)

### Java 21

- [The `java` command](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html)
- [Java Platform, Standard Edition HotSpot Virtual Machine Garbage Collection Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [Troubleshooting: Command-line options](https://docs.oracle.com/en/java/javase/21/troubleshoot/command-line-options1.html)

### Java 25

- [The `java` command](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html)
- [Java Platform, Standard Edition HotSpot Virtual Machine Garbage Collection Tuning Guide](https://docs.oracle.com/en/java/javase/25/gctuning/)
- [Troubleshooting: Command-line options](https://docs.oracle.com/en/java/javase/25/troubleshoot/command-line-options1.html)
