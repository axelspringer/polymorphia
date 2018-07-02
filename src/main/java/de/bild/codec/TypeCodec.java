package de.bild.codec;

import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.BsonValue;
import org.bson.codecs.Codec;
import org.bson.conversions.Bson;
import org.slf4j.LoggerFactory;


/**
 * All codecs used within polymorphia need to implement this interface.
 *
 *
 * @param <T> the value type
 */
public interface TypeCodec<T> extends Codec<T> {
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
        LoggerFactory.getLogger(TypeCodec.class).warn("generateIdIfAbsentFromDocument() should be overridden if used!");
        return document;
    }

    default boolean documentHasId(T document) {
        LoggerFactory.getLogger(TypeCodec.class).warn("documentHasId() should be overridden if used!");
        return true;
    }

    default BsonValue getDocumentId(T document) {
        LoggerFactory.getLogger(TypeCodec.class).warn("getDocumentId() should be overridden if used!");
        return BsonNull.VALUE;
    }

    default Bson getTypeFilter() {
        return new BsonDocument();
    }
}
