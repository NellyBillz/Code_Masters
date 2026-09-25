package za.codemaster.backend.service;

/**
 * Renders a small shields.io-style two-segment SVG badge (Embeddable README
 * Badge wow-feature, 2026-09-24) — a dark "label" segment beside a colored
 * "value" segment, e.g. {@code code masters | 3 maintainers}. Deliberately a
 * flat per-character width estimate rather than real font metrics: shields.io
 * itself measures actual glyph widths, but for the short label/value strings
 * this endpoint ever renders, an estimate reads identically and avoids
 * pulling in a font-metrics dependency for one small feature.
 */
final class SvgBadgeRenderer {

    /** Dark, neutral label-segment background — same tone as --cm-sidebar in the frontend palette. */
    static final String LABEL_COLOR = "#2b2b2b";
    /** Brand lime value-segment background — same as --cm-lime in the frontend palette. */
    static final String ACCENT_COLOR = "#C8FF64";
    /** Grey value-segment background for a "not found"/unknown result — never an HTTP error, see BadgeService. */
    static final String NEUTRAL_COLOR = "#9CA3AF";

    private static final int HEIGHT = 20;
    private static final int FONT_SIZE = 11;
    private static final double CHAR_WIDTH = 6.5;
    private static final int SEGMENT_PADDING = 16;
    private static final String LABEL_TEXT_COLOR = "#FFFFFF";
    private static final String VALUE_TEXT_COLOR = "#0A0A0A";

    private SvgBadgeRenderer() {
    }

    static String render(String label, String value, String valueColor) {
        int labelWidth = textWidth(label);
        int valueWidth = textWidth(value);
        int totalWidth = labelWidth + valueWidth;
        int labelCenterX = labelWidth / 2;
        int valueCenterX = labelWidth + valueWidth / 2;

        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + totalWidth + "\" height=\"" + HEIGHT
                + "\" role=\"img\" aria-label=\"" + escape(label) + ": " + escape(value) + "\">"
                + "<rect width=\"" + totalWidth + "\" height=\"" + HEIGHT + "\" rx=\"4\" fill=\"" + LABEL_COLOR + "\"/>"
                + "<rect x=\"" + labelWidth + "\" width=\"" + valueWidth + "\" height=\"" + HEIGHT
                + "\" rx=\"4\" fill=\"" + valueColor + "\"/>"
                + "<rect x=\"" + labelWidth + "\" width=\"6\" height=\"" + HEIGHT + "\" fill=\"" + valueColor + "\"/>"
                + "<text x=\"" + labelCenterX + "\" y=\"14\" font-family=\"Verdana,Geneva,sans-serif\" font-size=\""
                + FONT_SIZE + "\" fill=\"" + LABEL_TEXT_COLOR + "\" text-anchor=\"middle\">" + escape(label) + "</text>"
                + "<text x=\"" + valueCenterX + "\" y=\"14\" font-family=\"Verdana,Geneva,sans-serif\" font-size=\""
                + FONT_SIZE + "\" fill=\"" + VALUE_TEXT_COLOR + "\" text-anchor=\"middle\">" + escape(value) + "</text>"
                + "</svg>";
    }

    private static int textWidth(String text) {
        return (int) Math.ceil(text.length() * CHAR_WIDTH) + SEGMENT_PADDING;
    }

    /** SVG text content only ever needs these two characters escaped for the strings this renderer receives. */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;");
    }
}
