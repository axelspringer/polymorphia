package de.bild.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class EnumCodecProvider implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz.isEnum()) {
            return new EnumCodec(clazz);
        }
        return null;
    }
}