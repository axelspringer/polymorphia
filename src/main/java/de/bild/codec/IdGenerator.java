package de.bild.codec;

import org.bson.BsonDocumentWrapper;
import org.bson.BsonValue;

public interface IdGenerator<T> {
    T generate();

    default BsonValue asBsonValue(T id, TypeCodecRegistry typeCodecRegistry) {
        return BsonDocumentWrapper.asBsonDocument(id, typeCodecRegistry.getRegistry());
    }
}