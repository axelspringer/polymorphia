package de.bild.codec.enums;

import com.mongodb.MongoClient;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.enums.model.EnumA;
import de.bild.codec.enums.model.EnumB;
import de.bild.codec.enums.model.LovelyDisplayable;
import de.bild.codec.enums.model.Pojo;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.StringWriter;
import java.util.Arrays;

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
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register("de.bild.codec.enums.model")
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }


    @Autowired
    CodecRegistry codecRegistry;

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