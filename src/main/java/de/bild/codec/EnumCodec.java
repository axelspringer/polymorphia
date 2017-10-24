package de.bild.codec;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Codec for enum.
 * encode: value.name()
 * decode: Enum.valueOf(clazz, name)
 */
public class EnumCodec<T extends Enum<T>> implements Codec<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnumCodec.class);

    final Class<T> clazz;

    public EnumCodec(Class<T> clazz) {
        this.clazz = clazz;

    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        if (BsonType.STRING.equals(reader.getCurrentBsonType())) {
            String name = reader.readString();
            if (name != null) {
                try {
                    return Enum.valueOf(clazz, name);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Enum value {} could not be determined for enum type {}", name, clazz, e);
                }
            }
        } else {
            LOGGER.warn("Expected {} from reader but got {}. Skipping value.", BsonType.STRING, reader.getCurrentBsonType());
            reader.skipValue();
        }
        return null;
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        writer.writeString(value.name());
    }

    @Override
    public Class<T> getEncoderClass() {
        return clazz;
    }
}
