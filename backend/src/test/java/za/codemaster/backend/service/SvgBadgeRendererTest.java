package za.codemaster.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link SvgBadgeRenderer} — no Spring context or database
 * needed, same rationale as {@link RateLimitServiceTest}: it's self-contained
 * string-building logic.
 */
class SvgBadgeRendererTest {

    @Test
    void rendersAWellFormedSvgContainingTheLabelAndValue() {
        String svg = SvgBadgeRenderer.render("code masters", "3 maintainers", SvgBadgeRenderer.ACCENT_COLOR);

        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.endsWith("</svg>"));
        assertTrue(svg.contains("code masters"));
        assertTrue(svg.contains("3 maintainers"));
        assertTrue(svg.contains(SvgBadgeRenderer.ACCENT_COLOR));
    }

    @Test
    void widensAsTheValueTextGrowsSoLongLabelsDoNotClipInsideTheGrid() {
        String shortBadge = SvgBadgeRenderer.render("code masters", "1", SvgBadgeRenderer.ACCENT_COLOR);
        String longBadge = SvgBadgeRenderer.render("code masters", "128 maintainers", SvgBadgeRenderer.ACCENT_COLOR);

        int shortWidth = extractWidth(shortBadge);
        int longWidth = extractWidth(longBadge);

        assertTrue(longWidth > shortWidth,
                "a longer value string must produce a wider badge, got short=" + shortWidth + " long=" + longWidth);
    }

    @Test
    void escapesAmpersandAndLessThanInLabelAndValueSoTheSvgStaysWellFormed() {
        String svg = SvgBadgeRenderer.render("A & B", "<script>", SvgBadgeRenderer.NEUTRAL_COLOR);

        assertFalse(svg.contains("<script>"), "raw '<script>' must never appear unescaped in the output");
        assertTrue(svg.contains("&amp;"));
        assertTrue(svg.contains("&lt;script&gt;") || svg.contains("&lt;script>"),
                "'<' must be escaped even though this renderer only ever needs to escape '&' and '<'");
    }

    private static int extractWidth(String svg) {
        int start = svg.indexOf("width=\"") + "width=\"".length();
        int end = svg.indexOf('"', start);
        return Integer.parseInt(svg.substring(start, end));
    }
}
