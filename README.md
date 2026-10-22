# Trails PDF Printer

`io.github.yaforster:trails-printer-pdf` implements the Trails SDK's `PrintService` to render persisted test paths,
browser results, and complete test runs as PDF reports, including available action screenshots.

## Customer wrapper

Import `io.github.yaforster:trails-bom:0.0.1`, then add:

```xml
<dependency>
    <groupId>io.github.yaforster</groupId>
    <artifactId>trails-printer-pdf</artifactId>
</dependency>
```

Spring Boot discovers the adapter automatically. Trails requires exactly one `PrintService` provider and rejects
startup when none or multiple are installed.

**No printing configuration properties are required or supported.**
**The visual design of the printing output or the data that is printed cannot be customized.**
**If you need custom printing, do not use this adapter.**
Adding the Maven dependency enables the adapter.
