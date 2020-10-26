package de.bild.backend.polymorphia.nonregisteredmodel;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.nonregisteredmodel.model.CompletePojo;
import de.bild.backend.polymorphia.nonregisteredmodel.model.Pojo;
import de.bild.backend.polymorphia.nonregisteredmodel.nonmodel.NonModelSomePropertyEntity;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.TypeMismatchException;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class NonRegisteredModelClassTest extends AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NonRegisteredModelClassTest.class);

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }

    @Test
    public void persistNonModelClassTest() {
        Assertions.assertThrows( TypeMismatchException.class , () -> {
            MongoCollection<Document> collection = getCollection();

            Pojo pojo = new Pojo();
            pojo.x = 42;
            pojo.property = new NonModelSomePropertyEntity<>();
            pojo.property.aString = "hopefully persisted";
            pojo.property.nonPersistedProperty = "hopefully not persisted";


            collection.withDocumentClass(Pojo.class).insertOne(pojo);
            Assert.assertTrue(pojo.id != null);


            Document document = collection.find(Filters.eq("_id", pojo.id)).first();


            Assert.assertNull(((Document)document.get("property")).get("nonPersistedProperty"));
            Assert.assertEquals(((Document)document.get("property")).get("aString"), "hopefully persisted");

            // now trigger exception
            collection.withDocumentClass(Pojo.class).find(Filters.eq("_id", pojo.id)).first();

        });
    }

    static class MorePropertiesPojo extends CompletePojo {
        public String someNonPersistableString;
    }

    @Test
    public void additionalPropertiesInPojoTest() {
        MongoCollection<Document> collection = mongoClient.getDatabase("test").getCollection("documents");

        MorePropertiesPojo pojo = new MorePropertiesPojo();
        pojo.someNonPersistableString = "do not persist me!";
        pojo.aString = "persistable String";
        pojo.anInt = 22;

        // first insert
        collection.withDocumentClass(MorePropertiesPojo.class).insertOne(pojo);

        // second insert
        pojo.id = null;
        collection.withDocumentClass(CompletePojo.class).insertOne(pojo);

        CompletePojo readPojo = collection.withDocumentClass(CompletePojo.class).find(Filters.eq("_id", pojo.id)).first();

        Assert.assertEquals(readPojo.aString, pojo.aString);

    }
}
