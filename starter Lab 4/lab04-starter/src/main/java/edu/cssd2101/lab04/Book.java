package edu.cssd2101.lab04;

/**
 * An immutable catalogue entry. Currency is CAD; amounts are integer cents.
 *
 * @param id canonical lower-case UUID assigned by the service
 * @param title trimmed title of 1 to 120 characters
 * @param priceCents price between zero and 100,000,000 cents inclusive
 * @param format physical or digital publication format
 */
public record Book(String id, String title, long priceCents, Format format) {
    /**
     * Validates and normalizes an entry.
     *
     * @throws IllegalArgumentException if any field violates its contract
     */
    public Book {
        validateId(id);
        if (title == null || title.isBlank() || title.strip().length() > 120)
            throw new IllegalArgumentException("Title must contain 1 to 120 characters");
        title = title.strip();
        if (priceCents < 0 || priceCents > 100_000_000)
            throw new IllegalArgumentException("Price must be 0 to 100000000 CAD cents");
        if (format == null) throw new IllegalArgumentException("Format is required");
    }

    /**
     * Validates canonical identifiers.
     *
     * @param id identifier to validate
     * @throws IllegalArgumentException if not a canonical lower-case UUID
     */
    public static void validateId(String id) {
        if (id == null
                || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw new IllegalArgumentException("Identifier must be a canonical UUID");
    }
}
