package de.bild.codec.annotations;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
/**
 * Annotation to be used at POJO methods.
 * These methods will be invoked after a POJO was decoded.
 */
public @interface PostLoad {
}