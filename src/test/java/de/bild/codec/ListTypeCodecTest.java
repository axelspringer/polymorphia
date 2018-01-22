package de.bild.codec;

import com.mongodb.MongoClient;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ListTypeCodecTest.class)
@ComponentScan(basePackages = "de.bild")
public class ListTypeCodecTest {

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    MongoClient.getDefaultCodecRegistry(),
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder().register(ListTypeCodecTest.class).build()
                    )
            );
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

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
        JsonWriter writer = new JsonWriter(stringWriter, new JsonWriterSettings(true));
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
        Constructor<Set<Integer>> constructor = ((SetTypeCodec<Set<Integer>, Integer>) integerSortedSetCodec).defaultConstructor;
        Assert.assertTrue(TreeSet.class.equals(constructor.getDeclaringClass()));
    }
}
