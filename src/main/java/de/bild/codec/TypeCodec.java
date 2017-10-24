package de.bild.codec;

import org.apache.commons.lang3.NotImplementedException;
import org.bson.BsonValue;
import org.bson.codecs.Codec;


/**
 * All codecs within polymorphia would implement this interface.
 * @param <T> the value type
 */
public interface TypeCodec<T> extends Codec<T> {

    default T newInstance() {
        return null;
    }

    default T defaultInstance() {
        return null;
    }

    default boolean isCollectible() {
        return false;
    }

    default T generateIdIfAbsentFromDocument(T document) {
        throw new NotImplementedException("Please implement in collectible implementations.");
    }

    default boolean documentHasId(T document) {
        throw new NotImplementedException("Please implement in collectible implementations.");
    }

    default BsonValue getDocumentId(T document) {
        throw new NotImplementedException("Please implement in collectible implementations.");
    }
}
