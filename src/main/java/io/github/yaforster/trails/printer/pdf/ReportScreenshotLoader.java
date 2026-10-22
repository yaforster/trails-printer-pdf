package io.github.yaforster.trails.printer.pdf;

import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService;
import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService.ActionScreenshots;
import io.github.yaforster.trails.core.persisted.PersistedActionResult;
import io.github.yaforster.trails.core.persisted.PersistedArtifactFile;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Loads report screenshots in bounded queries and embeds their bytes as image data URIs. */
final class ReportScreenshotLoader {

    private static final int SCREENSHOT_BATCH_SIZE = 100;
    private final ActionResultScreenshotQueryService screenshotQuery;

    ReportScreenshotLoader(ActionResultScreenshotQueryService screenshotQuery) {
        this.screenshotQuery = Objects.requireNonNull(screenshotQuery,
                "Action-result screenshot query service is required");
    }

    /**
     * Returns persisted action-result ID to image data URI. Repeated action definitions can have different result IDs
     * and screenshots in different paths. Missing or empty screenshot content is omitted; repeated result IDs are
     * queried only once for the whole report.
     */
    Map<Long, String> loadForActions(List<PersistedActionResult> actions) {
        List<Long> actionResultIds = uniqueResultIds(actions);
        Map<Long, PersistedArtifactFile> screenshotFiles = loadScreenshotFiles(actionResultIds);
        return embedScreenshots(actionResultIds, screenshotFiles);
    }

    private List<Long> uniqueResultIds(List<PersistedActionResult> actions) {
        return actions.stream()
                      .map(PersistedActionResult::id)
                      .filter(Objects::nonNull)
                      .distinct()
                      .toList();
    }

    private Map<Long, PersistedArtifactFile> loadScreenshotFiles(List<Long> actionResultIds) {
        Map<Long, PersistedArtifactFile> screenshots = new LinkedHashMap<>();
        for (int offset = 0; offset < actionResultIds.size(); offset += SCREENSHOT_BATCH_SIZE) {
            int end = Math.min(offset + SCREENSHOT_BATCH_SIZE, actionResultIds.size());
            ActionScreenshots batch = new ActionScreenshots(actionResultIds.subList(offset, end));
            screenshots.putAll(screenshotQuery.findAll(batch));
        }
        return screenshots;
    }

    private Map<Long, String> embedScreenshots(List<Long> actionResultIds,
            Map<Long, PersistedArtifactFile> screenshotFiles) {
        Map<Long, String> embeddedImages = new LinkedHashMap<>();
        for (Long actionResultId : actionResultIds) {
            PersistedArtifactFile screenshot = screenshotFiles.get(actionResultId);
            if (hasImageContent(screenshot)) {
                embeddedImages.put(actionResultId, imageDataUri(screenshot));
            }
        }
        return Map.copyOf(embeddedImages);
    }

    private boolean hasImageContent(PersistedArtifactFile screenshot) {
        return screenshot != null && screenshot.content() != null && screenshot.content().length > 0;
    }

    private String imageDataUri(PersistedArtifactFile screenshot) {
        String contentType = supportedImageContentType(screenshot.contentType());
        String encodedImage = Base64.getEncoder()
                                    .encodeToString(screenshot.content());
        return "data:" + contentType + ";base64," + encodedImage;
    }

    /** Preserves the existing PNG fallback for missing or unrecognized screenshot MIME metadata. */
    private String supportedImageContentType(String contentType) {
        if (contentType == null) {
            return "image/png";
        }
        String normalized = contentType.trim()
                                       .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "image/png", "image/jpeg", "image/gif", "image/bmp", "image/webp" -> normalized;
            default -> "image/png";
        };
    }
}
