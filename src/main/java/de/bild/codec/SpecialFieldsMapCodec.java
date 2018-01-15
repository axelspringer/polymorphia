package de.bild.codec;

import de.bild.codec.annotations.FieldMapping;
import org.bson.BsonBinarySubType;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;


/**
 * This Codec can be used to decode/encode map-like structures but decode/encode certain properties within the map into specialized types
 * This Codec is mainly inspired by {@link org.bson.codecs.DocumentCodec}
 */
public class SpecialFieldsMapCodec<T extends Map<String, Object>> extends AbstractTypeCodec<T> {
    final Map<String, Codec> fieldMappingCodecs = new HashMap<>();
    private static final BsonTypeClassMap DEFAULT_BSON_TYPE_CLASS_MAP = new BsonTypeClassMap();

    private final BsonTypeCodecMap bsonTypeCodecMap;
    private final CodecRegistry codecRegistry;


    public SpecialFieldsMapCodec(Type type, TypeCodecRegistry typeCodecRegistry) {
        super(type, typeCodecRegistry);
        this.codecRegistry = typeCodecRegistry.getRegistry();
        for (MethodTypePair methodTypePair : ReflectionHelper.getDeclaredAndInheritedMethods(type)) {
            Method method = methodTypePair.getMethod();
            FieldMapping fieldMapping = method.getAnnotation(FieldMapping.class);
            if (fieldMapping != null) {
                fieldMappingCodecs.put(fieldMapping.value(), typeCodecRegistry.getCodec(methodTypePair.getRealType()));
            }
        }
        this.bsonTypeCodecMap = new BsonTypeCodecMap(DEFAULT_BSON_TYPE_CLASS_MAP, typeCodecRegistry.getRegistry());
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        T map = newInstance();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String key = reader.readName();

            Object value;
            Codec fieldMappingCodec = fieldMappingCodecs.get(key);
            if (fieldMappingCodec != null) {
                value = fieldMappingCodec.decode(reader, decoderContext);
            } else {
                value = readValue(reader, decoderContext);
            }

            map.put(key, value);
        }
        reader.readEndDocument();
        return map;
    }

    private Object readValue(final BsonReader reader, final DecoderContext decoderContext) {
        BsonType bsonType = reader.getCurrentBsonType();
        if (bsonType == BsonType.NULL) {
            reader.readNull();
            return null;
        } else if (bsonType == BsonType.ARRAY) {
            return readList(reader, decoderContext);
        } else if (bsonType == BsonType.BINARY && BsonBinarySubType.isUuid(reader.peekBinarySubType()) && reader.peekBinarySize() == 16) {
            return codecRegistry.get(UUID.class).decode(reader, decoderContext);
        }
        return bsonTypeCodecMap.get(bsonType).decode(reader, decoderContext);
    }

    private List<Object> readList(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();
        List<Object> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(readValue(reader, decoderContext));
        }
        reader.readEndArray();
        return list;
    }

    @Override
    public void encode(BsonWriter writer, T map, EncoderContext encoderContext) {
        if (map == null) {
            writer.writeNull();
        }
        else {
            writer.writeStartDocument();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                writer.writeName(entry.getKey());
                Object value = entry.getValue();

                Codec fieldMappingCodec = fieldMappingCodecs.get(entry.getKey());
                if (fieldMappingCodec != null) {
                    fieldMappingCodec.encode(writer, value, encoderContext);
                } else {
                    if (value != null) {
                        Codec codec = codecRegistry.get(value.getClass());
                        codec.encode(writer, value, encoderContext);
                    } else {
                        writer.writeNull();
                    }
                }
            }
            writer.writeEndDocument();
        }
    }
}