package de.bild.codec;

import org.bson.BsonDocumentWrapper;
import org.bson.BsonValue;

/**
 * Introducing this super-interface of {@link IdGenerator} allows for staying backward compatible with existing code.
 * @param <T> type of the id class
 * @param <P> type of the entity instance class
 */
public interface InstanceAwareIdGenerator<T, P> {
    T generate(P instance);

    default BsonValue asBsonValue(T id, TypeCodecRegistry typeCodecRegistry) {
        return BsonDocumentWrapper.asBsonDocument(id, typeCodecRegistry.getRegistry());
    }

}