package de.bild.codec.annotations;

import de.bild.codec.IdGenerator;
import de.bild.codec.ObjectIdGenerator;

import java.lang.annotation.*;

/**
 * Use this annotation to mark the id field.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
    Class<? extends IdGenerator> value() default DefaultIdGenerator.class;

    boolean collectible() default false;

    final class DefaultIdGenerator extends ObjectIdGenerator {}
}