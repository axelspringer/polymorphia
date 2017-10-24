package de.bild.codec;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.codec.annotations.Discriminator;
import de.bild.codec.annotations.DiscriminatorFallback;
import de.bild.codec.annotations.Id;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PolymorphicReflectionCodecTest.class)
@EnableAutoConfiguration
public class PolymorphicReflectionCodecTest {

    protected static final String DB_NAME = "test";

    @Autowired
    private MongoClient mongoClient;


    @Test
    public void testFallback() {
        PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().
                register(FirstClassInHierarchy.class).
                build();

        assertNotNull(pojoCodecProvider);
        CodecRegistry pojoCodecRegistry = fromRegistries(fromProviders(pojoCodecProvider), MongoClient.getDefaultCodecRegistry());
        MongoCollection<FirstClassInHierarchy> documents = mongoClient.getDatabase(DB_NAME)
                .getCollection("documents")
                .withCodecRegistry(pojoCodecRegistry)
                .withDocumentClass(FirstClassInHierarchy.class);

        FirstClassInHierarchy pojo = new FirstClassInHierarchy();
        documents.insertOne(pojo);


        Document document = mongoClient.getDatabase(DB_NAME).
                getCollection("documents").
                find(Filters.eq("_id", pojo.id)).first();

        assertNull(document.get("type"));
        FirstClassInHierarchy first = documents.find(Filters.eq("_id", pojo.id)).first();
        assertNotNull(first);


        // now build new codecregistry including class hierarchy
        pojoCodecProvider = PojoCodecProvider.builder().
                register(FirstClassInHierarchy.class).
                register(SubClass.class).
                register(AnotherSubClass.class).
                build();
        pojoCodecRegistry = fromRegistries(fromProviders(pojoCodecProvider), MongoClient.getDefaultCodecRegistry());

        documents = mongoClient.getDatabase(DB_NAME).
                getCollection("documents").
                withCodecRegistry(pojoCodecRegistry).
                withDocumentClass(FirstClassInHierarchy.class);

        first = documents.find(Filters.eq("_id", pojo.id)).first();
        assertNotNull(first);
        assertTrue(first instanceof AnotherSubClass);


    }


    static class FirstClassInHierarchy {
        @Id(collectible = true)
        ObjectId id;

        int property;
    }

    @Discriminator("SubClassDiscriminator")
    static class SubClass extends FirstClassInHierarchy {
        String subClassProperty;
    }

    @DiscriminatorFallback
    static class AnotherSubClass extends SubClass {

    }
}