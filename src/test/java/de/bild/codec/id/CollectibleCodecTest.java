package de.bild.codec.id;


import com.mongodb.MongoClient;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.annotations.Id;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

public class CollectibleCodecTest {

    CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromProviders(
                    PojoCodecProvider.builder().register(CollectibleCodecTest.class).build()
            ),
            MongoClient.getDefaultCodecRegistry());

    @Test
    public void testEntityWithId() {
        Codec<EntityWithId> entityWithIdCodec = codecRegistry.get(EntityWithId.class);
        Assert.assertFalse(entityWithIdCodec instanceof CollectibleCodec);
    }

    @Test
    public void testEntityWithCollectibleId() {
        Codec<EntityWithCollectibleId> codec = codecRegistry.get(EntityWithCollectibleId.class);
        Assert.assertTrue(codec instanceof CollectibleCodec);
    }

    @Test
    public void testEntityWithoutId() {
        Codec<EntityWithoutId> entityWithIdCodec = codecRegistry.get(EntityWithoutId.class);
        Assert.assertFalse(entityWithIdCodec instanceof CollectibleCodec);
    }

    static class EntityWithId {
        @Id
        ObjectId id;

        class YetAnotherInnerClass {
        }
    }

    static class EntityWithCollectibleId {
        @Id(collectible = true)
        ObjectId id;
    }


    static class EntityWithoutId {
        ObjectId id;
    }
}
