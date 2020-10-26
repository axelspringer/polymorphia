package de.bild.backend.polymorphia.enums;

import com.mongodb.MongoClientSettings;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.enums.model.EnumA;
import de.bild.backend.polymorphia.enums.model.EnumB;
import de.bild.backend.polymorphia.enums.model.LovelyDisplayable;
import de.bild.backend.polymorphia.enums.model.Pojo;
import de.bild.codec.PojoCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonMode;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.StringWriter;
import java.util.Arrays;

public class TestEnums extends AbstractTest {

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(Pojo.class.getPackage().getName())
                                    .build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }



    @Test
    public void testEnums() {
        Codec<Pojo> pojoCodec = codecRegistry.get(Pojo.class);

        LovelyDisplayable lovelyDisplayable = LovelyDisplayable.builder().identityProperty("foo").build();

        Pojo pojo = Pojo.builder()
                .simpleEnumProperty(EnumA.TYPE1)
                .displayable(Arrays.asList(EnumB.TYPE1, EnumA.TYPE1, EnumA.TYPE3, lovelyDisplayable))
                .build();

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        pojoCodec.encode(writer, pojo, EncoderContext.builder().build());
        System.out.println(stringWriter.toString());

        Pojo decodedPojo = pojoCodec.decode(new JsonReader(stringWriter.toString()), DecoderContext.builder().build());

        MatcherAssert.assertThat(decodedPojo.getDisplayable(),
                IsIterableContainingInOrder.contains(EnumB.TYPE1, EnumA.TYPE1, EnumA.TYPE3, lovelyDisplayable));

    }
 }