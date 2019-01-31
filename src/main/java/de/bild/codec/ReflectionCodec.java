package de.bild.codec;

import java.util.Map;
import java.util.Set;


/**
 * Used by Polymorphia internally to tag codecs that use reflection to build a Codec for al properties
 * @param <T>
 */
public interface ReflectionCodec<T> extends PolymorphicCodec<T> {
    Map<String, MappedField<T, Object>> getPersistenceFields();

    MappedField getMappedField(String mappedFieldName);

    /**
     * Called after entity has been decoded
     * @param instance
     */
    void postDecode(T instance);

    /**
     * Called just before encoding
     * @param instance
     */
    void preEncode(T instance);

    @Override
    default void verifyFieldsNotNamedLikeAnyDiscriminatorKey(Set<String> propertyNames) throws IllegalArgumentException {
        for (String propertyName : propertyNames) {
            MappedField mappedField = getMappedField(propertyName);
            if (mappedField != null) {
                LOGGER.error("A field {} within {} is named like one of the discriminator keys {}", mappedField.getMappedFieldName(), getEncoderClass(), propertyNames);
                throw new IllegalArgumentException("A field " + mappedField.getMappedFieldName() + " within " + getEncoderClass() + " is named like one of the discriminator keys " + propertyNames);

            }
        }
    }
}