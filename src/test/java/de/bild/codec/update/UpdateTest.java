package de.bild.codec.update;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import de.bild.codec.CodecResolverTest;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.annotations.Id;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
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

import java.util.*;

import static com.mongodb.client.model.Updates.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = UpdateTest.class)
@EnableAutoConfiguration
@ComponentScan(basePackages = "de.bild")
public class UpdateTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodecResolverTest.class);

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register(UpdateTest.class)
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

    @Autowired
    private MongoClient mongoClient;

    interface PolymorphicType {
    }

    static class A implements PolymorphicType {
        Integer integer;

        public A() {
        }

        public A(Integer integer) {
            this.integer = integer;
        }
    }

    static class B implements PolymorphicType {
        Float aFloat;

        public B() {
        }

        public B(Float aFloat) {
            this.aFloat = aFloat;
        }
    }

    static class Pojo {
        @Id
        BsonTimestamp id;
        Action action;
        String user;
        List<PolymorphicType> listOfPolymorphicTypes;
    }

    static class ExtendedPojo {
        List<String> someListProperty;
    }

    enum Action {
        CREATE,
        READ,
        UPDATE,
        DELETE
    }

    // helper class to enable type detection at runtime
    static class PolymorphicTypeList extends ArrayList<PolymorphicType> {
        public PolymorphicTypeList() {
        }

        public PolymorphicTypeList(Collection<? extends PolymorphicType> c) {
            super(c);
        }
    }

    @Test
    public void updatePojoTest() {

        Bson update = combine(set("user", "Jim"),
                set("action", Action.DELETE),
                // unfortunately at this point we need to provide a non generic class, so the codec is able to determine all types
                // remember: type erasure makes it impossible to retrieve type argument values at runtime
                // @todo provide a mechanism to generate non-generic class on the fly. Is that even possible ?
                // set("listOfPolymorphicTypes", buildNonGenericClassOnTheFly(Arrays.asList(new A(123), new B(456f)), List.class, Type.class),
                set("listOfPolymorphicTypes", new PolymorphicTypeList(Arrays.asList(new A(123), new B(456f)))),
                currentDate("creationDate"),
                set("emptyListProperty", Collections.emptyList()),
                set("emptySetProperty", Collections.emptySet()),
                set("emptyMapProperty", Collections.emptyMap()),
                set("someListProperty", Arrays.asList("Hans", "Wurst")), // will be written to db, but ignored when reading
                currentTimestamp("_id"));

        FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);

        MongoCollection<Document> collection = mongoClient.getDatabase("test").getCollection("documents");
        MongoCollection<Pojo> pojoMongoCollection = collection.withDocumentClass(Pojo.class);

        Pojo pojo = pojoMongoCollection.findOneAndUpdate(Filters.and(Filters.lt(DBCollection.ID_FIELD_NAME, 0),
                Filters.gt(DBCollection.ID_FIELD_NAME, 0)), update, findOptions);

        assertNotNull(pojo.id);

        // finally check if non pojo fields have been updated too
        MongoCollection<ExtendedPojo> extPojoMongoCollection = collection.withDocumentClass(ExtendedPojo.class);
        ExtendedPojo extendedPojo = extPojoMongoCollection.find(Filters.eq(DBCollection.ID_FIELD_NAME, pojo.id)).first();
        assertEquals(extendedPojo.someListProperty, Arrays.asList("Hans", "Wurst"));
    }


    //private static <R> R buildNonGenericClassOnTheFly(R value, final Class<?> raw, final Type... typeArguments) {
    //}
}
