package de.bild.codec.objectmodel;


import com.mongodb.MongoClient;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.objectmodel.model.NiceEnum;
import de.bild.codec.objectmodel.model.Pojo;
import de.bild.codec.objectmodel.model.RandomObject;
import de.bild.codec.objectmodel.model.SomeInterface;
import lombok.AllArgsConstructor;
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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AnyThingTest.class)
@ComponentScan(basePackages = "de.bild")
public class AnyThingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnyThingTest.class);


    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    MongoClient.getDefaultCodecRegistry(),
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register(Object.class)
                                    .register(String.class)
                                    .register(Integer.class)
                                    .register(Pojo.class.getPackage().getName())
                                    //.ignoreTypesMatchingClassNamePredicate(className -> className.contains("$CaseInsensitiveComparator"))
                                    //.ignoreClasses(Integer.class)
                                    .build()
                    )
            );
        }
    }

    @Autowired
    CodecRegistry codecRegistry;


    @AllArgsConstructor
    private static class NonRegisteredExtendingRegisteredClass extends RandomObject {
        String notToBeEncoded;
    }

    @Test
    public void someTest() {
        Codec<Pojo> codec = codecRegistry.get(Pojo.class);

        Pojo pojo = new Pojo(
                "Test",
                222.44f,
                Arrays.asList(
                        "Any Object",
                        Double.valueOf(212d),
                        new Integer(22),
                        new RandomObject(),
                        null,
                        NiceEnum.TYPE_A,
                        new SomeInterface() {},
                        new NonRegisteredExtendingRegisteredClass("Do not persist this property")
                ));

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        codec.encode(writer, pojo, EncoderContext.builder().build());
        LOGGER.info("The encoded json looks like: {}", stringWriter);
        Pojo readPojo = codec.decode(new JsonReader(stringWriter.toString()), DecoderContext.builder().build());

        Assert.assertNotEquals(pojo, readPojo);
        Assert.assertEquals(pojo.getAFloat(), readPojo.getAFloat());
        Assert.assertTrue(readPojo.getObjects().get(0).getClass() == String.class);
        Assert.assertTrue(readPojo.getObjects().get(1).getClass() == Object.class); // Double is not part of domain model, bt Object is!!
        Assert.assertTrue(readPojo.getObjects().get(2).getClass() == Integer.class);

    }
}