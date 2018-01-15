package de.bild.codec;

public class IdGenerationException extends RuntimeException {
    public IdGenerationException(String message) {
        super(message);
    }

    public IdGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
