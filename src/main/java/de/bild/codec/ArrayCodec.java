package de.bild.codec;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Codec for multi dimensional
 */
public class ArrayCodec<T> implements TypeCodec<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArrayCodec.class);
    final Codec<Object> arrayElementCodec;
    final Class<T> arrayClazz;

    public ArrayCodec(Type type, TypeCodecRegistry typeCodecRegistry) {
        if (TypeUtils.isArrayType(type)) {
            arrayClazz = ReflectionHelper.extractRawClass(type);
            if (type instanceof GenericArrayType) {
                GenericArrayType genericArrayType = (GenericArrayType) type;
                arrayElementCodec = typeCodecRegistry.getCodec(genericArrayType.getGenericComponentType());
            } else {
                arrayElementCodec = typeCodecRegistry.getCodec(arrayClazz.getComponentType());
            }
        } else {
            throw new IllegalArgumentException("Unknown array type?!" + type);
        }
    }

    @Override
    public void encode(BsonWriter writer, T array, EncoderContext encoderContext) {
        writer.writeStartArray();
        for (int i = 0; i < Array.getLength(array); i++) {
            Object value = Array.get(array, i);
            if (value != null) {
                arrayElementCodec.encode(writer, value, encoderContext);
            } else {
                writer.writeNull();
            }

        }
        writer.writeEndArray();

    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        T array = null;
        if (BsonType.ARRAY.equals(reader.getCurrentBsonType())) {

            List list = new ArrayList();
            reader.readStartArray();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
                    reader.skipValue();
                    list.add(null);
                } else {
                    list.add(arrayElementCodec.decode(reader, decoderContext));
                }
            }
            reader.readEndArray();
            array = (T) Array.newInstance(arrayClazz.getComponentType(), list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
        } else {
            LOGGER.warn("Expected {} from reader but got {}. Skipping value.", BsonType.ARRAY, reader.getCurrentBsonType());
            reader.skipValue();
        }
        return array;
    }


    @Override
    public Class<T> getEncoderClass() {
        return arrayClazz;
    }

}