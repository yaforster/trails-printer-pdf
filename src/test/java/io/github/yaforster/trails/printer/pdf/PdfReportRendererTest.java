package io.github.yaforster.trails.printer.pdf;

import io.github.yaforster.trails.core.print.TestPathPrintContext;
import io.github.yaforster.trails.core.test.Browser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.exceptions.TemplateInputException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfReportRendererTest {

    @Test
    void shouldPreserveTemplateFailureAsCause_whenReportTemplateCannotBeFound() {
        PdfReportRenderer renderer = new PdfReportRenderer(PdfPrinterAutoConfiguration.createReportTemplateEngine());
        TestPathPrintContext path = new TestPathPrintContext(1L, 2L, "Checkout", Browser.CHROME, List.of());
        RenderConfiguration renderConfig = new RenderConfiguration("missing-report", path, Map.of());

        assertThatThrownBy(
                () -> renderer.render(renderConfig, new ByteArrayOutputStream())).isInstanceOf(IOException.class)
                                                                                 .hasMessage("Could not render PDF")
                                                                                 .hasCauseInstanceOf(
                                                                                         TemplateInputException.class);
    }
}
