package de.bild.codec.annotations;

import de.bild.codec.SpecialFieldsMap;

import java.lang.annotation.*;

/**
 * An entity class that implements {@link SpecialFieldsMap} can have methods
 * that define the value being encoded/decoded by their return type. Those methods mus tbe annotated with
 * this annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldMapping {
    // although the name of the field could be determined by evaluating the method name,
    // for now the field name must be set
    String value();
}