package io.github.yaforster.trails.printer.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Converts prepared report data into HTML and then writes the PDF document. */
final class PdfReportRenderer {

    private final TemplateEngine templates;

    PdfReportRenderer(TemplateEngine templates) {
        this.templates = Objects.requireNonNull(templates, "Template engine is required");
    }

    void render(RenderConfiguration renderConfig, OutputStream outputStream) throws IOException {
        try {
            String reportHtml = renderReportHtml(renderConfig);
            writePdf(reportHtml, outputStream);
        } catch (RuntimeException exception) {
            throw new IOException("Could not render PDF", exception);
        }
    }

    /**
     * Binds the fixed names expected by the bundled templates. {@code context} holds SDK report data;
     * {@code screenshotsByActionResultId} lets the shared action fragment find an image by its result ID at every
     * report level. These strings are template variable names, not data keys. Keeping them here avoids generic variable
     * maps throughout report preparation.
     */
    private String renderReportHtml(RenderConfiguration renderConfig) {
        Context templateContext = new Context();
        templateContext.setVariable("context", renderConfig.printContext());
        templateContext.setVariable("screenshotsByActionResultId", renderConfig.screenshotsByActionResultId());
        return templates.process(renderConfig.template(), templateContext);
    }

    private void writePdf(String reportHtml, OutputStream outputStream) throws IOException {
        PdfRendererBuilder pdf = new PdfRendererBuilder();
        pdf.withHtmlContent(reportHtml, null);
        pdf.toStream(outputStream);
        pdf.run();
    }
}
