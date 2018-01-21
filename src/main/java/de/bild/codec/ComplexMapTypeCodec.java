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
import java.util.Map;


public class ComplexMapTypeCodec<K, V> extends MapTypeCodec<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexMapTypeCodec.class);
    final Codec<K> keyTypeCodec;

    public ComplexMapTypeCodec(Class<Map<K, V>> encoderClass, Type keyType, Type valueType, TypeCodecRegistry typeCodecRegistry) {
        super(encoderClass, valueType, typeCodecRegistry);
        this.keyTypeCodec = typeCodecRegistry.getCodec(keyType);
    }

    @Override
    public Map<K, V> decode(BsonReader reader, DecoderContext decoderContext) {
        Map<K, V> map = null;
        if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
            reader.skipValue();
        } else if (BsonType.ARRAY.equals(reader.getCurrentBsonType())) {
            map = newInstance();
            reader.readStartArray();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (BsonType.DOCUMENT.equals(reader.getCurrentBsonType())) {
                    reader.readStartDocument();
                    reader.readName(); // don't need the key == "key"
                    K key = keyTypeCodec.decode(reader, decoderContext);
                    reader.readName(); // don't need the key == "value"
                    V value = valueTypeCodec.decode(reader, decoderContext);
                    map.put(key, value);
                    reader.readEndDocument();
                } else {
                    LOGGER.warn("Expected {} from reader but got {}. Skipping value.", BsonType.DOCUMENT, reader.getCurrentBsonType());
                    reader.skipValue();
                }
            }
            reader.readEndArray();
        } else {
            LOGGER.warn("Expected {} from reader but got {}. Skipping value.", BsonType.ARRAY, reader.getCurrentBsonType());
            reader.skipValue();
        }
        return map;
    }

    @Override
    public void encode(BsonWriter writer, Map<K, V> map, EncoderContext encoderContext) {
        writer.writeStartArray();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            writer.writeStartDocument();
            writer.writeName("key");
            keyTypeCodec.encode(writer, entry.getKey(), encoderContext);
            writer.writeName("value");
            V value = entry.getValue();
            if (value != null) {
                valueTypeCodec.encode(writer, value, encoderContext);
            } else {
                writer.writeNull();
            }
            writer.writeEndDocument();
        }
        writer.writeEndArray();
    }
}
