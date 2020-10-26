package de.bild.backend.polymorphia.objectmodel;

import com.mongodb.MongoClientSettings;
import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.objectmodel.model.*;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.PolymorphicCodec;
import lombok.*;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonMode;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;

public class AnyThingTest extends AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnyThingTest.class);


    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    // since NonModelThingProvidingPolymorphicCodecCodec is a polymorphic codec, encoded entities within polymorphic structures won't need an additional data-wrapping!!
                    CodecRegistries.fromCodecs(new NonModelThingProvidingPolymorphicCodecCodec()),
                    CodecRegistries.fromCodecs(new NonModelThingProvidingStandardCodecCodec()),
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder()
                                    .register(Object.class)
                                    .register(String.class)
                                    .register(Integer.class)
                                    .register(Float.class)
                                    .register(NonModelThingProvidingPolymorphicCodec.class)
                                    .register(NonModelThingProvidingStandardCodec.class)
                                    .register(Pojo.class.getPackage().getName())
                                    //.ignoreTypesMatchingClassNamePredicate(className -> className.contains("$CaseInsensitiveComparator"))
                                    //.ignoreClasses(Integer.class)
                                    .build()
                    )
            );
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    @ToString
    @EqualsAndHashCode
    @Builder
    public static class NonModelThingProvidingStandardCodec {
        int anInt;
    }
    static class NonModelThingProvidingStandardCodecCodec implements Codec<NonModelThingProvidingStandardCodec> {
        @Override
        public NonModelThingProvidingStandardCodec decode(BsonReader reader, DecoderContext decoderContext) {
            NonModelThingProvidingStandardCodec instance = new NonModelThingProvidingStandardCodec();
            //reader.readName("standardCodecFieldName");
            instance.setAnInt(reader.readInt32());
            return null;
        }

        @Override
        public void encode(BsonWriter writer, NonModelThingProvidingStandardCodec value, EncoderContext encoderContext) {
            //writer.writeName("standardCodecFieldName");
            writer.writeInt32(value.anInt);
        }

        @Override
        public Class<NonModelThingProvidingStandardCodec> getEncoderClass() {
            return NonModelThingProvidingStandardCodec.class;
        }
    }


    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    @ToString
    @EqualsAndHashCode
    @Builder
    public static class NonModelThingProvidingPolymorphicCodec {
        int anInt;
    }

    static class NonModelThingProvidingPolymorphicCodecCodec implements PolymorphicCodec<NonModelThingProvidingPolymorphicCodec> {
        @Override
        public NonModelThingProvidingPolymorphicCodec decodeFields(BsonReader reader, DecoderContext decoderContext, NonModelThingProvidingPolymorphicCodec instance) {
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if (name.equals("wasWrittenBySpecializedCodec")) {
                    instance.anInt = reader.readInt32();
                } else {
                    reader.skipValue();
                }
            }
            return instance;
        }

        @Override
        public void encodeFields(BsonWriter writer, NonModelThingProvidingPolymorphicCodec instance, EncoderContext encoderContext) {
            writer.writeName("wasWrittenBySpecializedCodec");
            writer.writeInt32(instance.anInt);
        }

        @Override
        public NonModelThingProvidingPolymorphicCodec newInstance() {
            return new NonModelThingProvidingPolymorphicCodec();
        }

        @Override
        public void verifyFieldsNotNamedLikeAnyDiscriminatorKey(Set<String> discriminatorKeys) throws IllegalArgumentException {
        }

        @Override
        public Class<NonModelThingProvidingPolymorphicCodec> getEncoderClass() {
            return NonModelThingProvidingPolymorphicCodec.class;
        }
    }



    @AllArgsConstructor
    private static class NonRegisteredExtendingRegisteredClass extends RandomObject {
        String notToBeEncoded;
    }

    @Test
    public void someTest() throws JSONException {
        Codec<Pojo> codec = codecRegistry.get(Pojo.class);

        Pojo pojo = Pojo.builder()
                .aString("Test")
                .aFloat(222.44f)
                .nonModelThingProvidingPolymorphicCodec(new NonModelThingProvidingPolymorphicCodec(44))
                .nonModelThingProvidingStandardCodec(new NonModelThingProvidingStandardCodec(98))
                .anotherEnum(AnotherEnum.XYZ)
                .onlyOneImplementationInterface(AnotherEnum.XYZ)
                .niceEnum(NiceEnum.TYPE_A)
                .someInterface(AnotherEnum.XYZ)
                .objects(Arrays.asList(
                        "Any Object",
                        Double.valueOf(212d),
                        new Integer(22),
                        new RandomObject(),
                        null,
                        Float.valueOf(44455.5f),
                        NiceEnum.TYPE_A,
                        new NonModelThingProvidingPolymorphicCodec(815),
                        new SomeInterface() {},
                        new NonModelThingProvidingStandardCodec(443),
                        new NonRegisteredExtendingRegisteredClass("Do not persist this property")
                ))
                .niceEnum2(NiceEnum.TYPE_B)
                .someInterface2(NiceEnum.TYPE_A)
                .yetAnotherNonModelThingProvidingPolymorphicCodec(new NonModelThingProvidingPolymorphicCodec(345678))
                .yetAnothernNonModelThingProvidingStandardCodec(new NonModelThingProvidingStandardCodec(777))
                .build();

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

        JSONAssert.assertEquals("{\n" +
                "  \"_t\" : \"Pojo\",\n" +
                "  \"aString\" : \"Test\",\n" +
                "  \"aFloat\" : {\n" +
                "    \"$numberDouble\" : \"222.44000244140625\"\n" +
                "  },\n" +
                "  \"nonModelThingProvidingPolymorphicCodec\" : {\n" +
                "    \"wasWrittenBySpecializedCodec\" : {\n" +
                "      \"$numberInt\" : \"44\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"nonModelThingProvidingStandardCodec\" : {\n" +
                "    \"$numberInt\" : \"98\"\n" +
                "  },\n" +
                "  \"anotherEnum\" : \"XYZ\",\n" +
                "  \"onlyOneImplementationInterface\" : {\n" +
                "    \"_t\" : \"AnotherEnum\",\n" +
                "    \"data\" : \"XYZ\"\n" +
                "  },\n" +
                "  \"niceEnum\" : \"TYPE_A\",\n" +
                "  \"someInterface\" : {\n" +
                "    \"_t\" : \"AnotherEnum\",\n" +
                "    \"data\" : \"XYZ\"\n" +
                "  },\n" +
                "  \"objects\" : [{\n" +
                "      \"_t\" : \"String\",\n" +
                "      \"data\" : \"Any Object\"\n" +
                "    }, {\n" +
                "      \"_t\" : \"Object\"\n" +
                "    }, {\n" +
                "      \"_t\" : \"Integer\",\n" +
                "      \"data\" : {\n" +
                "        \"$numberInt\" : \"22\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"_t\" : \"RandomObject\",\n" +
                "      \"anInt\" : {\n" +
                "        \"$numberInt\" : \"33\"\n" +
                "      }\n" +
                "    }, null, {\n" +
                "      \"_t\" : \"Float\",\n" +
                "      \"data\" : {\n" +
                "        \"$numberDouble\" : \"44455.5\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"_t\" : \"NiceEnum\",\n" +
                "      \"data\" : \"TYPE_A\"\n" +
                "    }, {\n" +
                "      \"_t\" : \"NonModelThingProvidingPolymorphicCodec\",\n" +
                "      \"wasWrittenBySpecializedCodec\" : {\n" +
                "        \"$numberInt\" : \"815\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"_t\" : \"Object\"\n" +
                "    }, {\n" +
                "      \"_t\" : \"NonModelThingProvidingStandardCodec\",\n" +
                "      \"data\" : {\n" +
                "        \"$numberInt\" : \"443\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"_t\" : \"RandomObject\",\n" +
                "      \"anInt\" : {\n" +
                "        \"$numberInt\" : \"33\"\n" +
                "      }\n" +
                "    }],\n" +
                "  \"niceEnum2\" : \"TYPE_B\",\n" +
                "  \"someInterface2\" : {\n" +
                "    \"_t\" : \"NiceEnum\",\n" +
                "    \"data\" : \"TYPE_A\"\n" +
                "  },\n" +
                "  \"yetAnotherNonModelThingProvidingPolymorphicCodec\" : {\n" +
                "    \"wasWrittenBySpecializedCodec\" : {\n" +
                "      \"$numberInt\" : \"345678\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"yetAnothernNonModelThingProvidingStandardCodec\" : {\n" +
                "    \"$numberInt\" : \"777\"\n" +
                "  }\n" +
                "}\n", stringWriter.toString(), true);
    }
}