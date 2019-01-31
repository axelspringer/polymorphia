package de.bild.codec.annotations;

import java.lang.annotation.*;

/**
 * Use this annotation on pjo fields to define the codec behaviour in case of field decoding errors.
 * Strategies are:
 *
 * SET_TO_NULL - ignore exception and set field to null
 * RETHROW_EXCEPTION - rethrow the exception
 * SKIP - ignore the exception and leave the field to it's initial value
 *
 *
 * @since 2.5.0
 */
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
