package de.bild.codec.idspecialcodecresolver;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.codec.*;
import de.bild.codec.idspecialcodecresolver.model.CustomId;
import de.bild.codec.idspecialcodecresolver.model.Pojo;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
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
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .registerCodecResolver((CodecResolver) (type, typeCodecRegistry) -> {
                                        if (TypeUtils.isAssignable(type, CustomId.class)) {
                                            return new CustomIdCodec((Class<CustomId>)type, typeCodecRegistry);
                                        }
                                        return null;
                                    }).build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }



    public static class CustomIdGenerator implements IdGenerator<CustomId> {
        @Override
        public CustomId generate() {
            return CustomId.builder().aStringProperty("IdString: " + RANDOM.nextInt()).build();
        }
    }

    static class CustomIdCodec<T extends CustomId> extends BasicReflectionCodec<T> {
        public CustomIdCodec(Class<T> type, TypeCodecRegistry typeCodecRegistry) {
            super(type, typeCodecRegistry);
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

        @Override
        public T defaultInstance() {
            return null;
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
