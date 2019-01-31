package de.bild.codec.annotations;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface DecodingFieldFailureStrategy {
    Strategy value() default Strategy.RETHROW_EXCEPTION;

    enum Strategy {
        SET_TO_NULL,
        RETHROW_EXCEPTION,
        SKIP
    }
}
