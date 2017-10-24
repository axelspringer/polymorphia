package de.bild.codec;

import java.lang.reflect.Type;

/**
 * This interface can be used to add special handling pojo of certain types.
 * Simply register a CodecResolver when you build the {@link PojoCodecProvider}
 */
public interface CodecResolver<T> {
    /**
     *
     * @param typeCodecRegistry codec registry for any type
     * @param type the type to be handled
     * @return null, if resolver cannot handle type or a codec that is able to handle the type
     */
    ReflectionCodec getCodec(Type type, TypeCodecRegistry typeCodecRegistry);
}
