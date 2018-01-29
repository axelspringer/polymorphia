package de.bild.codec;

import com.mongodb.MongoClient;
import de.bild.codec.annotations.PreSave;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonMode;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.junit.Assert;
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
import java.util.Objects;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PreSaveHookTest.class)
@ComponentScan(basePackages = "de.bild")
public class PreSaveHookTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreSaveHookTest.class);


    static final String STRING = "Hi, this is a String !§$%&/()\"äüöß";
    static final String MODIFIED_STRING = "prehook was active";

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder().register(PreSaveHookTest.class).build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

    static class BasePojo {
        String aString;
        @PreSave
        void testPreSaveHook() {
            aString = MODIFIED_STRING;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BasePojo basePojo = (BasePojo) o;
            return Objects.equals(aString, basePojo.aString);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aString);
        }
    }

    @Test
    public void basicTest() {
        BasePojo basePojo = new BasePojo();
        basePojo.aString = STRING;

        Codec<BasePojo> primitivePojoCodec = codecRegistry.get(BasePojo.class);

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        primitivePojoCodec.encode(writer, basePojo, EncoderContext.builder().build());
        LOGGER.info("The encoded json looks like: {}", stringWriter);

        BasePojo readBasePojo = primitivePojoCodec.decode(new JsonReader(stringWriter.toString()), DecoderContext.builder().build());
        // assert that the modified version was actually written to the database
        Assert.assertEquals(basePojo, readBasePojo);
        Assert.assertEquals(MODIFIED_STRING, readBasePojo.aString);
    }
}