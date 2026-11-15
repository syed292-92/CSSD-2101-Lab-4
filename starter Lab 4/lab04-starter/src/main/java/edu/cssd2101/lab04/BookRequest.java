package edu.cssd2101.lab04;

/**
 * Input DTO deliberately has no identifier or currency selector.
 *
 * @param title proposed title
 * @param priceCents proposed CAD cents; null detects a missing JSON field
 * @param format proposed publication format
 */
public record BookRequest(String title, Long priceCents, Format format) {
    /**
     * Converts untrusted input to a validated domain entry.
     *
     * @param id service-selected identifier
     * @return validated immutable entry
     * @throws IllegalArgumentException if a required field is absent or invalid
     */
    public Book toBook(String id) {
        if (priceCents == null) throw new IllegalArgumentException("Price is required");
        return new Book(id, title, priceCents, format);
    }
}
