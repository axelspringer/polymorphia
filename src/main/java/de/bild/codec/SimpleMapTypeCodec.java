package de.bild.codec;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;


public class SimpleMapTypeCodec<V> extends MapTypeCodec<String, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMapTypeCodec.class);

    public SimpleMapTypeCodec(Class<Map<String, V>> encoderClass, Type valueType, TypeCodecRegistry typeCodecRegistry) {
        super(encoderClass, valueType, typeCodecRegistry);
    }

    @Override
    public Map<String, V> decode(BsonReader reader, DecoderContext decoderContext) {
        Map<String, V> map = null;
        if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
            reader.skipValue();
        } else if (BsonType.DOCUMENT.equals(reader.getCurrentBsonType())) {
            map = newInstance();
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String key = reader.readName();
                V value = null;
                if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
                    reader.skipValue();
                } else {
                    value = valueTypeCodec.decode(reader, decoderContext);
                }
                map.put(key, value);
            }
            reader.readEndDocument();
        } else {
            LOGGER.warn("Expected {} from reader but got {}. Skipping value.", BsonType.DOCUMENT, reader.getCurrentBsonType());
            reader.skipValue();
        }
        return map;
    }

    @Override
    public void encode(BsonWriter writer, Map<String, V> map, EncoderContext encoderContext) {
        writer.writeStartDocument();
        for (Map.Entry<String, V> entry : map.entrySet()) {
            writer.writeName(entry.getKey());
            V value = entry.getValue();
            if (value != null) {
                valueTypeCodec.encode(writer, value, encoderContext);
            } else {
                writer.writeNull();
            }
        }
        writer.writeEndDocument();
    }
}