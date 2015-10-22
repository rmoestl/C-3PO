package org.c_3po.generation;

/**
 * Exception indicating a generation problem.
 */
public final class GenerationException extends Exception {
    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
