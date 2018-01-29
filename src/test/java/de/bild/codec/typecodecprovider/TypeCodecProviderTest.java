package de.bild.codec.typecodecprovider;

import com.mongodb.MongoClient;
import de.bild.codec.*;
import lombok.*;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
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
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TypeCodecProviderTest.class)
@ComponentScan(basePackages = "de.bild")
public class TypeCodecProviderTest {

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register(TypeCodecProviderTest.class)
                                    .register(new CustomTypeCodecProvider(), new SetOfStringTypeCodecProvider())
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Autowired
    CodecRegistry codecRegistry;


    public static class CustomTypeCodecProvider implements TypeCodecProvider {
        @Override
        public <T> Codec<T> get(Type type, TypeCodecRegistry typeCodecRegistry) {
            if (TypeUtils.isAssignable(type, CustomType.class)) {
                return (Codec<T>) new CustomTypeCodec((ParameterizedType) type, typeCodecRegistry);
            }
            return null;
        }
    }

    public static class SetOfStringTypeCodecProvider implements TypeCodecProvider {
        static final Type setOfStringsType;
        static {
            try {
                setOfStringsType = Pojo.class.getDeclaredField("strings").getGenericType();
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Could not get type of field strings in Pojo.class");
            }
        }
        @Override
        public <T> Codec<T> get(Type type, TypeCodecRegistry typeCodecRegistry) {
            if (TypeUtils.isAssignable(type, setOfStringsType)) {
                return (Codec<T>) new SetOfStringCodec((ParameterizedType) type, typeCodecRegistry);
            }
            return null;
        }
    }

    /**
     * A codec that specifically handles Sets of Strings
     */
    public static class SetOfStringCodec implements Codec<Set<String>>{
        final Class<Set<String>> clazz;

        public SetOfStringCodec(ParameterizedType type, TypeCodecRegistry typeCodecRegistry) {
            this.clazz = ReflectionHelper.extractRawClass(type);

        }

        @Override
        public Set<String> decode(BsonReader reader, DecoderContext decoderContext) {
            Set<String> stringSet = new HashSet<>();
            String[] split = reader.readString().split("#");
            stringSet.addAll(Arrays.asList(split));
            return stringSet;
        }

        @Override
        public void encode(BsonWriter writer, Set<String> value, EncoderContext encoderContext) {
            StringBuilder sb = new StringBuilder();
            for (String s : value) {
                sb.append(s).append('#');
            }
            writer.writeString(sb.toString());
        }

        @Override
        public Class<Set<String>> getEncoderClass() {
            return clazz;
        }
    }


    public static class CustomTypeCodec<T> implements Codec<CustomType<T>> {
        final Codec<String> nameCodec;
        final Codec<List<T>> listCodec;
        final Codec<InnerType<T>> innerTypeCodec;
        final Class<CustomType<T>> clazz;

        public CustomTypeCodec(ParameterizedType type, TypeCodecRegistry typeCodecRegistry) {
            Type typeParameter = type.getActualTypeArguments()[0];
            this.nameCodec = typeCodecRegistry.getCodec(String.class);
            this.listCodec = typeCodecRegistry.getCodec(TypeUtils.parameterize(ArrayList.class, typeParameter));
            this.innerTypeCodec = typeCodecRegistry.getCodec(ReflectionHelper.getDeclaredAndInheritedFieldTypePair(type, "innerType").getRealType());
            this.clazz = ReflectionHelper.extractRawClass(type);
        }

        @Override
        public CustomType decode(BsonReader reader, DecoderContext decoderContext) {
            reader.readStartDocument();
            reader.readName("name");
            String name = nameCodec.decode(reader, decoderContext);

            reader.readName("list");
            List<T> stringArrayList = listCodec.decode(reader, decoderContext);

            reader.readName("innerType");
            InnerType<T> innerType = innerTypeCodec.decode(reader, decoderContext);

            reader.readEndDocument();
            CustomType<T> customType = new CustomType(name);
            customType.setInnerType(innerType);
            customType.addAll(stringArrayList);

            return customType;
        }

        @Override
        public void encode(BsonWriter writer, CustomType<T> value, EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeName("name");
            nameCodec.encode(writer, value.getANameForTheList(), encoderContext);

            writer.writeName("list");
            listCodec.encode(writer, value, encoderContext);

            writer.writeName("innerType");
            innerTypeCodec.encode(writer, value.getInnerType(), encoderContext);
            writer.writeEndDocument();
        }

        @Override
        public Class<CustomType<T>> getEncoderClass() {
            return clazz;
        }
    }

    @NoArgsConstructor
    public static class InnerType<T> {
        T someThing;

        public InnerType(T someThing) {
            this.someThing = someThing;
        }
    }

    @Setter
    @Getter
    public static class CustomType<T> extends ArrayList<T> {
        String aNameForTheList;
        InnerType<T> innerType;
        T type;

        public CustomType(String aNameForTheList) {
            this.aNameForTheList = aNameForTheList;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("CustomType{");
            sb.append("aNameForTheList='").append(aNameForTheList).append('\'');
            sb.append("list='" + super.toString()).append('\'');
            sb.append(", innerType=").append(innerType);
            sb.append(", type=").append(type);
            sb.append('}');
            return sb.toString();
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Pojo {
        String name;
        Set<String> strings; // should be serialized as "value1#value2#value3#" instead of a json array
        CustomType<String> customTypeString;
        CustomType<Integer> customTypeInteger;
    }

    @Test
    public void testDifferentTypes() {
        Codec<Pojo> pojoCodec = codecRegistry.get(Pojo.class);

        CustomType<String> customTypeString = new CustomType("A custom string type");
        String[] strings = {"a", "nice", "list", "of", "strings"};
        customTypeString.addAll(Arrays.asList(strings));
        customTypeString.setInnerType(new InnerType<>("String value"));


        CustomType<Integer> customTypeInteger = new CustomType("A custom integer type");
        Integer[] integers = {1, 42, 66, 89};
        customTypeInteger.addAll(Arrays.asList(integers));
        customTypeInteger.setInnerType(new InnerType<>(11234567));


        String[] stringsForSet = {"Tee", "Brot", "Butter"};
        Pojo pojo = Pojo.builder()
                .customTypeString(customTypeString)
                .customTypeInteger(customTypeInteger)
                .name("aName")
                .strings(new HashSet<>(Arrays.asList(stringsForSet)))
                .build();

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        pojoCodec.encode(writer, pojo, EncoderContext.builder().build());
        System.out.println(stringWriter.toString());

        Pojo decodedPojo = pojoCodec.decode(new JsonReader(stringWriter.toString()), DecoderContext.builder().build());

        System.out.println(decodedPojo);

        Assert.assertNotNull(decodedPojo);
        MatcherAssert.assertThat(decodedPojo.getCustomTypeString(), CoreMatchers.hasItems(strings));
        MatcherAssert.assertThat(decodedPojo.getCustomTypeInteger(), CoreMatchers.hasItems(integers));
        MatcherAssert.assertThat(decodedPojo.getStrings(), CoreMatchers.hasItems(stringsForSet));
    }
}
