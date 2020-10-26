package de.bild.backend.polymorphia;


import com.jayway.jsonpath.JsonPath;
import com.mongodb.MongoClientSettings;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.annotations.Discriminator;
import de.bild.codec.annotations.Polymorphic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.StringWriter;
import java.util.Map;

public class PolymorphicDetectionTest extends AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolymorphicDetectionTest.class);

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(PolymorphicDetectionTest.class).build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }

    /************  DOMAIN MODEL START ************/

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Polymorphic
    static class Pojo {
        String aString;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    static class PojoNonPolymorphic {
        String aString;
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Discriminator("ANiceOne")
    static class PojoWithDiscriminatorAnnotation {
        String aString;
    }



    interface XYZ {}

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    static class PojoImplementingInterface implements XYZ {
        String aString;
    }


    static class PojoBase {
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    static class PojoExtendingBase extends PojoBase {
        String aString;
    }

    /************  DOMAIN MODEL END ************/



    protected Map<String, Object> encode(Object object) {
        Codec codec = codecRegistry.get(object.getClass());

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        codec.encode(writer, object, EncoderContext.builder().build());
        LOGGER.info("The encoded json looks like: {}", stringWriter);

        return JsonPath.parse(stringWriter.toString()).read("$");
    }

    @Test
    public void polymorphiaTest()  {
        Assert.assertTrue("PojoExtendingBase".equals(encode(PojoExtendingBase.builder().aString("PojoExtendingBase").build()).get("_t")));
        Assert.assertTrue("PojoImplementingInterface".equals(encode(PojoImplementingInterface.builder().aString("PojoImplementingInterface").build()).get("_t")));
        Assert.assertTrue("ANiceOne".equals(encode(PojoWithDiscriminatorAnnotation.builder().aString("discriminatorannotaton").build()).get("_t")));
        Assert.assertTrue("Pojo".equals(encode(Pojo.builder().aString("write discriminator").build()).get("_t")));
        Assert.assertTrue(encode(PojoNonPolymorphic.builder().aString("no discriminator please").build()).get("_t") == null);
    }

}