package io.github.yaforster.trails.printer.pdf;

import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService;
import io.github.yaforster.trails.sdk.composition.AdapterId;
import io.github.yaforster.trails.sdk.composition.TrailsPersistenceRegistration;
import io.github.yaforster.trails.sdk.composition.TrailsPrintingRegistration;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Registers PDF printing without changing the customer's template engine or configuration. */
@AutoConfiguration
public class PdfPrinterAutoConfiguration {

    private static final String REPORT_TEMPLATE_DIRECTORY = "templates/trails-printer-pdf/";

    @Bean
    TrailsPrintingRegistration trailsPdfPrinter(TrailsPersistenceRegistration persistence) {
        SpringTemplateEngine reportTemplates = createReportTemplateEngine();
        ActionResultScreenshotQueryService screenshotQuery = persistence.ports()
                                                                        .actionResultScreenshotQueryService();
        return new TrailsPrintingRegistration(new AdapterId("pdf"),
                new PdfPrintService(reportTemplates, screenshotQuery));
    }

    static SpringTemplateEngine createReportTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(REPORT_TEMPLATE_DIRECTORY);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
