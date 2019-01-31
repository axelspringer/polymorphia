package de.bild.codec.annotations;

import java.lang.annotation.*;

/**
 * Use this annotation to define the behaviour of the pojo codec in case of exceptions.
 * You can use this annotation on class level.
 * Two strategies can be chosen: NULL or RETHROW_EXCEPTION
 * Attention: be careful using this feature as this can lead to a "wrong" representation of your data stored within the database
 * @since 2.5.0
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DecodingPojoFailureStrategy {
    Strategy value() default Strategy.RETHROW_EXCEPTION;

    enum Strategy {
        NULL, // return a null value and ignore the exception
        RETHROW_EXCEPTION
    }
}
