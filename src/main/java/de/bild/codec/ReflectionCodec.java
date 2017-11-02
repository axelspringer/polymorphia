package de.bild.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Map;

/**
 *
 * @param <T> the value type
 */
public interface ReflectionCodec<T> extends TypeCodec<T> {
    Map<String, MappedField> getPersistenceFields();

    T decodeFields(BsonReader reader, DecoderContext decoderContext, T instance);

    void encodeFields(BsonWriter writer, T instance, EncoderContext encoderContext);

    default void postDecode(T instance) {
        // do nothing
    }

    default void preEncode(T instance) {
        // solely meant to not introduce braking changes
    }

    void initializeDefaults(T instance);

    MappedField getMappedField(String mappedFieldName);

    MappedField getIdField();
}
