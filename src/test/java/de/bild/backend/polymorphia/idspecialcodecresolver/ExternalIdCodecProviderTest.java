package de.bild.backend.polymorphia.idspecialcodecresolver;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.idspecialcodecresolver.model.CustomId;
import de.bild.backend.polymorphia.idspecialcodecresolver.model.Pojo;
import de.bild.codec.*;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
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
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .registerCodecResolver((CodecResolver) (type, typeCodecRegistry, codecConfiguration) -> {
                                        if (TypeUtils.isAssignable(type, CustomId.class)) {
                                            return new CustomIdCodec((Class<CustomId>)type, typeCodecRegistry, codecConfiguration);
                                        }
                                        return null;
                                    }).build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }



    public static class CustomIdGenerator implements IdGenerator<CustomId> {
        @Override
        public CustomId generate() {
            return CustomId.builder().aStringProperty("IdString: " + RANDOM.nextInt()).build();
        }
    }

    static class CustomIdCodec<T extends CustomId> extends BasicReflectionCodec<T> {
        public CustomIdCodec(Class<T> type, TypeCodecRegistry typeCodecRegistry, CodecConfiguration codecConfiguration) {
            super(type, typeCodecRegistry, codecConfiguration);
        }

        @Override
        public T decodeFields(BsonReader reader, DecoderContext decoderContext, T instance) {
            // do some custom stuff here. as an example have a look at super.decodeFields(reader, decoderContext, instance);
            return super.decodeFields(reader, decoderContext, instance);
        }

        @Override
        public void encodeFields(BsonWriter writer, T instance, EncoderContext encoderContext) {
            // do some custom stuff here. as an example have a look at super.encodeFields(writer, instance, encoderContext)
            super.encodeFields(writer, instance, encoderContext);
        }
    }


    @Test
    public void testExternalId() {
        Pojo pojo = Pojo.builder().id(null).someOtherProperty("some nice string").build();
        MongoCollection<Pojo> collection = getCollection(Pojo.class);
        collection.insertOne(pojo);

        Pojo readPojo = collection.find(Filters.eq("_id", pojo.getId())).first();

    }
}
