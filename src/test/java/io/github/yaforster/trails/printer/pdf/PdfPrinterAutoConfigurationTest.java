package io.github.yaforster.trails.printer.pdf;

import io.github.yaforster.trails.app.services.CapabilityContribution;
import io.github.yaforster.trails.app.services.CapabilityContributor;
import io.github.yaforster.trails.app.services.PrintService;
import io.github.yaforster.trails.core.CapabilityLabel;
import io.github.yaforster.trails.core.print.TestPathPrintContext;
import io.github.yaforster.trails.core.test.Browser;
import io.github.yaforster.trails.sdk.composition.AdapterId;
import io.github.yaforster.trails.sdk.composition.TrailsBrowserRegistration;
import io.github.yaforster.trails.sdk.composition.TrailsComposition;
import io.github.yaforster.trails.sdk.composition.TrailsPrintingRegistration;
import io.github.yaforster.trails.sdk.composition.TrailsPersistenceRegistration;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PdfPrinterAutoConfigurationTest {

    @Test
    void shouldRejectAnotherPrintingRegistration_evenWhenItIsPrimary() {
        new ApplicationContextRunner().withUserConfiguration(Wrapper.class)
                                      .withBean("customerPrinter", TrailsPrintingRegistration.class,
                                              () -> new TrailsPrintingRegistration(new AdapterId("customer-printer"),
                                                      mock()),
                                              definition -> definition.setPrimary(true))
                                      .run(context -> assertThat(context.getStartupFailure()).hasRootCauseMessage(
                                              "Invalid Trails composition: exactly one printing registration is required, found 2"));
    }

    @Test
    void shouldDiscoverPrinterAndRender_whenWrapperHasNoPrintingConfiguration() {
        new ApplicationContextRunner().withUserConfiguration(Wrapper.class)
                                      .run(context -> {
                                          ByteArrayOutputStream output = new ByteArrayOutputStream();
                                          context.getBean(TrailsComposition.class)
                                                 .printing()
                                                 .printService()
                                                 .renderTestPathPdf(new PrintService.TestPathPdf(
                                                         new TestPathPrintContext(1L, 2L, "Automatically configured",
                                                                 Browser.CHROME, List.of()),
                                                         output));
                                          try (PDDocument document = PDDocument.load(output.toByteArray())) {
                                              assertThat(new PDFTextStripper().getText(document)).contains(
                                                      "Test Path Result", "CHROME", "No action results available.");
                                          }
                                      });
    }

    @Test
    void shouldIgnoreHostTemplateEngineAndProperties_whenRendering() {
        TemplateEngine hostEngine = mock();
        new ApplicationContextRunner().withUserConfiguration(Wrapper.class)
                                      .withBean("hostTemplates", TemplateEngine.class, () -> hostEngine)
                                      .withPropertyValues("spring.thymeleaf.prefix=classpath:/missing/",
                                              "spring.thymeleaf.enabled=false")
                                      .run(context -> {
                                          context.getBean(TrailsComposition.class)
                                                 .printing()
                                                 .printService()
                                                 .renderTestPathPdf(
                                                         new PrintService.TestPathPdf(
                                                                 new TestPathPrintContext(1L, 2L, "Isolated",
                                                                         Browser.CHROME, List.of()),
                                                                 new ByteArrayOutputStream()));
                                          verifyNoInteractions(hostEngine);
                                      });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Wrapper {

        @Bean
        TrailsPersistenceRegistration persistence() {
            return new TrailsPersistenceRegistration(new AdapterId("test-persistence"),
                    new TrailsPersistenceRegistration.Ports(mock(), mock(), mock(), mock(), mock(), mock(), mock(),
                            mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(),
                            mock()));
        }

        @Bean
        TrailsBrowserRegistration browser() {
            return new TrailsBrowserRegistration(new AdapterId("test-browser"), mock());
        }

        @Bean
        CapabilityContributor browserCapabilities() {
            return () -> List.of(new CapabilityContribution(CapabilityLabel.FILE_DOWNLOAD_TIMEOUT_MILLIS, "30000"));
        }
    }
}
