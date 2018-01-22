package de.bild.codec;


import de.bild.codec.annotations.Discriminator;
import de.bild.codec.annotations.Polymorphic;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final List<TypeCodecProvider> typeCodecProviders;
    private final CodecConfiguration codecConfiguration;

    private static final boolean MONGO_VERSION_LESS_THEN_3_5;

    static {
        boolean classPojoCodecProviderExists = false;
        try {
            Thread.currentThread().getContextClassLoader().loadClass("org.bson.codecs.pojo.PojoCodecProvider");
            classPojoCodecProviderExists = true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Assuming a mongo version < 3.5.");
        }

        MONGO_VERSION_LESS_THEN_3_5 = !classPojoCodecProviderExists;
    }

    /**
     * The TypeCodecProvider for all known standard types
     */
    private static final TypeCodecProvider DEFAULT_TYPE_CODEC_PROVIDER = new TypeCodecProvider() {
        @Override
        public <T> Codec<T> get(Type type, TypeCodecRegistry typeCodecRegistry) {
            if (TypeUtils.isArrayType(type)) {
                Codec<T> primitiveArrayCodec = ArrayCodec.PrimitiveArrayCodec.get(ReflectionHelper.extractRawClass(type));
                if (primitiveArrayCodec != null) {
                    return primitiveArrayCodec;
                } else {
                    return new ArrayCodec(type, typeCodecRegistry);
                }
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
            return null;
        }
    };


    PojoContext(final TypesModel typesModel,
                List<CodecResolver> codecResolvers,
                List<TypeCodecProvider> typeCodecProviders,
                CodecConfiguration codecConfiguration) {
        this.typesModel = typesModel;
        this.codecResolvers = codecResolvers;
        this.typeCodecProviders = typeCodecProviders;
        this.codecConfiguration = codecConfiguration;
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
            Codec<T> codec;
            //if type is a class, we ask the mongo codecregistry if it provides a specialized codec
            // side note: this PojoContext is chained within this codecRegistry and gets asked as well!!
            if (type instanceof Class) {
                codec = codecRegistry.get(ReflectionHelper.extractRawClass(type));
                // replace certain codecs as they do harm
                if (codec instanceof IterableCodec) {
                    codec = pojoContext.getCodec(type, this);
                } else if (MONGO_VERSION_LESS_THEN_3_5) {
                    // we need to ignore half implemented codecs for Float, Byte and Short
                    if (codec instanceof org.bson.codecs.FloatCodec ||
                            codec instanceof org.bson.codecs.ByteCodec ||
                            codec instanceof org.bson.codecs.ShortCodec) {
                        codec = pojoContext.getCodec(type, this);
                    }
                }

            } else {
                // if type is any other type than {@link Class}, there is no reason to ask codecRegistry
                // directly as anyway the class would need to be extracted from the type and we would loose type information
                // so in this case we do not ask the codecRegistry for potential more specialized codecs.
                // we ask the pojoContext directly
                codec = pojoContext.getCodec(type, this);
            }
            return codec;
        }

        @Override
        public CodecRegistry getRegistry() {
            return codecRegistry;
        }
    }


    public synchronized <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        return getCodec(clazz, new AnyTypeCodecRegistry(registry, this));
    }

    /**
     * Calculates and returns a codec for the given type, null otherwise
     *
     * @param type              type for which a codec is requested
     * @param typeCodecRegistry codec registry that can handle any type including parameterizd types, generic arrays, etc
     * @param <T>               the value type
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
     * Iterates over the list of codecResolvers and returns a PolymorphicCodec if match is found.
     * Codecs eligible to encode/decode sub classes of polymorphic structures need to provide special functionality and
     * can be registered during setup of the PojoCodecProvider {@link PojoCodecProvider.Builder#registerCodecResolver(CodecResolver[])}
     *
     * @param type              the value type
     * @param typeCodecRegistry codec registry that can handle any type including parameterizd types, generic arrays, etc
     * @return PolymorphicCodec if responsible resolver is found
     */
    public synchronized <T> PolymorphicCodec<T> resolve(Type type, TypeCodecRegistry typeCodecRegistry) {
        PolymorphicCodec<T> codec;
        for (CodecResolver codecResolver : codecResolvers) {
            codec = codecResolver.getCodec(type, typeCodecRegistry, codecConfiguration);
            if (codec != null) {
                return codec;
            }
        }
        // enums are special - a PolymorphicCodec for enums can be build on the fly {@link EnumReflectionCodecWrapper}
        if (TypeUtils.isAssignable(type, Enum.class)) {
            return new EnumReflectionCodecWrapper(typeCodecRegistry.getCodec(type));
        }
        // fallback is BasicReflectionCodec
        return new BasicReflectionCodec(type, typeCodecRegistry, codecConfiguration);
    }

    private static class EnumReflectionCodecWrapper<T extends Enum<T>> implements PolymorphicCodec<T> {
        final Codec<T> codec;

        EnumReflectionCodecWrapper(Codec<T> codec) {
            this.codec = codec;
        }

        public T decodeFields(BsonReader reader, DecoderContext decoderContext, T instance) {
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String fieldName = reader.readName();
                if ("enum".equals(fieldName)) {
                    return codec.decode(reader, decoderContext);
                } else {
                    reader.skipValue();
                }
            }
            // throw new EnumValueNotFoundException??? instead of returning null?
            return null;
        }

        @Override
        public void encodeFields(BsonWriter writer, T instance, EncoderContext encoderContext) {
            writer.writeName("enum");
            codec.encode(writer, instance, encoderContext);
        }

        @Override
        public T newInstance() {
            return null;
        }

        @Override
        public Class<T> getEncoderClass() {
            return codec.getEncoderClass();
        }

        @Override
        public void verifyFieldsNotNamedLikeAnyDiscriminatorKey(Set<String> discriminatorKeys) throws IllegalArgumentException {
            if (discriminatorKeys.contains("enum")) {
                throw new IllegalArgumentException("One of the discriminator keys equals the reserved word 'enum' " + discriminatorKeys);
            }
        }
    }

    /**
     * Will try to find an appropriate codec for the given type.
     *
     * @return Codec, if found or null, in case the type can or should not be handled by the pojo codec
     */
    private <T> Codec<T> calculateCodec(Type type, TypeCodecRegistry typeCodecRegistry) {

        Codec<T> codec;

        // first treat special types and custom codecs
        for (TypeCodecProvider typeCodecProvider : typeCodecProviders) {
            codec = typeCodecProvider.get(type, typeCodecRegistry);
            if (codec != null) {
                return codec;
            }
        }

        if (TypeUtils.isAssignable(type, Enum.class)) {
            return null;
        }

        // standard codec available ?
        codec = DEFAULT_TYPE_CODEC_PROVIDER.get(type, typeCodecRegistry);
        if (codec != null) {
            return codec;
        }

        /*
         * now try pojo codec with potentially polymorphic structures
         */
        Set<Type> validTypesForType = typesModel.getAssignableTypesWithinClassHierarchy(type);

        if (validTypesForType == null || validTypesForType.isEmpty()) {
            LOGGER.debug("Could not find concrete implementation for type {}. Maybe another codec is able to handle te class?!", type);
            return null;
        } else if (validTypesForType.size() > 1 || isPolymorphic(type)) {
            LOGGER.debug("Creating polymorphic codec for type {} with valid types {}", type, validTypesForType);
            return new PolymorphicReflectionCodec<>(type, validTypesForType, typeCodecRegistry, this);
        } else {
            LOGGER.debug("Creating simple reflection based codec for type {} (generic type {}) as only one concrete implementation known.", type, validTypesForType);
            Type singleType = validTypesForType.iterator().next();
            codec = resolve(singleType, typeCodecRegistry);
            if (codec != null) {
                return codec;
            }
        }
        return null;
    }


    /**
     * @param type to be checked for polymorphic structure
     * @return true, if type represents a polymorphic type structure
     */
    private boolean isPolymorphic(Type type) {
        Class clazz = ReflectionHelper.extractRawClass(type);
        return clazz.getAnnotation(Polymorphic.class) != null ||    // marked as polymorphic (@inherited annotation)
                clazz.getDeclaredAnnotation(Discriminator.class) != null || // explicit discriminator annotation (non @inherited)
                isClassPartOfPolymorphicStructureWithinTypesModel(clazz);
    }

    private boolean isClassPartOfPolymorphicStructureWithinTypesModel(Class clazz) {
        Class superclass = clazz.getSuperclass();
        // first check if superclass is part of typesModel, if so return true
        if (typesModel.getClassHierarchyNodeForType(superclass) != null) {
            return true;
        }
        // now check if any interface is part of the typesModel, if so, return true
        for (Class anInterface : clazz.getInterfaces()) {
            if (typesModel.getClassHierarchyNodeForType(anInterface) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Class is used internally to detect cycles.
     */
    private static class LazyCodec<T> implements TypeCodec<T> {
        private final Type type;
        private volatile Codec<T> wrapped;
        private final TypeCodecRegistry typeCodecRegistry;

        LazyCodec(final Type type, TypeCodecRegistry typeCodecRegistry) {
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