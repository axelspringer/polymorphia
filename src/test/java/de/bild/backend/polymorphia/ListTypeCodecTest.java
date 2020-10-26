package de.bild.backend.polymorphia;

import com.mongodb.MongoClientSettings;
import de.bild.codec.BasicReflectionCodec;
import de.bild.codec.DelegatingCodec;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.SetTypeCodec;
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
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;


public class ListTypeCodecTest extends AbstractTest {

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder().register(ListTypeCodecTest.class).build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry()
            );
        }
    }

    static class ListOfStrings extends ArrayList<String> {
    }

    static class DecodingPojo {
        ListOfStrings someList;
    }

    static class EncodingPojo {
        List<String> someList;
    }


    static class SetPojo {
        SortedSet<Integer> integerSortedSet;
        Set<Integer> integerSet;
    }

    /**
     * Testing if List<String> can be decoded into a
     */
    @Test
    public void testResilience() {

        Codec<EncodingPojo> encodingPojoCodec = codecRegistry.get(EncodingPojo.class);
        Codec<DecodingPojo> decodingPojoCodec = codecRegistry.get(DecodingPojo.class);

        EncodingPojo encodingPojo = new EncodingPojo();
        encodingPojo.someList = new ArrayList<>();
        encodingPojo.someList.add("string1");
        encodingPojo.someList.add("string2");
        encodingPojo.someList.add("string3");


        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        encodingPojoCodec.encode(writer, encodingPojo, EncoderContext.builder().build());
        System.out.println(stringWriter.toString());

        DecodingPojo decodingPojo = decodingPojoCodec.decode(new JsonReader(stringWriter.toString()), DecoderContext.builder().build());
        Assert.assertNotNull(decodingPojo.someList);
        assertThat(decodingPojo.someList, instanceOf(ListOfStrings.class));
    }

    @Test
    public void testDifferentTypes() {
        Codec<SetPojo> setPojoCodec = codecRegistry.get(SetPojo.class);

        if (setPojoCodec instanceof DelegatingCodec) {
            setPojoCodec = ((DelegatingCodec) setPojoCodec).unWrapRecursively();
        }
        Assert.assertTrue(setPojoCodec instanceof BasicReflectionCodec);
        BasicReflectionCodec<SetPojo> basicReflectionCodec = (BasicReflectionCodec<SetPojo>) setPojoCodec;
        Codec integerSortedSetCodec = basicReflectionCodec.getMappedField("integerSortedSet").getCodec();
        if (integerSortedSetCodec instanceof DelegatingCodec) {
            integerSortedSetCodec = ((DelegatingCodec)integerSortedSetCodec).unWrapRecursively();
        }


        Constructor<Set<Integer>> constructor = (Constructor<Set<Integer>>)ReflectionTestUtils.getField(((SetTypeCodec<Set<Integer>, Integer>) integerSortedSetCodec), "defaultConstructor");
        Assert.assertTrue(TreeSet.class.equals(constructor.getDeclaringClass()));
    }
}
