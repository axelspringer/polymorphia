package de.bild.backend.polymorphia.dividedclassmodel;

import com.mongodb.MongoClientSettings;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.dividedclassmodel.basemodel.BasePojo;
import de.bild.backend.polymorphia.dividedclassmodel.nonmodel.NonModelPojo;
import de.bild.backend.polymorphia.dividedclassmodel.submodel.SubPojoWithinModel;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.PolymorphicReflectionCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class DividedModelTest extends AbstractTest {

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(BasePojo.class.getPackage().getName())
                                    .register(SubPojoWithinModel.class.getPackage().getName())
                                    .build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }

    @Test
    public void testModelClasses() {
        Codec<SubPojoWithinModel> subPojoWithinModelCodec = codecRegistry.get(SubPojoWithinModel.class);
        Assert.assertTrue(subPojoWithinModelCodec instanceof PolymorphicReflectionCodec);
        Codec<NonModelPojo> nonModelPojoCodec = codecRegistry.get(NonModelPojo.class);
        // interestingly nonModelPojoCodec will "upcast" NonModelPojo to BasePojo and a codec for BasePojo will be created
        Assert.assertTrue(nonModelPojoCodec instanceof PolymorphicReflectionCodec);
        //todo : What else do we need to check here?
    }
}
