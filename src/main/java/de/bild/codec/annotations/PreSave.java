package de.bild.codec.annotations;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
/**
 * Annotation to be used at POJO methods.
 * These methods will be invoked before a POJO will be encoded and written to the database
 */
public @interface PreSave {
}