package de.bild.codec;

import de.bild.codec.annotations.*;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @param <T> The type of the persistence class container (pojo) for this mapped field
 * @param <F> The type of the field value
 */
public class MappedField<T, F> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappedField.class);
    public static final String ID_KEY = "_id";

    private static final List<Class<? extends Annotation>> ANNOTATIONS_TO_BE_HANDLED = new ArrayList<>();

    static {
        ANNOTATIONS_TO_BE_HANDLED.add(Id.class);
        ANNOTATIONS_TO_BE_HANDLED.add(LockingVersion.class);
        ANNOTATIONS_TO_BE_HANDLED.add(EncodeNullHandlingStrategy.class);
        ANNOTATIONS_TO_BE_HANDLED.add(DecodeUndefinedHandlingStrategy.class);
        ANNOTATIONS_TO_BE_HANDLED.add(EncodeNulls.class);
    }


    final Field field;
    final Class<T> persistedClass;

    private Codec<F> codec;
    private PrimitiveType primitiveType;

    final FieldTypePair fieldTypePair;
    final EncodeNullHandlingStrategy.Strategy encodeNullHandlingStrategy;
    final DecodeUndefinedHandlingStrategy.Strategy decodeUndefinedHandlingStrategy;
    final boolean encodeNulls;

    final CodecConfiguration codecConfiguration;


    // Annotations that have been found relevant to mapping
    private final Map<Class<? extends Annotation>, Annotation> foundAnnotations;

    public MappedField(FieldTypePair fieldTypePair,
                       Class<T> persistedClass,
                       TypeCodecRegistry typeCodecRegistry,
                       CodecConfiguration codecConfiguration) {
        this.field = fieldTypePair.getField();
        this.field.setAccessible(true);
        this.fieldTypePair = fieldTypePair;
        this.persistedClass = persistedClass;
        this.foundAnnotations = buildAnnotationMap(field);
        this.codecConfiguration = codecConfiguration;

        if (field.getType().isPrimitive()) {
            this.primitiveType = PrimitiveType.get(field.getType());
        } else {
            CodecToBeUsed codecToBeUsed = fieldTypePair.getField().getDeclaredAnnotation(CodecToBeUsed.class);
            if (codecToBeUsed != null) {
                /**
                 * Pojo fields can be annotated with the {@link de.bild.backend.domain.model.annotations.CodecToBeUsed} annotation
                 * The provided value points to a codec to be used
                 * This code could be moved to {@link de.bild.backend.domain.model.codec.PojoContext#getCodec(Type, TypeCodecRegistry)}
                 * to provide better reuse of the resolved codec. For now it seems clearer to do it here.
                 */
                Class<? extends Codec> clazz = codecToBeUsed.value();
                try {
                    Constructor declaredConstructor = clazz.getDeclaredConstructor(TypeCodecRegistry.class);
                    declaredConstructor.setAccessible(true);
                    try {
                        this.codec = (Codec) declaredConstructor.newInstance(typeCodecRegistry);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.warn("Unable to instantiate codec for {} ", clazz, e);
                    }
                } catch (NoSuchMethodException e) {
                    LOGGER.warn("Unable to find constructor {}(TypeCodecRegistry.class)", clazz, e);
                }
            } else {
                this.codec = typeCodecRegistry.getCodec(fieldTypePair.getRealType());
            }
        }

        /**
         * get configuration for field
         */

        EncodeNullHandlingStrategy classEncodeNullHandlingStrategy = persistedClass.getDeclaredAnnotation(EncodeNullHandlingStrategy.class);
        EncodeNullHandlingStrategy fieldEncodeNullHandlingStrategy = getAnnotation(EncodeNullHandlingStrategy.class);
        this.encodeNullHandlingStrategy = (fieldEncodeNullHandlingStrategy != null) ? fieldEncodeNullHandlingStrategy.value() : (classEncodeNullHandlingStrategy != null) ? classEncodeNullHandlingStrategy.value() : codecConfiguration.getEncodeNullHandlingStrategy();

        DecodeUndefinedHandlingStrategy classDecodeUndefinedHandlingStrategy = persistedClass.getDeclaredAnnotation(DecodeUndefinedHandlingStrategy.class);
        DecodeUndefinedHandlingStrategy fieldDecodeUndefinedHandlingStrategy = getAnnotation(DecodeUndefinedHandlingStrategy.class);
        this.decodeUndefinedHandlingStrategy = (fieldDecodeUndefinedHandlingStrategy != null) ? fieldDecodeUndefinedHandlingStrategy.value() : (classDecodeUndefinedHandlingStrategy != null) ? classDecodeUndefinedHandlingStrategy.value() : codecConfiguration.getDecodeUndefinedHandlingStrategy();

        EncodeNulls classEncodeNulls = persistedClass.getDeclaredAnnotation(EncodeNulls.class);
        EncodeNulls fieldEncodeNulls = getAnnotation(EncodeNulls.class);
        this.encodeNulls = (fieldEncodeNulls != null) ? fieldEncodeNulls.value() : (classEncodeNulls != null) ? classEncodeNulls.value() : codecConfiguration.isEncodeNulls();
    }


    private static Map<Class<? extends Annotation>, Annotation> buildAnnotationMap(Field field) {
        final Map<Class<? extends Annotation>, Annotation> foundAnnotations = new HashMap<>();
        for (final Class<? extends Annotation> annotationClass : ANNOTATIONS_TO_BE_HANDLED) {
            if (field.isAnnotationPresent(annotationClass)) {
                foundAnnotations.put(annotationClass, field.getAnnotation(annotationClass));
            }
        }
        return Collections.unmodifiableMap(foundAnnotations);
    }

    /**
     * @param clazz the annotation to search for
     * @param <T>   the type of the annotation
     * @return the annotation instance if it exists on this field
     */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
        return (T) foundAnnotations.get(clazz);
    }

    /**
     * @return the annotations found while mapping
     */
    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return foundAnnotations;
    }

    /**
     * Indicates whether the annotation is present in the mapping (does not check the java field annotations, just the ones discovered)
     *
     * @param ann the annotation to search for
     * @return true if the annotation was found
     */
    public boolean hasAnnotation(final Class ann) {
        return foundAnnotations.containsKey(ann);
    }

    public String getMappedFieldName() {
        if (isIdField()) {
            return ID_KEY;
        }
        return field.getName();
    }

    /**
     * @return the underlying java field
     */
    public Field getField() {
        return field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MappedField that = (MappedField) o;

        return field.equals(that.field);

    }

    @Override
    public int hashCode() {
        return field.hashCode();
    }

    public boolean setFieldValue(T instance, F value) {
        try {
            field.set(instance, value);
            return true;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            Type valueType = value != null ? value.getClass() : null;
            LOGGER.warn("Could not set field {} of instance {} to value {} of type", field, instance, value, e);
            throw new TypeMismatchException("Could not set field " + field + " of instance " + instance + " to value " + value + " of type " + valueType, e);
        }
    }

    public F getFieldValue(T instance) {
        try {
            return (F) field.get(instance);
        } catch (IllegalAccessException e) {
            LOGGER.warn("Could not get field value.", field, instance, e);
        }
        return null;
    }

    public void encode(BsonWriter writer, T instance, EncoderContext encoderContext) {
        LOGGER.debug("Encode field : " + getMappedFieldName());
        if (field.getType().isPrimitive()) {
            if (isLockingVersionField()) {
                writeLockingVersion(writer, instance);
            } else {
                primitiveType.encode(writer, instance, encoderContext, this);
            }
        } else if (codec != null) {
            F fieldValue = getFieldValue(instance);
            if (fieldValue == null) {
                switch (encodeNullHandlingStrategy) {
                    case CODEC: {
                        if (codec instanceof TypeCodec) {
                            TypeCodec<F> typeCodec = (TypeCodec) codec;
                            fieldValue = typeCodec.defaultInstance();
                        }
                        break;
                    }
                    case KEEP_NULL:
                        break;

                }
            }

            if (encodeNulls || fieldValue != null) {
                writer.writeName(getMappedFieldName());
                if (fieldValue == null) {
                    writer.writeNull();
                } else {
                    codec.encode(writer, fieldValue, encoderContext);
                }
            }
        }
    }

    private void writeLockingVersion(BsonWriter writer, T instance) {
        try {
            writer.writeName(getMappedFieldName());
            int lockingVersion = field.getInt(instance) + 1;
            writer.writeInt32(lockingVersion);
        } catch (IllegalAccessException e) {
            LOGGER.warn("IllegalAccessException while writeLockingVersion field " + field.getName(), e);
        }
    }

    public void decode(BsonReader reader, T instance, DecoderContext decoderContext) {
        LOGGER.debug("Decode field : " + getMappedFieldName() + " Signature: " + fieldTypePair.getRealType());
        if (field.getType().isPrimitive()) {
            if (reader.getCurrentBsonType() == BsonType.NULL || reader.getCurrentBsonType() == BsonType.UNDEFINED) {
                reader.skipValue();
            } else {
                primitiveType.decode(reader, instance, decoderContext, this);
            }
        } else if (codec != null) {
            if (reader.getCurrentBsonType() == BsonType.NULL ) {
                reader.readNull();
                setFieldValue(instance, null);
            }
            else if (reader.getCurrentBsonType() == BsonType.UNDEFINED) {
                reader.skipValue();
            }
            else {
                F decoded = codec.decode(reader, decoderContext);
                setFieldValue(instance, decoded);
            }
        }
    }

    public boolean isIdField() {
        return hasAnnotation(Id.class);
    }

    public boolean isLockingVersionField() {
        return hasAnnotation(LockingVersion.class) && Integer.TYPE.equals(field.getType());
    }

    public Codec getCodec() {
        return codec;
    }

    /**
     * @param instance to initialize
     * @return true, if the codec initialized (potentially changed) the field
     */
    public void initializeUndefinedValue(T instance) {
        if (field.getType().isPrimitive()) {
            return;
        }
        switch (decodeUndefinedHandlingStrategy) {
            case CODEC: {
                // this potentially overwrites the default value set within the pojo
                if (codec != null && codec instanceof TypeCodec) {
                    TypeCodec<F> typeCodec = (TypeCodec) codec;
                    F defaultValue = typeCodec.defaultInstance();
                    setFieldValue(instance, defaultValue);
                    return;
                } else {
                    LOGGER.info("The provided codec {} for field {} is not capable of retrieving default values.", codec, field);
                }
                break;
            }
            case SET_TO_NULL: {
                // this potentially overwrites the default value set within the pojo
                setFieldValue(instance, null);
                return;
            }
            case KEEP_POJO_DEFAULT:
                break;
        }
    }


    private interface DefaultPrimitiveType {
        <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException;

        <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException;

        default <T> void encode(BsonWriter writer, T instance, EncoderContext encoderContext, MappedField mappedField) {
            try {
                writer.writeName(mappedField.getMappedFieldName());
                encodeInternal(writer, instance, mappedField.getField());
            } catch (IllegalAccessException e) {
                LOGGER.warn("Cannot access mappedField. ", mappedField, e);
            }
        }

        default <T> void decode(BsonReader reader, T instance, DecoderContext decoderContext, MappedField mappedField) {
            try {
                if (checkBsonTypeAndSkipOnMisMatch(reader)) {
                    decodeInternal(reader, instance, mappedField.getField());
                }
            } catch (IllegalAccessException e) {
                LOGGER.warn("Could not decode mappedField.", mappedField, e);
            }
        }

        BsonType getBsonType();

        default boolean checkBsonTypeAndSkipOnMisMatch(BsonReader reader) {
            if (getBsonType().equals(reader.getCurrentBsonType())) {
                return true;
            } else {
                LOGGER.warn("Expected {} from reader but got {}. Skipping value.", getBsonType(), reader.getCurrentBsonType());
                reader.skipValue();
            }
            return false;
        }
    }


    private enum PrimitiveType implements DefaultPrimitiveType {
        BYTE(byte.class, BsonType.INT32) {
            @Override
            public <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException {
                field.setByte(instance, (byte) reader.readInt32());
            }

            @Override
            public <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException {
                writer.writeInt32(field.getByte(instance));
            }
        },
        BOOLEAN(boolean.class, BsonType.BOOLEAN) {
            @Override
            public <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException {
                field.setBoolean(instance, reader.readBoolean());
            }

            @Override
            public <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException {
                writer.writeBoolean(field.getBoolean(instance));
            }
        },
        CHARACTER(char.class, BsonType.INT32) {
            @Override
            public <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException {
                field.setChar(instance, (char) reader.readInt32());
            }

            @Override
            public <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException {
                writer.writeInt32(field.getChar(instance));
            }
        },
        FLOAT(float.class, BsonType.DOUBLE) {
            @Override
            public <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException {
                field.setFloat(instance, (float) reader.readDouble());
            }

            @Override
            public <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException {
                writer.writeDouble(field.getFloat(instance));
            }
        },
        INTEGER(int.class, BsonType.INT32) {
            @Override
            public <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException {
                field.setInt(instance, reader.readInt32());
            }

            @Override
            public <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException {
                writer.writeInt32(field.getInt(instance));
            }
        },
        LONG(long.class, BsonType.INT64) {
            @Override
            public <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException {
                field.setLong(instance, reader.readInt64());
            }

            @Override
            public <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException {
                writer.writeInt64(field.getLong(instance));
            }
        },
        SHORT(short.class, BsonType.INT32) {
            @Override
            public <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException {
                field.setShort(instance, (short) reader.readInt32());
            }

            @Override
            public <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException {
                writer.writeInt32(field.getShort(instance));
            }
        },
        DOUBLE(double.class, BsonType.DOUBLE) {
            @Override
            public <T> void decodeInternal(BsonReader reader, T instance, Field field) throws IllegalAccessException {
                field.setDouble(instance, reader.readDouble());
            }

            @Override
            public <T> void encodeInternal(BsonWriter writer, T instance, Field field) throws IllegalAccessException {
                writer.writeDouble(field.getDouble(instance));
            }
        };
        final Class<?> primitiveClass;

        static final Map<Class<?>, PrimitiveType> PRIMITIVE_CLASS_TO_PRIMITIVE_TYPE = new HashMap<>();

        protected BsonType bsonType;

        PrimitiveType(Class<?> primitiveClass, BsonType bsonType) {
            this.primitiveClass = primitiveClass;
            this.bsonType = bsonType;
        }

        @Override
        public BsonType getBsonType() {
            return bsonType;
        }

        static {
            for (PrimitiveType primitiveType : values()) {
                PRIMITIVE_CLASS_TO_PRIMITIVE_TYPE.put(primitiveType.primitiveClass, primitiveType);
            }
        }

        public static PrimitiveType get(Class type) {
            return PRIMITIVE_CLASS_TO_PRIMITIVE_TYPE.get(type);
        }

    }
}
