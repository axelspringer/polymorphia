package de.bild.codec.dividedclassmodel;

import com.mongodb.MongoClient;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.PolymorphicReflectionCodec;
import de.bild.codec.dividedclassmodel.basemodel.BasePojo;
import de.bild.codec.dividedclassmodel.nonmodel.NonModelPojo;
import de.bild.codec.dividedclassmodel.submodel.SubPojoWithinModel;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DividedModelTest.class)
@ComponentScan(basePackages = "de.bild")
public class DividedModelTest {

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register(BasePojo.class.getPackage().getName())
                                    .register(SubPojoWithinModel.class.getPackage().getName())
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

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
