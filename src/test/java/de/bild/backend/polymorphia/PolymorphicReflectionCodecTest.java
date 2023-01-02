package de.bild.backend.polymorphia;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.annotations.Discriminator;
import de.bild.codec.annotations.DiscriminatorFallback;
import de.bild.codec.annotations.Id;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.*;

public class PolymorphicReflectionCodecTest extends AbstractTest {

    protected static final String DB_NAME = "test";

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

    @Test
    public void testFallback() {
        PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().
                register(FirstClassInHierarchy.class).
                build();

        assertNotNull(pojoCodecProvider);
        CodecRegistry pojoCodecRegistry = fromRegistries(fromProviders(pojoCodecProvider), MongoClientSettings.getDefaultCodecRegistry());
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
        pojoCodecRegistry = fromRegistries(fromProviders(pojoCodecProvider), MongoClientSettings.getDefaultCodecRegistry());

        documents = mongoClient.getDatabase(DB_NAME).
                getCollection("documents").
                withCodecRegistry(pojoCodecRegistry).
                withDocumentClass(FirstClassInHierarchy.class);

        first = documents.find(Filters.eq("_id", pojo.id)).first();
        assertNotNull(first);
        assertTrue(first instanceof AnotherSubClass);
    }


    public interface Shape {
    }


    public static class ClassWithReservedFieldName implements Shape {
        String _t;
    }


    @Test
    public void testFieldNameLikeDiscriminatorKey() {
        Assertions.assertThrows(Exception.class, () -> {
            PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().
                    register(Shape.class).
                    register(ClassWithReservedFieldName.class).
                    build();

            assertNotNull(pojoCodecProvider);
            CodecRegistry pojoCodecRegistry = fromRegistries(fromProviders(pojoCodecProvider), MongoClientSettings.getDefaultCodecRegistry());
            MongoCollection<Shape> documents = mongoClient.getDatabase(DB_NAME)
                    .getCollection("documents")
                    .withCodecRegistry(pojoCodecRegistry)
                    .withDocumentClass(Shape.class);
            documents.insertOne(new ClassWithReservedFieldName());
        });
    }
}