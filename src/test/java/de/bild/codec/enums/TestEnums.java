package de.bild.codec.enums;

import com.mongodb.MongoClient;
import de.bild.codec.BaseTest;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.enums.model.Displayable;
import de.bild.codec.enums.model.MyEnumType;
import de.bild.codec.enums.model.Pojo;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestEnums.class)
@ComponentScan(basePackages = "de.bild")
public class TestEnums {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestEnums.class);

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            //new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register("de.bild.codec.enums.model")
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }


    @Autowired
    CodecRegistry codecRegistry;


    /**
     * TODO The following test should instantiate a codec for given interface
     * Interestingly the interface is being implemented by  enums
     */
    @Test
    @Ignore // remove me!
    public void testEnums() {
        Codec<Displayable> pojoCodec = codecRegistry.get(Displayable.class);
    }
 }