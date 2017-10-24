package de.bild.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.reflect.Type;

public interface TypeCodecRegistry {
    <T> Codec<T> getCodec(Type type);

    CodecRegistry getRegistry();
}
