package de.bild.codec.nonregisteredmodel;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.TypeMismatchException;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.nonregisteredmodel.model.CompletePojo;
import de.bild.codec.nonregisteredmodel.model.Pojo;
import de.bild.codec.nonregisteredmodel.nonmodel.NonModelSomePropertyEntity;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = NonRegisteredModelClassTest.class)
@EnableAutoConfiguration
@ComponentScan(basePackages = "de.bild")
public class NonRegisteredModelClassTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NonRegisteredModelClassTest.class);

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register("de.bild.codec.nonregisteredmodel.model")
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }
    @Autowired
    CodecRegistry codecRegistry;

    @Autowired
    private MongoClient mongoClient;


    @Test(expected = TypeMismatchException.class)
    public void persistNonModelClassTest() {
        MongoCollection<Document> collection = mongoClient.getDatabase("test").getCollection("documents");

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
