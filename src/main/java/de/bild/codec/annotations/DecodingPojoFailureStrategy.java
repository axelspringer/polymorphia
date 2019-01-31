package de.bild.codec.annotations;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface DecodingPojoFailureStrategy {
    Strategy value() default Strategy.RETHROW_EXCEPTION;

    enum Strategy {
        NULL,
        RETHROW_EXCEPTION
    }
}
