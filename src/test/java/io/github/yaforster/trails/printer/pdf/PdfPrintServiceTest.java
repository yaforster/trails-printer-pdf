package io.github.yaforster.trails.printer.pdf;

import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService;
import io.github.yaforster.trails.app.services.PrintService;
import io.github.yaforster.trails.core.data.ResultIndicator;
import io.github.yaforster.trails.core.persisted.ActionResultType;
import io.github.yaforster.trails.core.persisted.PersistedActionResult;
import io.github.yaforster.trails.core.persisted.PersistedArtifactFile;
import io.github.yaforster.trails.core.print.TestPathPrintContext;
import io.github.yaforster.trails.core.print.TestRunPrintContext;
import io.github.yaforster.trails.core.print.TestSetPrintContext;
import io.github.yaforster.trails.core.test.Browser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfPrintServiceTest {

    @ParameterizedTest
    @EnumSource(Report.class)
    void shouldRenderEscapedResultText_whenPrintingEachReport(Report report) throws Exception {
        PrintService printer = new PdfPrintService(PdfPrinterAutoConfiguration.createReportTemplateEngine(), mock());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        render(printer, report, List.of(action(1L)), output);
        try (PDDocument document = PDDocument.load(output.toByteArray())) {
            assertThat(new PDFTextStripper().getText(document)).contains("CHROME", "<b>Pay & confirm</b>", "SUCCESS");
        }
    }

    @ParameterizedTest
    @EnumSource(Report.class)
    void shouldEmbedScreenshot_whenPrintingEachReport(Report report) throws Exception {
        ActionResultScreenshotQueryService screenshots = mock();
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", image);
        when(screenshots.findAll(any())).thenReturn(
                Map.of(1L, new PersistedArtifactFile(9L, "screenshot.png", "image/png", image.toByteArray())));
        PrintService printer = new PdfPrintService(PdfPrinterAutoConfiguration.createReportTemplateEngine(),
                screenshots);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        render(printer, report, List.of(action(1L)), output);
        try (PDDocument document = PDDocument.load(output.toByteArray())) {
            int images = 0;
            for (PDPage page : document.getPages()) {
                for (COSName name : page.getResources()
                                        .getXObjectNames()) {
                    if (page.getResources()
                            .isImageXObject(name)) {
                        images++;
                    }
                }
            }
            assertThat(images).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @EnumSource(Report.class)
    void shouldRenderEmptyReports_whenNoActionsExist(Report report) throws Exception {
        PrintService printer = new PdfPrintService(PdfPrinterAutoConfiguration.createReportTemplateEngine(), mock());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        render(printer, report, List.of(), output);
        try (PDDocument document = PDDocument.load(output.toByteArray())) {
            assertThat(new PDFTextStripper().getText(document)).contains("No action results available.");
        }
    }

    @Test
    void shouldPropagateWriteFailure_whenOutputCannotBeWritten() {
        PrintService printer = new PdfPrintService(PdfPrinterAutoConfiguration.createReportTemplateEngine(), mock());
        OutputStream unavailable = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("Output unavailable");
            }
        };
        assertThatThrownBy(() -> render(printer, Report.PATH, List.of(), unavailable)).isInstanceOf(IOException.class);
    }

    private static PersistedActionResult action(long id) {
        return new PersistedActionResult(id, 78L, 10L, "<b>Pay & confirm</b>", "Payment accepted",
                ActionResultType.SUCCESS, null);
    }

    private void render(PrintService printer, Report report, List<PersistedActionResult> actions, OutputStream output)
            throws IOException {
        TestPathPrintContext path = new TestPathPrintContext(78L, 56L, "Checkout", Browser.CHROME, actions);
        TestSetPrintContext browser = new TestSetPrintContext(56L, 45L, 12L, 23L, Browser.CHROME, List.of(path));
        TestRunPrintContext run = new TestRunPrintContext(45L, 12L, 23L, 34L, Timestamp.valueOf("2026-10-03 12:34:56"),
                ResultIndicator.SUCCESS, "Checkout", List.of(browser));
        switch (report) {
            case PATH -> printer.renderTestPathPdf(new PrintService.TestPathPdf(path, output));
            case BROWSER -> printer.renderTestSetPdf(new PrintService.TestSetPdf(browser, output));
            case RUN -> printer.renderTestRunPdf(new PrintService.TestRunPdf(run, output));
        }
    }

    enum Report {
        PATH,
        BROWSER,
        RUN
    }
}
