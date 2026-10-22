package io.github.yaforster.trails.printer.pdf;

import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService;
import io.github.yaforster.trails.app.services.PrintService;
import io.github.yaforster.trails.core.persisted.PersistedActionResult;
import io.github.yaforster.trails.core.print.TestPathPrintContext;
import io.github.yaforster.trails.core.print.TestRunPrintContext;
import io.github.yaforster.trails.core.print.TestSetPrintContext;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.thymeleaf.TemplateEngine;

/** Prepares path, browser-result, and test-run reports with their persisted action screenshots. */
public final class PdfPrintService implements PrintService {

    private static final String TEST_PATH_TEMPLATE = "print_test_path_template";
    private static final String TEST_SET_TEMPLATE = "print_test_set_template";
    private static final String TEST_RUN_TEMPLATE = "print_test_run_template";
    private final ReportScreenshotLoader screenshots;
    private final PdfReportRenderer renderer;

    /**
     * Creates a printer using an adapter-owned template engine and the selected persistence adapter's screenshot port.
     *
     * @param templates
     *            the template engine for this adapter's bundled report templates
     * @param screenshotQueryService
     *            queries screenshots by persisted action-result ID
     */
    public PdfPrintService(TemplateEngine templates, ActionResultScreenshotQueryService screenshotQueryService) {
        this.renderer = new PdfReportRenderer(templates);
        this.screenshots = new ReportScreenshotLoader(screenshotQueryService);
    }

    @Override
    public void renderTestPathPdf(TestPathPdf request) throws IOException {
        TestPathPrintContext path = request.testPathPrintContext();
        Map<Long, String> screenshotDataUris = screenshots.loadForActions(path.actionResults());
        RenderConfiguration renderConfig = new RenderConfiguration(TEST_PATH_TEMPLATE, path, screenshotDataUris);
        renderer.render(renderConfig, request.outputStream());
    }

    @Override
    public void renderTestSetPdf(TestSetPdf request) throws IOException {
        TestSetPrintContext browserResult = request.testSetPrintContext();
        List<PersistedActionResult> actions = actionsInPaths(browserResult.pathPrintContexts());
        Map<Long, String> screenshotDataUris = screenshots.loadForActions(actions);
        RenderConfiguration renderConfig = new RenderConfiguration(TEST_SET_TEMPLATE, browserResult,
                screenshotDataUris);
        renderer.render(renderConfig, request.outputStream());
    }

    @Override
    public void renderTestRunPdf(TestRunPdf request) throws IOException {
        TestRunPrintContext testRun = request.testRunPrintContext();
        List<PersistedActionResult> actions = actionsInTestRun(testRun);
        Map<Long, String> screenshotDataUris = screenshots.loadForActions(actions);
        RenderConfiguration renderConfig = new RenderConfiguration(TEST_RUN_TEMPLATE, testRun, screenshotDataUris);
        renderer.render(renderConfig, request.outputStream());
    }

    private List<PersistedActionResult> actionsInTestRun(TestRunPrintContext testRun) {
        List<TestPathPrintContext> paths = testRun.testSetPrintContexts()
                                                  .stream()
                                                  .flatMap(browserResult -> browserResult.pathPrintContexts()
                                                                                         .stream())
                                                  .toList();
        return actionsInPaths(paths);
    }

    private List<PersistedActionResult> actionsInPaths(List<TestPathPrintContext> paths) {
        return paths.stream()
                    .flatMap(path -> path.actionResults()
                                         .stream())
                    .toList();
    }

}
