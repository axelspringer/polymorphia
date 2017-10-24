package de.bild.codec;


import de.bild.codec.annotations.Discriminator;
import de.bild.codec.annotations.Polymorphic;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PojoContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(PojoContext.class);

    private static final FloatCodec FLOAT_CODEC = new FloatCodec();
    private static final ByteCodec BYTE_CODEC = new ByteCodec();
    private static final ShortCodec SHORT_CODEC = new ShortCodec();

    private final Map<Type, Codec<?>> codecMap = new ConcurrentHashMap<>();
    private final TypesModel typesModel;
    private final List<CodecResolver> codecResolvers;

    public PojoContext(final TypesModel typesModel, List<CodecResolver> codecResolvers) {
        this.typesModel = typesModel;
        this.codecResolvers = codecResolvers;
    }


    public synchronized <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        return getCodec(clazz, new AnyTypeCodecRegistry(registry, this));
    }

    /**
     * Calculates and returns a codec for the given type, null otherwise
     *
     * @param type type for which a codec is requested
     * @param typeCodecRegistry codec registry that can handle any type including parameterizd types, generic arrays, etc
     * @param <T> the value type
     * @return the codec responsible for the given type or null
     */
    public synchronized <T> Codec<T> getCodec(Type type, TypeCodecRegistry typeCodecRegistry) {
        Codec codec = codecMap.get(type);
        if (codec != null) {
            return codec;
        }

        // we register within codecMap as LazyCodec for recursion detection
        LazyCodec lazyCodec = new LazyCodec(type, typeCodecRegistry);
        codecMap.put(type, lazyCodec);

        // calculate the codec for given type
        codec = calculateCodec(type, typeCodecRegistry);
        if (codec == null) {
            codecMap.remove(type);
        } else {
            codecMap.put(type, codec);
        }

        return codec;
    }


    /**
     * Iterates over the list of codecResolvers and returns a ReflectionCodec if match is found.
     * @param type the value type
     * @param typeCodecRegistry codec registry that can handle any type including parameterizd types, generic arrays, etc
     * @return ReflectionCodec if responsible resolver si found
     */
    public synchronized ReflectionCodec resolve(Type type, TypeCodecRegistry typeCodecRegistry) {
        ReflectionCodec codec;
        for (CodecResolver codecResolver : codecResolvers) {
            codec = codecResolver.getCodec(type, typeCodecRegistry);
            if (codec != null) {
                return codec;
            }
        }
        return null;
    }

    /**
     * Will try to find an appropriate codec for the given type.
     *
     * @return Codec, if found or null, in case the type can or should not be handled by the pojo codec
     */
    private <T> Codec<T> calculateCodec(Type type, TypeCodecRegistry typeCodecRegistry) {
        // first treat special types

        // byte arrays are handled well by the mongo java driver
        if (type.equals(byte[].class)) {
            return null;
        } else if (TypeUtils.isArrayType(type)) {
            return new ArrayCodec(type, typeCodecRegistry);
        } else if (type instanceof TypeVariable) {
            throw new IllegalArgumentException("This registry (and probably no other one as well) can not handle generic type variables.");
        } else if (type instanceof WildcardType) {
            LOGGER.error("WildcardTypes are not yet supported. {}", type);
            throw new NotImplementedException("WildcardTypes are not yet supported. " + type);
        }
        // the default codecs provided by the mongo driver lack the decode method, hence this redefinition
        else if (Float.class.equals(type)) {
            return (Codec<T>) FLOAT_CODEC;
        } else if (Short.class.equals(type)) {
            return (Codec<T>) SHORT_CODEC;
        } else if (Byte.class.equals(type)) {
            return (Codec<T>) BYTE_CODEC;
        }
        // enums will be handled by a general enum codec registered as CodecProvider within CodecRegistry
        else if (TypeUtils.isAssignable(type, Enum.class)) {
            return null;
        } else if (TypeUtils.isAssignable(type, SpecialFieldsMap.class)) {
            return new SpecialFieldsMapCodec(type, typeCodecRegistry);
        }

        // List ?
        Codec<T> codec = ListTypeCodec.getCodecIfApplicable(type, typeCodecRegistry);
        if (codec != null) {
            return codec;
        }
        // Set ?
        codec = SetTypeCodec.getCodecIfApplicable(type, typeCodecRegistry);
        if (codec != null) {
            return codec;
        }
        // Map ?
        codec = MapTypeCodec.getCodecIfApplicable(type, typeCodecRegistry);
        if (codec != null) {
            return codec;
        }

        /**
         * now try pojo codec
         */
        Set<Type> validTypesForType = getValidTypesForType(type);

        if (validTypesForType == null || validTypesForType.isEmpty()) {
            LOGGER.debug("Could not find concrete implementation for type {}. Maybe another codec is able to handle te class?!", type);
            return null;
        } else if (isPolymorphic(validTypesForType)) {
            LOGGER.debug("Creating polymorphic codec for type {} with valid types {}", type, validTypesForType);
            return new PolymorphicReflectionCodec<>(type, validTypesForType, typeCodecRegistry, this);
        } else {
            LOGGER.debug("Creating simple reflection based codec for type {} (generic type {}) as only one concrete implementation known.", type, validTypesForType);
            Type singleType = validTypesForType.iterator().next();
            codec = resolve(singleType, typeCodecRegistry);
            if (codec != null) {
                return codec;
            } else {
                return new BasicReflectionCodec<>(singleType, typeCodecRegistry);
            }
        }
    }


    /**
     * @param validTypes
     * @return true, if validTypes reflects a polymorphic type structure
     */
    private boolean isPolymorphic(Set<Type> validTypes) {
        if (validTypes != null) {
            if (validTypes.size() > 1) {
                return true;
            } else if (!validTypes.isEmpty()) {
                Class clazz = ReflectionHelper.extractRawClass(validTypes.iterator().next());
                return hasNonAbstractSuperClass(clazz) || clazz.getAnnotation(Polymorphic.class) != null || clazz.getDeclaredAnnotation(Discriminator.class) != null;
            }
        }
        return false;
    }

    /**
     * @param clazz
     * @return true if the type is part of a class hierarchy or
     * alternatively if the annotation @Polymorphic is found in the class hierarchy
     */
    private boolean hasNonAbstractSuperClass(Class clazz) {
        //if superclass of given type is present in all known types, the class is considered to be polymorphic
        Class superclass = clazz.getSuperclass();
        return superclass != null && !Modifier.isAbstract(superclass.getModifiers()) && typesModel.getClassHierarchyNodeForType(superclass) != null;

    }


    private Set<Type> getValidTypesForType(Type type) {
        Set<Type> validTypes = typesModel.getAssignableTypesWithinClassHierarchy(type);
        return validTypes;
    }

    /**
     * First the pojoContext is requested to return a valid codec, if this fails, the mongo codecregistry will be asked
     */
    private static class AnyTypeCodecRegistry implements TypeCodecRegistry {
        final CodecRegistry codecRegistry;
        final PojoContext pojoContext;

        public AnyTypeCodecRegistry(CodecRegistry codecRegistry, PojoContext pojoContext) {
            this.codecRegistry = codecRegistry;
            this.pojoContext = pojoContext;
        }

        @Override
        public <T> Codec<T> getCodec(Type type) {
            Codec codec = pojoContext.getCodec(type, this);
            if (codec == null) {
                codec = codecRegistry.get(ReflectionHelper.extractRawClass(type));
            }
            return codec;
        }

        @Override
        public CodecRegistry getRegistry() {
            return codecRegistry;
        }
    }


    /**
     * Class is used internally to detect cycles.
     */
    private static class LazyCodec<T> implements TypeCodec<T> {
        private final Type type;
        private volatile Codec<T> wrapped;
        private final TypeCodecRegistry typeCodecRegistry;

        public LazyCodec(final Type type, TypeCodecRegistry typeCodecRegistry) {
            this.type = type;
            this.typeCodecRegistry = typeCodecRegistry;
        }

        @Override
        public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
            getWrapped().encode(writer, value, encoderContext);
        }

        @Override
        public Class<T> getEncoderClass() {
            return getWrapped().getEncoderClass();
        }

        @Override
        public T decode(final BsonReader reader, final DecoderContext decoderContext) {
            return getWrapped().decode(reader, decoderContext);
        }

        private Codec<T> getWrapped() {
            if (wrapped == null) {
                wrapped = typeCodecRegistry.getCodec(type);
            }

            return wrapped;
        }
    }

    /**
     * for some reason some mongo driver provided Codecs do not implement decode properly
     */
    private static class FloatCodec extends org.bson.codecs.FloatCodec {
        @Override
        public Float decode(BsonReader reader, DecoderContext decoderContext) {
            return (float) reader.readDouble();
        }
    }

    private static class ByteCodec extends org.bson.codecs.ByteCodec {
        @Override
        public Byte decode(BsonReader reader, DecoderContext decoderContext) {
            return (byte) reader.readInt32();
        }
    }

    private static class ShortCodec extends org.bson.codecs.ShortCodec {
        @Override
        public Short decode(BsonReader reader, DecoderContext decoderContext) {
            return (short) reader.readInt32();
        }
    }
}
