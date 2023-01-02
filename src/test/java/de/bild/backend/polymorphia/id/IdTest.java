package de.bild.backend.polymorphia.id;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.codec.PojoCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


public class IdTest extends AbstractTest {

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder().register(IdTest.class.getPackage().getName()).build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }

    @Test
    public void testStrangeId() {
        final MongoCollection<EntityWithStrangeId> collection = getCollection(EntityWithStrangeId.class);
        EntityWithStrangeId entityWithStrangeId = new EntityWithStrangeId();
        collection.insertOne(entityWithStrangeId);
        assertThat(entityWithStrangeId.id, is(not(nullValue())));
        EntityWithStrangeId foundEntityWithStrangeId = collection.find(Filters.eq("_id", entityWithStrangeId.id)).first();
        assertThat(foundEntityWithStrangeId, is(not(nullValue())));
        assertThat(foundEntityWithStrangeId.id, equalTo(entityWithStrangeId.id));
    }


    @Test
    public void testMissingGeneratorClass() {
        Assertions.assertThrows(Exception.class, () -> {
            Codec<EntityWithMissingGenerator> codec = codecRegistry.get(EntityWithMissingGenerator.class);
        });
    }

    @Test
    public void testInstanceAwareIdGeneration() {
        final MongoCollection<PojoWithInstanceAwareIdGeneration> collection = getCollection(PojoWithInstanceAwareIdGeneration.class);
        PojoWithInstanceAwareIdGeneration pojo = new PojoWithInstanceAwareIdGeneration();
        pojo.useMeForIdGeneration = 42;
        collection.insertOne(pojo);
        assertThat(pojo.id, is(new PojoWithInstanceAwareIdGeneration.SomeId(42, 42)));
        PojoWithInstanceAwareIdGeneration foundPojo = collection.find(Filters.eq("_id", pojo.id)).first();
        assertThat(foundPojo, is(not(nullValue())));
        assertThat(foundPojo.id, equalTo(pojo.id));

    }
}
