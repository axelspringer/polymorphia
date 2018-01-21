package de.bild.codec;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * This codec encodes/decodes any Set and Map (see sub classes)
 * Note that this codec will never persist any addition properties declared in sub classes
 * Solely the collection values itself will be persisted.
 * The advantage is to keep declared methods of Collecton sub classes.
 * If you really need to persist additional fields with your map, think about using composition over inheritance.
 * Use a container class that holds a reference to a Collection.
 *
 * @param <C> collection type
 * @param <V> value type
 */
public abstract class CollectionTypeCodec<C extends Collection<V>, V> extends AbstractTypeCodec<C> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionTypeCodec.class);
    Codec<V> typeCodec;

    public CollectionTypeCodec(Class<C> collectionClass, Type valueType, TypeCodecRegistry typeCodecRegistry) {
        super(collectionClass, typeCodecRegistry);
        typeCodec = typeCodecRegistry.getCodec(valueType);
    }

    @Override
    public C decode(BsonReader reader, DecoderContext decoderContext) {
        C collection = null;
        if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
            reader.skipValue();
        } else if (BsonType.ARRAY.equals(reader.getCurrentBsonType())) {
            collection = newInstance();
            reader.readStartArray();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                V decode = typeCodec.decode(reader, decoderContext);
                collection.add(decode);
            }
            reader.readEndArray();
        } else {
            LOGGER.warn("Expected {} from reader but got {}. Skipping value.", BsonType.ARRAY, reader.getCurrentBsonType());
            reader.skipValue();
        }
        return collection;
    }

    @Override
    public void encode(BsonWriter writer, C values, EncoderContext encoderContext) {
        writer.writeStartArray();
        for (V value : values) {
            if (value != null) {
                typeCodec.encode(writer, value, encoderContext);
            } else {
                writer.writeNull();
            }
        }
        writer.writeEndArray();
    }

    public C defaultInstance() {
        return newInstance();
    }
}
