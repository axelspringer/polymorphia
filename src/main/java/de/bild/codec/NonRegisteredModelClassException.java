package de.bild.codec;

public class NonRegisteredModelClassException extends RuntimeException {
    public NonRegisteredModelClassException(String message) {
        super(message);
    }

    public NonRegisteredModelClassException(String message, Throwable cause) {
        super(message, cause);
    }
}
