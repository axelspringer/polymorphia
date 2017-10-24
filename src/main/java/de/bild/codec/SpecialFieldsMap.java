package de.bild.codec;


import java.util.Map;

/**
 * A marker interface to mark entity classes that are maps with special fields
 * Special fields can be declared by getter methods with a {@link de.bild.codec.annotations.FieldMapping} annotation
 */
public interface SpecialFieldsMap extends Map<String, Object> {
}