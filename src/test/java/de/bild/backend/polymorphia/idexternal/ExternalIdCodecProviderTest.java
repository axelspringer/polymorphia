package de.bild.backend.polymorphia.idexternal;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.IgnoreAnnotation;
import de.bild.backend.polymorphia.idexternal.model.CustomId;
import de.bild.backend.polymorphia.idexternal.model.Pojo;
import de.bild.codec.IdGenerator;
import de.bild.codec.PojoCodecProvider;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


public class ExternalIdCodecProviderTest extends AbstractTest {
    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromCodecs(new CustomIdCodec()),
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .ignoreTypesAnnotatedWith(IgnoreAnnotation.class)
                                    .build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
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


    @Test
    public void testExternalId() {
        Pojo pojo = Pojo.builder().id(null).someOtherProperty("some nice string").build();
        MongoCollection<Pojo> collection = getCollection(Pojo.class);
        collection.insertOne(pojo);

        Pojo readPojo = collection.find(Filters.eq("_id", pojo.getId())).first();

        Assert.assertNotNull(readPojo);
    }
}
