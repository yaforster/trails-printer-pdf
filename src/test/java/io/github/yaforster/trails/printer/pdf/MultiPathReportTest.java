package io.github.yaforster.trails.printer.pdf;

import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService;
import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService.ActionScreenshots;
import io.github.yaforster.trails.app.services.PrintService;
import io.github.yaforster.trails.core.data.ResultIndicator;
import io.github.yaforster.trails.core.persisted.ActionResultType;
import io.github.yaforster.trails.core.persisted.PersistedActionResult;
import io.github.yaforster.trails.core.persisted.PersistedArtifactFile;
import io.github.yaforster.trails.core.print.TestPathPrintContext;
import io.github.yaforster.trails.core.print.TestRunPrintContext;
import io.github.yaforster.trails.core.print.TestSetPrintContext;
import io.github.yaforster.trails.core.test.Browser;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiPathReportTest {

    @ParameterizedTest
    @EnumSource(Report.class)
    void shouldPlaceScreenshotsInPathOrder_whenSameActionWasExecutedInDifferentPaths(Report report) throws Exception {
        ActionResultScreenshotQueryService screenshotQuery = mock();
        when(screenshotQuery.findAll(new ActionScreenshots(List.of(101L, 102L)))).thenReturn(
                Map.of(101L, screenshot(Color.RED), 102L, screenshot(Color.BLUE)));
        PrintService printer = new PdfPrintService(PdfPrinterAutoConfiguration.createReportTemplateEngine(),
                screenshotQuery);
        List<TestPathPrintContext> paths = List.of(path(78L, 101L), path(79L, 102L));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        render(printer, report, paths, output);

        try (PDDocument document = PDDocument.load(output.toByteArray())) {
            assertThat(screenshotColorsInReadingOrder(document)).containsExactly(Color.RED.getRGB(),
                    Color.BLUE.getRGB());
        }
    }

    private void render(PrintService printer, Report report, List<TestPathPrintContext> paths,
            ByteArrayOutputStream output) throws IOException {
        TestSetPrintContext browser = new TestSetPrintContext(56L, 45L, 12L, 23L, Browser.CHROME, paths);
        if (report == Report.BROWSER) {
            printer.renderTestSetPdf(new PrintService.TestSetPdf(browser, output));
        } else {
            TestRunPrintContext run = new TestRunPrintContext(45L, 12L, 23L, 34L,
                    Timestamp.valueOf("2026-10-03 12:34:56"), ResultIndicator.SUCCESS, "Checkout", List.of(browser));
            printer.renderTestRunPdf(new PrintService.TestRunPdf(run, output));
        }
    }

    private TestPathPrintContext path(long pathId, long actionResultId) {
        PersistedActionResult action = new PersistedActionResult(actionResultId, pathId, 10L, "Confirm order",
                "Accepted", ActionResultType.SUCCESS, null);
        return new TestPathPrintContext(pathId, 56L, "Checkout " + pathId, Browser.CHROME, List.of(action));
    }

    private PersistedArtifactFile screenshot(Color color) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, color.getRGB());
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return new PersistedArtifactFile(9L, "screenshot.png", "image/png", png.toByteArray());
    }

    private List<Integer> screenshotColorsInReadingOrder(PDDocument document) throws IOException {
        List<Integer> colors = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            PDFStreamParser parser = new PDFStreamParser(page);
            parser.parse();
            List<Object> tokens = parser.getTokens();
            for (int index = 1; index < tokens.size(); index++) {
                if (tokens.get(index) instanceof Operator operator && operator.getName()
                                                                              .equals("Do")) {
                    COSName imageName = (COSName) tokens.get(index - 1);
                    PDXObject resource = page.getResources()
                                             .getXObject(imageName);
                    if (resource instanceof PDImageXObject image) {
                        colors.add(image.getImage()
                                        .getRGB(0, 0));
                    }
                }
            }
        }
        return colors;
    }

    enum Report {
        BROWSER, RUN
    }
}
