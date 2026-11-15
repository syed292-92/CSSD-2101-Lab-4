package edu.cssd2101.lab04;

/**
 * Stable public error representation; never contains a stack trace or filesystem path.
 *
 * @param code machine-readable classification
 * @param message safe human-readable explanation
 */
public record ApiError(String code, String message) {}
