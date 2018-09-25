package de.bild.codec.idexternal;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.IdGenerator;
import de.bild.codec.IgnoreAnnotation;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.idexternal.model.CustomId;
import de.bild.codec.idexternal.model.Pojo;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Random;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ExternalIdCodecProviderTest.class)
@EnableAutoConfiguration
@ComponentScan(basePackages = "de.bild")
public class ExternalIdCodecProviderTest {
    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromCodecs(new CustomIdCodec()),
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .ignoreTypesAnnotatedWith(IgnoreAnnotation.class)
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }



    static class CustomIdCodec implements Codec<CustomId> {
        @Override
        public CustomId decode(BsonReader bsonReader, DecoderContext decoderContext) {
            return CustomId.builder().aStringProperty(bsonReader.readString()).build();
        }

        @Override
        public void encode(BsonWriter bsonWriter, CustomId customId, EncoderContext encoderContext) {
            bsonWriter.writeString(customId.getAStringProperty());
        }

        @Override
        public Class<CustomId> getEncoderClass() {
            return CustomId.class;
        }
    }

    public static class CustomIdGenerator implements IdGenerator<CustomId> {
        @Override
        public CustomId generate() {
            return CustomId.builder().aStringProperty(new ObjectId().toString()).build();
        }
    }


    @Autowired
    CodecRegistry codecRegistry;

    @Autowired
    private MongoClient mongoClient;

    @Test
    public void testExternalId() {
        Pojo pojo = Pojo.builder().id(null).someOtherProperty("some nice string").build();
        MongoCollection<Pojo> collection = mongoClient.getDatabase("test").getCollection("documents").withDocumentClass(Pojo.class);
        collection.insertOne(pojo);

        Pojo readPojo = collection.find(Filters.eq("_id", pojo.getId())).first();

        Assert.assertNotNull(readPojo);
    }
}
