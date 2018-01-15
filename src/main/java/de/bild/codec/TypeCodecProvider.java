package de.bild.codec;

import org.bson.codecs.Codec;

import java.lang.reflect.Type;


/**
 * The mongo driver {@link org.bson.codecs.configuration.CodecProvider} was not designed to accept {@link Type} as parameter
 * This {@link TypeCodecProvider} can be used to register {@link Codec}s for any given {@link Type}
 * Register your {@link TypeCodecProvider} when building {@link PojoCodecProvider.Builder#register(TypeCodecProvider...)}
 *
 * TypeCodecProvider helps you to handle a given fields within your Pojos as special way.
 */
public interface TypeCodecProvider {

    <T> Codec<T> get(Type type, TypeCodecRegistry typeCodecRegistry);
}
