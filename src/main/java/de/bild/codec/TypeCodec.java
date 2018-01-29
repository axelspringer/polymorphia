package de.bild.codec;

import org.apache.commons.lang3.NotImplementedException;
import org.bson.BsonNull;
import org.bson.BsonValue;
import org.bson.codecs.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * All codecs used within polymorphia need to implement this interface.
 *
 *
 * @param <T> the value type
 */
public interface TypeCodec<T> extends Codec<T> {
    Logger LOGGER = LoggerFactory.getLogger(TypeCodec.class);

    /**
     * Override this method if your Codec needs to supply default values as replacements for null values.
     * @return null or a default value
     */
    default T defaultInstance() {
        return null;
    }

    default boolean isCollectible() {
        return false;
    }

    default T generateIdIfAbsentFromDocument(T document) {
        LOGGER.warn("generateIdIfAbsentFromDocument() should be overridden if used!");
        return document;
    }

    default boolean documentHasId(T document) {
        LOGGER.warn("documentHasId() should be overridden if used!");
        return true;
    }

    default BsonValue getDocumentId(T document) {
        LOGGER.warn("getDocumentId() should be overridden if used!");
        return BsonNull.VALUE;
    }
}
