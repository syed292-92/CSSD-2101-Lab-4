package edu.cssd2101.lab04;

/** Indicates an unavailable or invalid persistence snapshot. */
public final class StorageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Wraps an internal failure without exposing its message through HTTP.
     *
     * @param cause internal failure, retained for debugging
     */
    public StorageException(Throwable cause) {
        super("Catalogue storage unavailable", cause);
    }
}
