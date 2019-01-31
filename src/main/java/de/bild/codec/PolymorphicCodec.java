package de.bild.codec;

import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Provides functionality for handling polymorphic structures.
 * decode() as well as encode() have default implementations within this interface.
 * <p>
 * Attention: The name of this interface may be misleading.
 * Any Codec that might be part of a polymorphic codec structure needs to implement this interface,
 * as the codec needs to answer certain questions, most obviously {@link #verifyFieldsNotNamedLikeAnyDiscriminatorKey}
 * A map of {@link PolymorphicCodec} is part of {@link PolymorphicReflectionCodec} which itself does not implement {@link PolymorphicCodec}
 *
 * @param <T> the value type
 */
public interface PolymorphicCodec<T> extends TypeCodec<T> {
    Logger LOGGER = LoggerFactory.getLogger(PolymorphicCodec.class);

    /*
     * When encoding polymorphic types, a discriminator must be written to the database along with the instance.
     * Instead of restructuring the json within the database, the discriminator key/value will be written at the same
     * json level as the instance data. Nesting the instance data in a deeper level may complicate things in the database,
     * especially if you store non-polymorphic entities together with polymorphic entities in one collection and if
     * you need to set an index at a specific field these entities have in common.
     *
     * Hence provide methods for encoding all entity fields as well as one for decoding just the fields. The wrapping
     * curly brackets will be written along with the discriminator in encode()
     *
     */
    T decodeFields(BsonReader reader, DecoderContext decoderContext, T instance);

    void encodeFields(BsonWriter writer, T instance, EncoderContext encoderContext);

    T newInstance();

    @Override
    default T decode(BsonReader reader, DecoderContext decoderContext) {

            T newInstance;
            if (reader.getCurrentBsonType() == null || reader.getCurrentBsonType() == BsonType.DOCUMENT) {

                BsonReaderMark mark = null;
                try {
                    mark = reader.getMark();
                    reader.readStartDocument();
                    newInstance = decodeFields(reader, decoderContext, newInstance());
                    reader.readEndDocument();
                } catch (Exception e) {
                    LOGGER.error("Exception while reading pojo from reader. Skipping value.", e);
                    switch (getDecodingPojoFailureStrategy()) {
                        case RETHROW_EXCEPTION:
                            throw e;
                        case NULL:
                        default: {
                            if (mark != null) {
                                mark.reset();
                                reader.skipValue();
                            }
                        }
                        return null;
                    }
                }

                return newInstance;
            } else {
                LOGGER.error("Expected to read document but reader is in state {}. Skipping value!", reader.getCurrentBsonType());
                reader.skipValue();
                return null;
            }

    }

    @Override
    default void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeStartDocument();
            encodeFields(writer, value, encoderContext);
            writer.writeEndDocument();
        }
    }

    /**
     * A check for properties with names equal to any of the identified discriminator keys
     *
     * @param discriminatorKeys the identified discriminator keys
     *                          throws an {@link IllegalArgumentException} if a name of an internally used property is equal to one of the discriminator keys
     */
    void verifyFieldsNotNamedLikeAnyDiscriminatorKey(Set<String> discriminatorKeys) throws IllegalArgumentException;

}