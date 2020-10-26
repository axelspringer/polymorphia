package de.bild.backend.polymorphia;

import com.mongodb.MongoClientSettings;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.annotations.DecodeUndefinedHandlingStrategy;
import de.bild.codec.annotations.EncodeNullHandlingStrategy;
import de.bild.codec.annotations.EncodeNulls;
import lombok.EqualsAndHashCode;
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
import org.json.JSONException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static org.hamcrest.Matchers.empty;

public class NullHandlingTest extends AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NullHandlingTest.class);


    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(NullHandlingTest.class)
                                    .encodeNulls(false)
                                    .decodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy.KEEP_POJO_DEFAULT)
                                    .encodeNullHandlingStrategy(EncodeNullHandlingStrategy.Strategy.KEEP_NULL)
                                    .build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }

    interface SomeInterface {}

    @EncodeNulls(true)
    @EqualsAndHashCode
    @DecodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy.CODEC)
    static class PojoProperty implements SomeInterface {
        SortedSet<String> sortedSet;
    }

    @EqualsAndHashCode
    @EncodeNulls(false)
    static class BasePojo {
        String aString;

        @EncodeNulls(false)
        List nonSpecializedList = Arrays.asList("www", "mobile");

        @EncodeNulls(false)
        List nonSpecializedListNull = null;

        @EncodeNulls(false)
        List<PojoProperty> encodeNullsFalse;

        @EncodeNulls(true)
        List<PojoProperty> encodeNullsTrue;

        @EncodeNullHandlingStrategy(EncodeNullHandlingStrategy.Strategy.CODEC)
        List<PojoProperty> encodeNullHandlingStrategy_CODEC;

        @DecodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy.CODEC)
        @EncodeNulls(false) //EncodeNullHandlingStrategy.Strategy.KEEP_NULL
        List<PojoProperty> encodeNullsFalseDecodeUndefined_CODEC = Arrays.asList(new PojoProperty(), new PojoProperty());

        @DecodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy.KEEP_POJO_DEFAULT)
        @EncodeNulls(false)//EncodeNullHandlingStrategy.Strategy.KEEP_NULL
        List<PojoProperty> encodeNullsFalseDecodeUndefined_KEEP_POJO_DEFAULT = Arrays.asList(new PojoProperty());


        @DecodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy.KEEP_POJO_DEFAULT)
        @EncodeNulls
        List<PojoProperty> encodeNullsShouldDecodeToNull = new ArrayList<>(Arrays.asList(null, new PojoProperty(), null, null));

    }

    @Test
    public void basicTest() throws JSONException {
        BasePojo basePojo = new BasePojo();

        basePojo.encodeNullsFalseDecodeUndefined_CODEC = null; // encode to undefined
        basePojo.encodeNullsFalseDecodeUndefined_KEEP_POJO_DEFAULT = null; // encode with null value set
        basePojo.encodeNullsShouldDecodeToNull = null; // encode with null value set

        Codec<BasePojo> primitivePojoCodec = codecRegistry.get(BasePojo.class);

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        primitivePojoCodec.encode(writer, basePojo, EncoderContext.builder().build());
        LOGGER.info("The encoded json looks like: {}", stringWriter);

        BasePojo readBasePojo = primitivePojoCodec.decode(new JsonReader(stringWriter.toString()), DecoderContext.builder().build());

        JSONAssert.assertEquals("{\n" +
                "  \"encodeNullsTrue\" : null,\n" +
                "  \"nonSpecializedList\" : [\"www\", \"mobile\"],\n" +
                "  \"encodeNullHandlingStrategy_CODEC\" : [],\n" +
                "  \"encodeNullsShouldDecodeToNull\" : null\n" +
                "}", stringWriter.toString(), true);


        Assert.assertNull(readBasePojo.encodeNullsFalse);
        Assert.assertNull(readBasePojo.aString);
        Assert.assertNull(readBasePojo.encodeNullsTrue);

        MatcherAssert.assertThat(readBasePojo.encodeNullHandlingStrategy_CODEC, empty());
        MatcherAssert.assertThat(readBasePojo.encodeNullsFalseDecodeUndefined_CODEC, empty());
        Assert.assertEquals(readBasePojo.encodeNullsFalseDecodeUndefined_KEEP_POJO_DEFAULT, Arrays.asList(new PojoProperty()));
        Assert.assertNull(readBasePojo.encodeNullsShouldDecodeToNull);
    }
}