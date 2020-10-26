package de.bild.backend.polymorphia.idtypemismatch;


import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.idtypemismatch.model.CustomId;
import de.bild.backend.polymorphia.idtypemismatch.model.Pojo;
import de.bild.codec.IdGenerationException;
import de.bild.codec.IdGenerator;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.annotations.IgnoreType;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

public class ExternalIdCodecProviderTest extends AbstractTest {
    private static final Random RANDOM = new Random();

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new CustomIdCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .ignoreTypesAnnotatedWith(IgnoreType.class)
                                    .build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
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

    @Test
    public void testExternalId() {
        Assertions.assertThrows(IdGenerationException.class, () -> {
            Pojo pojo = Pojo.builder().id(null).someOtherProperty("some nice string").build();
            MongoCollection<Pojo> collection = mongoClient.getDatabase("test").getCollection("documents").withDocumentClass(Pojo.class);
            collection.insertOne(pojo);

            Pojo readPojo = collection.find(Filters.eq("_id", pojo.getId())).first();

            Assert.assertNotNull(readPojo);
        });

    }
}
