package za.codemaster.backend.dto.common;

/**
 * Implemented by every enum whose JSON wire value (via {@code @JsonValue}) differs
 * from its Java constant name — e.g. {@code Difficulty.BEGINNER} serializes as
 * {@code "beginner"}. Spring's default {@code @RequestParam} enum binding calls
 * plain (case-sensitive) {@code Enum.valueOf}, which only matches the constant
 * name, never the wire value — so without a custom converter,
 * {@code ?difficulty=beginner} 500s even though that's the exact value the API
 * spec documents. {@code WireValueMatcher} (registered in {@code WebMvcConfig})
 * is the fix: it matches a query param against each constant's wire value instead.
 */
public interface WireValued {
    String getWireValue();
}
