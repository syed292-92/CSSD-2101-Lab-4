package edu.cssd2101.lab04;

/** Indicates that a valid identifier has no catalogue entry. */
public final class MissingBookException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Creates a missing-entry failure with a stable safe message. */
    public MissingBookException() {
        super("Book not found");
    }
}
