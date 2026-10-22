package io.github.yaforster.trails.printer.pdf;

import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService;
import io.github.yaforster.trails.app.services.ActionResultScreenshotQueryService.ActionScreenshots;
import io.github.yaforster.trails.core.persisted.ActionResultType;
import io.github.yaforster.trails.core.persisted.PersistedActionResult;
import io.github.yaforster.trails.core.persisted.PersistedArtifactFile;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportScreenshotLoaderTest {

    @Test
    void shouldBatchScreenshotQueries_whenReportExceedsOneBatch() {
        ActionResultScreenshotQueryService query = mock();
        ReportScreenshotLoader screenshots = new ReportScreenshotLoader(query);
        List<PersistedActionResult> actions = LongStream.rangeClosed(1, 101)
                                                        .mapToObj(ReportScreenshotLoaderTest::action)
                                                        .toList();

        screenshots.loadForActions(actions);

        ArgumentCaptor<ActionScreenshots> requests = ArgumentCaptor.forClass(ActionScreenshots.class);
        verify(query, times(2)).findAll(requests.capture());
        assertThat(requests.getAllValues()).containsExactly(new ActionScreenshots(LongStream.rangeClosed(1, 100)
                                                                                            .boxed()
                                                                                            .toList()),
                new ActionScreenshots(List.of(101L)));
    }

    @Test
    void shouldQueryEachResultOnce_whenResultsRepeatOrLackAnId() {
        ActionResultScreenshotQueryService query = mock();
        ReportScreenshotLoader screenshots = new ReportScreenshotLoader(query);

        screenshots.loadForActions(List.of(action(1L), action(1L), action(null), action(2L)));

        verify(query).findAll(new ActionScreenshots(List.of(1L, 2L)));
    }

    @Test
    void shouldAvoidScreenshotQueries_whenReportHasNoActions() {
        ActionResultScreenshotQueryService query = mock();

        new ReportScreenshotLoader(query).loadForActions(List.of());

        verifyNoInteractions(query);
    }

    @Test
    void shouldOmitUnavailableImages_whenScreenshotContentIsMissingOrEmpty() {
        ActionResultScreenshotQueryService query = mock();
        when(query.findAll(any())).thenReturn(Map.of(2L, file("image/png", null), 3L, file("image/png", new byte[0])));

        Map<Long, String> screenshots = new ReportScreenshotLoader(query).loadForActions(
                List.of(action(1L), action(2L), action(3L)));

        assertThat(screenshots).isEmpty();
    }

    @Test
    void shouldKeepSeparateScreenshots_whenSameActionDefinitionHasDifferentResults() {
        ActionResultScreenshotQueryService query = mock();
        when(query.findAll(any())).thenReturn(
                Map.of(1L, file("image/png", new byte[]{1, 2, 3}), 2L, file("image/png", new byte[]{4, 5, 6})));

        Map<Long, String> screenshots = new ReportScreenshotLoader(query).loadForActions(
                List.of(action(1L), action(2L)));

        assertThat(screenshots).containsExactlyInAnyOrderEntriesOf(
                Map.of(1L, "data:image/png;base64,AQID", 2L, "data:image/png;base64,BAUG"));
    }

    @ParameterizedTest
    @CsvSource({"image/png,image/png", "' IMAGE/JPEG ',image/jpeg", "image/gif,image/gif", "image/bmp,image/bmp",
            "image/webp,image/webp"})
    void shouldPreserveSupportedImageTypes_whenEmbeddingScreenshots(String storedType, String expectedType) {
        ActionResultScreenshotQueryService query = mock();
        when(query.findAll(any())).thenReturn(Map.of(1L, file(storedType, new byte[]{1, 2, 3})));

        Map<Long, String> screenshots = new ReportScreenshotLoader(query).loadForActions(List.of(action(1L)));

        assertThat(screenshots).containsEntry(1L, "data:" + expectedType + ";base64,AQID");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"application/octet-stream", "image/svg+xml"})
    void shouldRetainPngFallback_whenImageTypeIsMissingOrUnsupported(String storedType) {
        ActionResultScreenshotQueryService query = mock();
        when(query.findAll(any())).thenReturn(Map.of(1L, file(storedType, new byte[]{1, 2, 3})));

        Map<Long, String> screenshots = new ReportScreenshotLoader(query).loadForActions(List.of(action(1L)));

        assertThat(screenshots).containsEntry(1L, "data:image/png;base64,AQID");
    }

    @Test
    void shouldPropagateQueryFailure_whenPersistenceIsUnavailable() {
        ActionResultScreenshotQueryService query = mock();
        IllegalStateException failure = new IllegalStateException("Storage unavailable");
        when(query.findAll(any())).thenThrow(failure);

        assertThatThrownBy(() -> new ReportScreenshotLoader(query).loadForActions(List.of(action(1L)))).isSameAs(
                failure);
    }

    private static PersistedActionResult action(Long resultId) {
        return new PersistedActionResult(resultId, 78L, 10L, "Confirm order", "Accepted", ActionResultType.SUCCESS,
                null);
    }

    private PersistedArtifactFile file(String contentType, byte[] content) {
        return new PersistedArtifactFile(9L, "screenshot", contentType, content);
    }
}
