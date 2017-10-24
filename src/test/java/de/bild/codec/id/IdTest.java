package de.bild.codec.id;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.PojoCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IdTest.class)
@EnableAutoConfiguration
@ComponentScan(basePackages = "de.bild")
public class IdTest {
    protected static final String DB_NAME = "test";

    @Autowired
    private MongoClient mongoClient;

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder().register(IdTest.class.getPackage().getName()).build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Test
    public void testStrangeId() {
        final MongoCollection<EntityWithStrangeId> collection = mongoClient.getDatabase(DB_NAME).getCollection("documents").withDocumentClass(EntityWithStrangeId.class);
        EntityWithStrangeId entityWithStrangeId = new EntityWithStrangeId();
        collection.insertOne(entityWithStrangeId);
        assertThat(entityWithStrangeId.id, is(not(nullValue())));
        EntityWithStrangeId foundEntityWithStrangeId = collection.find(Filters.eq("_id", entityWithStrangeId.id)).first();
        assertThat(foundEntityWithStrangeId, is(not(nullValue())));
        assertThat(foundEntityWithStrangeId.id, equalTo(entityWithStrangeId.id));
    }
}
