package de.bild.backend.polymorphia.stream;


import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.stream.model.Pojo;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.annotations.DecodingFieldFailureStrategy;
import de.bild.codec.annotations.DecodingPojoFailureStrategy;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;

public class FindManyTest extends AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FindManyTest.class);


    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .decodingFieldFailureStrategy(DecodingFieldFailureStrategy.Strategy.RETHROW_EXCEPTION)
                                    .decodingPojoFailureStrategy(DecodingPojoFailureStrategy.Strategy.NULL)
                                    .build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry()
            );
        }
    }

    /**
     *
     * This test inserts many valid pojos into te database but also one broken document, what can not be decoded properly.
     * Instead of checking each and every type received from the reader, if it matches the expected type, the codec marks the reader prior to decoding
     * each entity and in case an Exception occurred, the reader will backoff to the marked position and skip the broken entity.
     * The codec will return null values for non-readable pojos, so that the user can react on such a situation
     */
    @Test
    public void someTest() {
        Codec<Pojo> codec = codecRegistry.get(Pojo.class);
        MongoCollection<Document> collection = mongoClient.getDatabase("test").getCollection("pojos");
        MongoCollection<Pojo> pojoMongoCollection = collection.withDocumentClass(Pojo.class);

        for (int i = 0; i < 10; i++) {
            pojoMongoCollection.insertOne(Pojo.builder().number(i).build());
        }
        // this will throw an exception during decoding, but this exception should be caught and null will be returned
        collection.insertOne(
                new Document("number", 55)
                        .append("integerList", new ArrayList<>())
                        .append("pojoList", Arrays.asList(new Document("number", 24), new Document("number", "fail"))));


        for (int i = 0; i < 10; i++) {
            pojoMongoCollection.insertOne(Pojo.builder().number(i).build());
        }

        Assert.assertEquals(21, pojoMongoCollection.countDocuments());


        int i = 0;
        for (Pojo pojo : pojoMongoCollection.find()) {
            LOGGER.debug("Found {} : {}", i++, pojo);
        }

    }
}