package de.bild.codec;

public class TypeMismatchException extends RuntimeException {
    public TypeMismatchException(String message) {
        super(message);
    }

    public TypeMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
