package de.bild.codec;

import com.mongodb.MongoClient;
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
import java.util.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BaseTest.class)
@ComponentScan(basePackages = "de.bild")
public class BaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);


    static final String STRING = "Hi, this is a String !§$%&/()\"äüöß";
    static final int PRIMITIVE_INT = 42;
    static final byte PRIMITIVE_BYTE = 45;
    static final float PRIMITIVE_FLOAT = 12f;
    static final double PRIMITIVE_DOUBLE = -1234567898765.2134545d;
    static final char PRIMITIVE_CHAR = 'ä';
    static final short PRIMITIVE_SHORT = 77;
    static final long PRIMITIVE_LONG = 96136196L;

    static final Integer INTEGER = 22;
    static final Byte BYTE = 55;
    static final Float FLOAT = 1332.66f;
    static final Double DOUBLE = 2355.2134545d;
    static final Character CHARACTER = 'ü';
    static final Short SHORT = 34;
    static final Long LONG = 34435556L;

    static final String[] STRINGS = {"hallo", "hi", "hello", null, null};
    static final float[] PRIMITIVE_FLOATS = {123f, 456.789f};
    static final int[] PRIMITIVE_INTS = {1, 5, 78, 42, 22, -232, -55, -442};
    static final long[] PRIMITIVE_LONGS = {141L, 212L, -1133L, -131L};
    static final char[] PRIMITIVE_CHARS = {'a', 'ä', '@', '#', '\''};
    static final short[] PRIMITIVE_SHORTS = {255, 0x7FFF, 888};
    static final byte[] PRIMITIVE_BYTES = {12, -0x7F, 0x7F};
    static final byte[][] PRIMITIVE_BYTES_2 = {{12, -0x7F, 0x7F}, {2, 4, 55}};
    static final double[] PRIMITIVE_DOUBLES = {-0.0000001d, -13131.3d, 4e22, 222};

    static final Float[] FLOATS = {123f, 456.789f, null};
    static final Integer[] INTEGERS = {1, 5, null, 42, 22, -232, -55, -442};
    static final Long[] LONGS = {141L, 212L, null, -1133L, -131L};
    static final Character[] CHARACTERS = {'a', 'ä', '@', null, '#', '\''};
    static final Short[] SHORTS = {255, 0x7FFF, null, 888};
    static final Byte[] BYTES = {12, -0x7F, null, 0x7F};
    static final Double[] DOUBLES = {-0.0000001d, null, -13131.3d, 4e22, 222d};

    static final Map<String, Integer>[][] MAPS_ARRAY;
    static final SortedMap<String, Integer>[] SORTEDMAPS_ARRAY;

    static {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", null);
        SortedMap<String, Integer> sortedMap = new TreeMap<>(map);

        MAPS_ARRAY = new Map[2][3];
        MAPS_ARRAY[0][1] = map;
        MAPS_ARRAY[1][2] = sortedMap;

        SORTEDMAPS_ARRAY = new TreeMap[1];
        SORTEDMAPS_ARRAY[0] = new TreeMap<>();
        SORTEDMAPS_ARRAY[0].put("zzz", null);
        SORTEDMAPS_ARRAY[0].put("aaa", 1);


    }


    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder().register(BaseTest.class).build()
                    ),
                    MongoClient.getDefaultCodecRegistry()
            );
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

    static class BasePojo {
        String aString;
        float aPrimitiveFloat;
        int aPrimitiveInt;
        long aPrimitiveLong;
        char aPrimitiveChar;
        short aPrimitiveShort;
        byte aPrimitiveByte;
        double aPrimitiveDouble;

        Float aFloat;
        Integer anInteger;
        Long aLong;
        Character aCharacter;
        Short aShort;
        Byte aByte;
        Double aDouble;

        String[] strings;
        float[] primitiveFloats;
        int[] primitiveInts;
        long[] primitiveLongs;
        char[] primitiveChars;
        short[] primitiveShorts;
        byte[] primitiveBytes;
        byte[][] primitiveBytes_2;
        double[] primitiveDoubles;

        Float[] floats;
        Integer[] integers;
        Long[] longs;
        Character[] characters;
        Short[] shorts;
        Byte[] bytes;
        Double[] doubles;
        Map<String, Integer>[][] mapsArray;
        SortedMap<String, Integer>[] sortedMapArray;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BasePojo)) return false;

            BasePojo basePojo = (BasePojo) o;

            if (Float.compare(basePojo.aPrimitiveFloat, aPrimitiveFloat) != 0) return false;
            if (aPrimitiveInt != basePojo.aPrimitiveInt) return false;
            if (aPrimitiveLong != basePojo.aPrimitiveLong) return false;
            if (aPrimitiveChar != basePojo.aPrimitiveChar) return false;
            if (aPrimitiveShort != basePojo.aPrimitiveShort) return false;
            if (aPrimitiveByte != basePojo.aPrimitiveByte) return false;
            if (Double.compare(basePojo.aPrimitiveDouble, aPrimitiveDouble) != 0) return false;
            if (aString != null ? !aString.equals(basePojo.aString) : basePojo.aString != null) return false;
            if (aFloat != null ? !aFloat.equals(basePojo.aFloat) : basePojo.aFloat != null) return false;
            if (anInteger != null ? !anInteger.equals(basePojo.anInteger) : basePojo.anInteger != null) return false;
            if (aLong != null ? !aLong.equals(basePojo.aLong) : basePojo.aLong != null) return false;
            if (aCharacter != null ? !aCharacter.equals(basePojo.aCharacter) : basePojo.aCharacter != null)
                return false;
            if (aShort != null ? !aShort.equals(basePojo.aShort) : basePojo.aShort != null) return false;
            if (aByte != null ? !aByte.equals(basePojo.aByte) : basePojo.aByte != null) return false;
            if (aDouble != null ? !aDouble.equals(basePojo.aDouble) : basePojo.aDouble != null) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(strings, basePojo.strings)) return false;
            if (!Arrays.equals(primitiveFloats, basePojo.primitiveFloats)) return false;
            if (!Arrays.equals(primitiveInts, basePojo.primitiveInts)) return false;
            if (!Arrays.equals(primitiveLongs, basePojo.primitiveLongs)) return false;
            if (!Arrays.equals(primitiveChars, basePojo.primitiveChars)) return false;
            if (!Arrays.equals(primitiveShorts, basePojo.primitiveShorts)) return false;
            if (!Arrays.equals(primitiveBytes, basePojo.primitiveBytes)) return false;
            if (!Arrays.deepEquals(primitiveBytes_2, basePojo.primitiveBytes_2)) return false;
            if (!Arrays.equals(primitiveDoubles, basePojo.primitiveDoubles)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(floats, basePojo.floats)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(integers, basePojo.integers)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(longs, basePojo.longs)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(characters, basePojo.characters)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(shorts, basePojo.shorts)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bytes, basePojo.bytes)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(doubles, basePojo.doubles)) return false;
            if (!Arrays.deepEquals(mapsArray, basePojo.mapsArray)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(sortedMapArray, basePojo.sortedMapArray);
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = aString != null ? aString.hashCode() : 0;
            result = 31 * result + (aPrimitiveFloat != +0.0f ? Float.floatToIntBits(aPrimitiveFloat) : 0);
            result = 31 * result + aPrimitiveInt;
            result = 31 * result + (int) (aPrimitiveLong ^ (aPrimitiveLong >>> 32));
            result = 31 * result + (int) aPrimitiveChar;
            result = 31 * result + (int) aPrimitiveShort;
            result = 31 * result + (int) aPrimitiveByte;
            temp = Double.doubleToLongBits(aPrimitiveDouble);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + (aFloat != null ? aFloat.hashCode() : 0);
            result = 31 * result + (anInteger != null ? anInteger.hashCode() : 0);
            result = 31 * result + (aLong != null ? aLong.hashCode() : 0);
            result = 31 * result + (aCharacter != null ? aCharacter.hashCode() : 0);
            result = 31 * result + (aShort != null ? aShort.hashCode() : 0);
            result = 31 * result + (aByte != null ? aByte.hashCode() : 0);
            result = 31 * result + (aDouble != null ? aDouble.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(strings);
            result = 31 * result + Arrays.hashCode(primitiveFloats);
            result = 31 * result + Arrays.hashCode(primitiveInts);
            result = 31 * result + Arrays.hashCode(primitiveLongs);
            result = 31 * result + Arrays.hashCode(primitiveChars);
            result = 31 * result + Arrays.hashCode(primitiveShorts);
            result = 31 * result + Arrays.hashCode(primitiveBytes);
            result = 31 * result + Arrays.deepHashCode(primitiveBytes_2);
            result = 31 * result + Arrays.hashCode(primitiveDoubles);
            result = 31 * result + Arrays.hashCode(floats);
            result = 31 * result + Arrays.hashCode(integers);
            result = 31 * result + Arrays.hashCode(longs);
            result = 31 * result + Arrays.hashCode(characters);
            result = 31 * result + Arrays.hashCode(shorts);
            result = 31 * result + Arrays.hashCode(bytes);
            result = 31 * result + Arrays.hashCode(doubles);
            result = 31 * result + Arrays.deepHashCode(mapsArray);
            result = 31 * result + Arrays.hashCode(sortedMapArray);
            return result;
        }
    }


    static class Base<T> {
        T anyType;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Base<?> base = (Base<?>) o;

            return anyType != null ? anyType.equals(base.anyType) : base.anyType == null;
        }

        @Override
        public int hashCode() {
            return anyType != null ? anyType.hashCode() : 0;
        }
    }

    static class IntegerType extends Base<Integer> {

    }

    @Test
    public void basicTest() {
        BasePojo basePojo = new BasePojo();
        basePojo.aString = STRING;
        basePojo.aPrimitiveByte = PRIMITIVE_BYTE;
        basePojo.aPrimitiveChar = PRIMITIVE_CHAR;
        basePojo.aPrimitiveDouble = PRIMITIVE_DOUBLE;
        basePojo.aPrimitiveShort = PRIMITIVE_SHORT;
        basePojo.aPrimitiveLong = PRIMITIVE_LONG;
        basePojo.aPrimitiveFloat = PRIMITIVE_FLOAT;
        basePojo.aPrimitiveInt = PRIMITIVE_INT;

        basePojo.aByte = BYTE;
        basePojo.aCharacter = CHARACTER;
        basePojo.aDouble = DOUBLE;
        basePojo.aShort = SHORT;
        basePojo.aLong = LONG;
        basePojo.aFloat = FLOAT;
        basePojo.anInteger = INTEGER;

        basePojo.strings = STRINGS;
        basePojo.primitiveFloats = PRIMITIVE_FLOATS;
        basePojo.primitiveInts = PRIMITIVE_INTS;
        basePojo.primitiveLongs = PRIMITIVE_LONGS;
        basePojo.primitiveChars = PRIMITIVE_CHARS;
        basePojo.primitiveShorts = PRIMITIVE_SHORTS;
        basePojo.primitiveBytes = PRIMITIVE_BYTES;
        basePojo.primitiveDoubles = PRIMITIVE_DOUBLES;
        basePojo.primitiveBytes_2 = PRIMITIVE_BYTES_2;

        basePojo.floats = FLOATS;
        basePojo.integers = INTEGERS;
        basePojo.longs = LONGS;
        basePojo.characters = CHARACTERS;
        basePojo.shorts = SHORTS;
        basePojo.bytes = BYTES;
        basePojo.doubles = DOUBLES;
        basePojo.mapsArray = MAPS_ARRAY;
        basePojo.sortedMapArray = SORTEDMAPS_ARRAY;

        Codec<BasePojo> primitivePojoCodec = codecRegistry.get(BasePojo.class);

        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        primitivePojoCodec.encode(writer, basePojo, EncoderContext.builder().build());
        LOGGER.info("The encoded json looks like: {}", stringWriter);
        BasePojo readBasePojo = primitivePojoCodec.decode(new JsonReader(stringWriter.toString()), DecoderContext.builder().build());
        Assert.assertEquals(basePojo, readBasePojo);
        Assert.assertTrue(readBasePojo.sortedMapArray[0] instanceof SortedMap);
    }

    @Test
    public void genericTest() {
        IntegerType integerType = new IntegerType();
        integerType.anyType = INTEGER;

        Codec<IntegerType> integerTypeCodec = codecRegistry.get(IntegerType.class);
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, JsonWriterSettings.builder().indent(true).outputMode(JsonMode.EXTENDED).build());
        integerTypeCodec.encode(writer, integerType, EncoderContext.builder().build());
        LOGGER.info("The encoded json looks like: {}", stringWriter);
        IntegerType readIntegerType = integerTypeCodec.decode(new JsonReader(stringWriter.toString()), DecoderContext.builder().build());

        Assert.assertEquals(integerType, readIntegerType);

    }

}