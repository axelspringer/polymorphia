package de.bild.codec;

import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Codecs for primitive arrays
 */
public enum PrimitiveArrayCodec implements Codec {
    BYTE(byte[].class) {
        @Override
        public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
            writer.writeBinaryData(new BsonBinary((byte[])value));
        }

        @Override
        public Object decode(BsonReader reader, DecoderContext decoderContext) {
            return reader.readBinaryData().getData();
        }

        @Override
        public void encodeInternal(BsonWriter writer, Object value, EncoderContext encoderContext) {
            throw new IllegalStateException("This method 'encodeInternal' on BYTE must never be called");
        }

        @Override
        public Object decodeInternal(BsonReader reader, DecoderContext decoderContext) {
            throw new IllegalStateException("This method 'decodeInternal' on BYTE must never be called");
        }

    },
    BOOLEAN(boolean[].class) {
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
    CHARACTER(char[].class) {
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
    FLOAT(float[].class) {
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
    INTEGER(int[].class) {
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
    LONG(long[].class) {
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
    SHORT(short[].class) {
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
    DOUBLE(double[].class) {
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
            return PRIMITIVE_CLASS_TO_TYPE.get(arrayClass);
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