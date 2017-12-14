package de.bild.codec.idexternal;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.IdGenerator;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.idexternal.model.Pojo;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
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

import java.util.Random;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ExternalIdCodecProviderTest.class)
@EnableAutoConfiguration
@ComponentScan(basePackages = "de.bild")
public class ExternalIdCodecProviderTest {
    private static final Random RANDOM = new Random();

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            new CustomIdCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }


    @SuppressWarnings("unchecked")
    public static class CustomIdCodecProvider implements CodecProvider {
        @Override
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry codecRegistry) {
            if (CustomId.class.isAssignableFrom(clazz)) {
                return (Codec<T>)new CustomIdCodec();
            }
            return null;
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

    /**
     * using WrongCustomIdGenerator generates a wrong customId and therefore generates an exception
     * use at {@link Pojo#id} -  @Id(collectible = true, value = ExternalIdCodecProviderTest.WrongCustomIdGenerator.class)
     */
    public static class WrongCustomIdGenerator implements IdGenerator<Object> {
        @Override
        public Object generate() {
            return "SomeRandomWrongCustomId";
        }
    }

    public static class CustomIdGenerator implements IdGenerator<CustomId> {
        @Override
        public CustomId generate() {
            return CustomId.builder().aStringProperty("IdString: " + RANDOM.nextInt()).build();
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

    }
}
