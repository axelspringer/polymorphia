package de.bild.codec;

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
import java.lang.reflect.ParameterizedType;
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
    final boolean isPrimitive;
    final Class<?> arrayType;

    public ArrayCodec(Type type, TypeCodecRegistry typeCodecRegistry) {
        if (type instanceof Class) {
            Class<?> currentComponentType = (Class) type;

            arrayType = currentComponentType;
            while (currentComponentType.isArray()) {
                currentComponentType = currentComponentType.getComponentType();
            }

            // the mongo driver has an optimized codec for byte arrays hence we do not handle it
            if (currentComponentType == byte.class) {
                isPrimitive = true;
                arrayElementCodec = typeCodecRegistry.getCodec(byte[].class);
            } else if (currentComponentType.isPrimitive()) {
                isPrimitive = true;
                arrayElementCodec = PrimitiveType.get(currentComponentType);
            } else {
                isPrimitive = false;
                arrayElementCodec = typeCodecRegistry.getCodec(currentComponentType);
            }

        } else if (type instanceof GenericArrayType) {
            isPrimitive = false;

            Type currentLevelType = type;
            int level = 0;
            while (currentLevelType instanceof GenericArrayType) {
                currentLevelType = ((GenericArrayType) currentLevelType).getGenericComponentType();
                level++;
            }

            Class<?> currentArrayClass = null;
            if (currentLevelType instanceof ParameterizedType) {
                currentArrayClass = (Class<?>) ((ParameterizedType) currentLevelType).getRawType();
            }
            for (int i = 0; i < level; i++) {
                currentArrayClass = Array.newInstance(currentArrayClass, 0).getClass();
            }

            arrayType = currentArrayClass;

            Type genericComponentType = currentLevelType;
            if (!(genericComponentType instanceof ParameterizedType)) {
                throw new IllegalArgumentException("Unable to determine array class! " + type);
            }
            arrayElementCodec = typeCodecRegistry.getCodec(genericComponentType);

        } else {
            throw new IllegalArgumentException("Unknown array type?!" + type);
        }
    }

    @Override
    public void encode(BsonWriter writer, Object array, EncoderContext encoderContext) {
        encodeDimension(writer, array, encoderContext, arrayType);
    }

    private void encodeDimension(BsonWriter writer, Object array, EncoderContext encoderContext, Class<?> componentType) {
        boolean encodeLastDimension;
        Class<?> childComponentType = componentType.getComponentType();
        encodeLastDimension = !childComponentType.isArray();

        if (childComponentType == byte.class) {
            arrayElementCodec.encode(writer, array, encoderContext);
        } else {
            writer.writeStartArray();

            if (encodeLastDimension) {
                if (isPrimitive) {
                    arrayElementCodec.encode(writer, array, encoderContext);
                } else {

                    for (int i = 0; i < Array.getLength(array); i++) {
                        arrayElementCodec.encode(writer, Array.get(array, i), encoderContext);
                    }
                }
            } else {
                for (int i = 0; i < Array.getLength(array); i++) {
                    encodeDimension(writer, Array.get(array, i), encoderContext, childComponentType);
                }
            }
            writer.writeEndArray();
        }
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        return decodeDimension(reader, decoderContext, arrayType);
    }

    private Object decodeDimension(BsonReader reader, DecoderContext decoderContext, Class<?> componentType) {
        Object array = null;
        boolean decodeLastDimension;
        Class<?> childComponentType = componentType.getComponentType();
        decodeLastDimension = !childComponentType.isArray();

        if (childComponentType == byte.class) {
            array = arrayElementCodec.decode(reader, decoderContext);
        } else if (BsonType.ARRAY.equals(reader.getCurrentBsonType())) {
            reader.readStartArray();
            if (decodeLastDimension) {
                if (isPrimitive) {
                    array = arrayElementCodec.decode(reader, decoderContext);
                } else {
                    List list = new ArrayList();
                    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                        list.add(arrayElementCodec.decode(reader, decoderContext));
                    }
                    array = Array.newInstance(childComponentType, list.size());
                    for (int i = 0; i < list.size(); i++) {
                        Array.set(array, i, list.get(i));
                    }
                }
            } else {
                List arrayList = new ArrayList();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    Object decoded = decodeDimension(reader, decoderContext, childComponentType);
                    arrayList.add(decoded);
                }

                array = Array.newInstance(childComponentType, arrayList.size());
                for (int i = 0; i < arrayList.size(); i++) {
                    Array.set(array, i, arrayList.get(i));
                }
            }
            reader.readEndArray();
        } else {
            LOGGER.warn("Expected {} from reader but got {}. Skipping value.", BsonType.ARRAY, reader.getCurrentBsonType());
            reader.skipValue();
        }
        return array;
    }

    @Override
    public Class getEncoderClass() {
        return arrayType;
    }

    /**
     * Codecs for primitive arrays
     */
    private enum PrimitiveType implements TypeCodec {
        BOOLEAN(boolean.class) {
            @Override
            public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (boolean i : (boolean[]) value) {
                    writer.writeBoolean(i);
                }
            }

            @Override
            public Object decode(BsonReader reader, DecoderContext decoderContext) {
                List<Boolean> arrayList = new ArrayList();
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
            public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (char i : (char[]) value) {
                    writer.writeInt32(i);
                }
            }

            @Override
            public Object decode(BsonReader reader, DecoderContext decoderContext) {
                List<Character> arrayList = new ArrayList();
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
            public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (float i : (float[]) value) {
                    writer.writeDouble(i);
                }
            }

            @Override
            public Object decode(BsonReader reader, DecoderContext decoderContext) {
                List<Float> arrayList = new ArrayList();
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
            public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (int i : (int[]) value) {
                    writer.writeInt32(i);
                }
            }

            @Override
            public Object decode(BsonReader reader, DecoderContext decoderContext) {
                /**
                 * Efficient way of decoding an int[] of unknown size
                 * InstStream uses an primitive int buffer internally
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
            public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (long i : (long[]) value) {
                    writer.writeInt64(i);
                }
            }

            @Override
            public Object decode(BsonReader reader, DecoderContext decoderContext) {
                LongStream.Builder builder = LongStream.builder();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    builder.add(reader.readInt64());
                }
                return builder.build().toArray();
            }
        },
        SHORT(short.class) {
            @Override
            public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (short i : (short[]) value) {
                    writer.writeInt32(i);
                }
            }

            @Override
            public Object decode(BsonReader reader, DecoderContext decoderContext) {
                List<Short> arrayList = new ArrayList();
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
            public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
                for (double i : (double[]) value) {
                    writer.writeDouble(i);
                }
            }

            @Override
            public Object decode(BsonReader reader, DecoderContext decoderContext) {
                List<Double> arrayList = new ArrayList();
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

        static final Map<Class<?>, PrimitiveType> PRIMITIVE_CLASS_TO_TYPE = new HashMap<>();

        PrimitiveType(Class<?> primitiveClass) {
            this.primitiveClass = primitiveClass;
        }

        @Override
        public Class getEncoderClass() {
            return primitiveClass;
        }


        static {
            for (PrimitiveType primitiveType : values()) {
                PRIMITIVE_CLASS_TO_TYPE.put(primitiveType.primitiveClass, primitiveType);
            }
        }

        public static PrimitiveType get(Class type) {
            return PRIMITIVE_CLASS_TO_TYPE.get(type);
        }

    }
}