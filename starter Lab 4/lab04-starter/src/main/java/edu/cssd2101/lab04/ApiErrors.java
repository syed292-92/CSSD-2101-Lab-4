package edu.cssd2101.lab04;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates known failures at the HTTP boundary. */
@RestControllerAdvice
public final class ApiErrors {
    /** Creates the stateless exception adapter. */
    public ApiErrors() {}

    /**
     * Maps invalid domain values or malformed JSON to 400.
     *
     * @return safe invalid-request response
     */
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> invalid() {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", "Check the request fields and identifier"));
    }

    /**
     * Maps missing resources to 404.
     *
     * @return safe not-found response
     */
    @ExceptionHandler(MissingBookException.class)
    public ResponseEntity<ApiError> missing() {
        return ResponseEntity.status(404).body(new ApiError("NOT_FOUND", "Book not found"));
    }

    /**
     * Maps storage failure to 503.
     *
     * @return safe retry-later response
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiError> storage() {
        return ResponseEntity.status(503)
                .body(new ApiError("STORAGE_UNAVAILABLE", "Catalogue could not be saved"));
    }
}
