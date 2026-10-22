# Trails PDF Printer repository map

```text
trails-printer-pdf/
|-- pom.xml                                  SDK, Thymeleaf, OpenHTMLtoPDF, and publication setup
|-- .mvn/wrapper/                            Maven wrapper configuration
|-- mvnw                                    Unix Maven wrapper
|-- mvnw.cmd                                Windows Maven wrapper
|-- format/java-formatter.xml               SDK-aligned formatting rules
|-- src/main/java/.../printer/pdf/
|   |-- PdfPrintService.java                Prepares path, browser-result, and test-run reports
|   |-- ReportScreenshotLoader.java        Loads and encodes screenshots by persisted action-result ID
|   |-- RenderConfiguration.java           Immutable rendering inputs and documented screenshot lookup
|   |-- PdfReportRenderer.java             Template binding, HTML rendering, and PDF output
|   `-- PdfPrinterAutoConfiguration.java    PrintService registration and isolated template engine
|-- src/main/resources/
|   |-- META-INF/spring/                    Boot auto-configuration discovery
|   `-- templates/trails-printer-pdf/       Three report templates and shared fragments
|-- src/test/java/.../printer/pdf/          Real PDF, screenshot, composition, and isolation tests
|-- README.md                              Consumer setup and complete configuration reference
|-- DEVELOPMENT.md                         Build and publication instructions
|-- ARCHITECTURE.md                        Report flow, template contract, and correctness guarantees
|-- LICENSE                                Apache 2.0 license
`-- REPOMAP.md                             This map
```

The adapter owns PDF rendering only. Application contracts, result retrieval orchestration, hierarchy validation,
and HTTP transport remain in `trails-sdk`.
