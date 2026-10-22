package io.github.yaforster.trails.printer.pdf;

import java.util.Map;
import java.util.Objects;

/**
 * Prepared report data passed to the renderer, separate from the destination output stream.
 *
 * @param template
 *            classpath template name without the HTML suffix
 * @param printContext
 *            SDK path, browser-result, or test-run context; the chosen template determines its shape
 * @param screenshotsByActionResultId
 *            persisted action-result ID to embedded image data URI, not action-definition ID; the screenshot query port
 *            already uses this identity across paths, so no path-to-action nested map is needed
 */
record RenderConfiguration(String template, Object printContext, Map<Long, String> screenshotsByActionResultId) {

    RenderConfiguration {
        Objects.requireNonNull(template, "Report template is required");
        Objects.requireNonNull(printContext, "Report context is required");
        screenshotsByActionResultId = Map.copyOf(screenshotsByActionResultId);
    }
}
