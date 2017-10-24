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
        Map<String, V> map = newInstance();
        if (BsonType.DOCUMENT.equals(reader.getCurrentBsonType())) {
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String key = reader.readName();
                V value = valueTypeCodec.decode(reader, decoderContext);
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
            valueTypeCodec.encode(writer, entry.getValue(), encoderContext);
        }
        writer.writeEndDocument();
    }
}