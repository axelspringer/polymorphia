package de.bild.codec;

import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.types.ObjectId;

public class ObjectIdGenerator implements IdGenerator<ObjectId> {
    @Override
    public ObjectId generate() {
        return new ObjectId();
    }

    @Override
    public BsonValue asBsonValue(ObjectId id, TypeCodecRegistry typeCodecRegistry) {
        return new BsonObjectId(id);
    }
}
