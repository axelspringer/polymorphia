package de.bild.codec;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class ArrayCodec implements TypeCodec {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArrayCodec.class);
    final Codec arrayElementCodec;
    final Class<?> arrayClazz;

    public ArrayCodec(Type type, TypeCodecRegistry typeCodecRegistry) {
        if (TypeUtils.isArrayType(type)) {
            arrayClazz = ReflectionHelper.extractRawClass(type);
            if (type instanceof GenericArrayType) {
                GenericArrayType genericArrayType = (GenericArrayType) type;
                arrayElementCodec = typeCodecRegistry.getCodec(genericArrayType.getGenericComponentType());
            } else {
                arrayElementCodec = typeCodecRegistry.getCodec(arrayClazz.getComponentType());
            }
        } else {
            throw new IllegalArgumentException("Unknown array type?!" + type);
        }
    }

    @Override
    public void encode(BsonWriter writer, Object array, EncoderContext encoderContext) {
        writer.writeStartArray();
        for (int i = 0; i < Array.getLength(array); i++) {
            Object value = Array.get(array, i);
            if (value != null) {
                arrayElementCodec.encode(writer, value, encoderContext);
            } else {
                writer.writeNull();
            }

        }
        writer.writeEndArray();

    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        Object array = null;
        if (BsonType.ARRAY.equals(reader.getCurrentBsonType())) {

            List list = new ArrayList();
            reader.readStartArray();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
                    reader.skipValue();
                    list.add(null);
                } else {
                    list.add(arrayElementCodec.decode(reader, decoderContext));
                }
            }
            reader.readEndArray();
            array = Array.newInstance(arrayClazz.getComponentType(), list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
        } else {
            LOGGER.warn("Expected {} from reader but got {}. Skipping value.", BsonType.ARRAY, reader.getCurrentBsonType());
            reader.skipValue();
        }
        return array;
    }


    @Override
    public Class getEncoderClass() {
        return arrayClazz;
    }

    /**
     * Codecs for primitive arrays
     */
    public enum PrimitiveArrayCodec implements Codec {
        BYTE(byte.class) {
            @Override
            public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (byte i : (byte[]) value) {
                    writer.writeInt32(i);
                }
            }

            @Override
            public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
                List<Byte> arrayList = new ArrayList<>();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    arrayList.add((byte) reader.readInt32());
                }
                byte[] bytes = new byte[arrayList.size()];
                int i = 0;
                for (byte aPrimitiveByte : arrayList) {
                    bytes[i++] = aPrimitiveByte;
                }
                return bytes;
            }

        },
        BOOLEAN(boolean.class) {
            @Override
            public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (boolean i : (boolean[]) value) {
                    writer.writeBoolean(i);
                }
            }

            @Override
            public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
                List<Boolean> arrayList = new ArrayList<>();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    arrayList.add(reader.readBoolean());
                }
                boolean[] booleans = new boolean[arrayList.size()];
                int i = 0;
                for (boolean aPrimitiveBoolean : arrayList) {
                    booleans[i++] = aPrimitiveBoolean;
                }
                return booleans;
            }
        },
        CHARACTER(char.class) {
            @Override
            public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (char i : (char[]) value) {
                    writer.writeInt32(i);
                }
            }

            @Override
            public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
                List<Character> arrayList = new ArrayList<>();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    arrayList.add((char) reader.readInt32());
                }
                char[] chars = new char[arrayList.size()];
                int i = 0;
                for (char aPrimitive : arrayList) {
                    chars[i++] = aPrimitive;
                }
                return chars;
            }
        },
        FLOAT(float.class) {
            @Override
            public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (float i : (float[]) value) {
                    writer.writeDouble(i);
                }
            }

            @Override
            public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
                List<Float> arrayList = new ArrayList<>();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    arrayList.add((float) reader.readDouble());
                }
                float[] floats = new float[arrayList.size()];
                int i = 0;
                for (float aPrimitive : arrayList) {
                    floats[i++] = aPrimitive;
                }
                return floats;
            }
        },
        INTEGER(int.class) {
            @Override
            public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (int i : (int[]) value) {
                    writer.writeInt32(i);
                }
            }

            @Override
            public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
                /*
                 * Efficient way of decoding an int[] of unknown size
                 * IntStream uses an primitive int buffer internally
                 */
                IntStream.Builder builder = IntStream.builder();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    builder.add(reader.readInt32());
                }
                return builder.build().toArray();
            }
        },
        LONG(long.class) {
            @Override
            public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (long i : (long[]) value) {
                    writer.writeInt64(i);
                }
            }

            @Override
            public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
                LongStream.Builder builder = LongStream.builder();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    builder.add(reader.readInt64());
                }
                return builder.build().toArray();
            }
        },
        SHORT(short.class) {
            @Override
            public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (short i : (short[]) value) {
                    writer.writeInt32(i);
                }
            }

            @Override
            public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
                List<Short> arrayList = new ArrayList<>();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    arrayList.add((short) reader.readInt32());
                }
                short[] shorts = new short[arrayList.size()];
                int i = 0;
                for (short aPrimitive : arrayList) {
                    shorts[i++] = aPrimitive;
                }
                return shorts;
            }
        },
        DOUBLE(double.class) {
            @Override
            public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (double i : (double[]) value) {
                    writer.writeDouble(i);
                }
            }

            @Override
            public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
                List<Double> arrayList = new ArrayList<>();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    arrayList.add(reader.readDouble());
                }
                double[] doubles = new double[arrayList.size()];
                int i = 0;
                for (double aPrimitive : arrayList) {
                    doubles[i++] = aPrimitive;
                }
                return doubles;
            }
        };
        final Class<?> primitiveClass;

        static final Map<Class<?>, PrimitiveArrayCodec> PRIMITIVE_CLASS_TO_TYPE = new HashMap<>();

        PrimitiveArrayCodec(Class<?> primitiveClass) {
            this.primitiveClass = primitiveClass;
        }


        @Override
        public Class getEncoderClass() {
            return primitiveClass;
        }


        static {
            for (PrimitiveArrayCodec primitiveType : PrimitiveArrayCodec.values()) {
                PRIMITIVE_CLASS_TO_TYPE.put(primitiveType.primitiveClass, primitiveType);
            }
        }

        public static PrimitiveArrayCodec get(Class<?> arrayClass) {
            if (arrayClass != null) {
                Class<?> componentType = arrayClass.getComponentType();
                return PRIMITIVE_CLASS_TO_TYPE.get(componentType);
            }
            return null;
        }


        protected abstract Object decodeInternal(BsonReader reader, DecoderContext decoderContext);

        protected abstract void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext);

        @Override
        public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
            writer.writeStartArray();
            encodeInternal(writer, value, encoderContext);
            writer.writeEndArray();
        }

        @Override
        public Object decode(BsonReader reader, DecoderContext decoderContext) {
            Object primitiveArray;
            reader.readStartArray();
            primitiveArray = decodeInternal(reader, decoderContext);
            reader.readEndArray();
            return primitiveArray;
        }
    }
}