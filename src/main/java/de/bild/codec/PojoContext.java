package de.bild.codec;


import de.bild.codec.annotations.Discriminator;
import de.bild.codec.annotations.Polymorphic;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.IterableCodec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PojoContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(PojoContext.class);

    private final Map<Type, Codec<?>> codecMap = new ConcurrentHashMap<>();
    private final TypesModel typesModel;
    private final List<CodecResolver> codecResolvers;
    private final List<TypeCodecProvider> typeCodecProviders;
    private final CodecConfiguration codecConfiguration;


    /**
     * The TypeCodecProvider for all known standard types
     */
    //@SuppressWarnings("unchecked")
    private static final TypeCodecProvider DEFAULT_TYPE_CODEC_PROVIDER = new TypeCodecProvider() {
        @Override
        public <T> Codec<T> get(Type type, TypeCodecRegistry typeCodecRegistry) {
            if (TypeUtils.isArrayType(type)) {
                PrimitiveArrayCodec primitiveArrayCodec = PrimitiveArrayCodec.get(ReflectionHelper.extractRawClass(type));
                if (primitiveArrayCodec != null) {
                    return primitiveArrayCodec;
                } else {
                    return new ArrayCodec<>(type, typeCodecRegistry);
                }
            } else if (type instanceof TypeVariable) {
                throw new IllegalArgumentException("This registry (and probably no other one as well) can not handle generic type variables.");
            } else if (type instanceof WildcardType) {
                LOGGER.error("WildcardTypes are not yet supported. {}", type);
                throw new NotImplementedException("WildcardTypes are not yet supported. " + type);
            } else if (TypeUtils.isAssignable(type, SpecialFieldsMap.class)) {
                return new SpecialFieldsMapCodec(type, typeCodecRegistry);
            }
            // default enum Codec if user did not register any
            else if (TypeUtils.isAssignable(type, Enum.class)) {
                return new EnumCodec(ReflectionHelper.extractRawClass(type));
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

        // if there is a standard codec provided for a type, that needs to be stored along with
        // polymorphic Type information "_t", we need to wrap that codec
        /** Alternatively a user could always provide a {@link CodecResolver} in order to override this default handling
         * Additionally a user can register a {@link PolymorphicCodec} within teh CodecRegistry chain.
         * That codec can be used to encode/decode a type in polymorphic contexts as well as non-polymorphic contexts
         * For an example: {@link AnyThingTest} **/
        // if codecMap contains a mapping for type (may be LazyCodec), this means, that within the chain of registered codecs,
        // there is no codec able to handle the type -> so we need to skip this part and create a reflection based codec
        if (!codecMap.containsKey(type)) {
            Codec<T> standardCodec = typeCodecRegistry.getCodec(type);
            if (standardCodec instanceof PolymorphicCodec) {
                return (PolymorphicCodec) standardCodec; // lovely, user provided a PolymorphicCodec
            } else if (!(standardCodec instanceof TypeCodec)) {
                return new PolymorphicCodecWrapper<>(standardCodec);
            }
        }

        // if user has not registered a default Codec for Enums, use the internal one
        if (TypeUtils.isAssignable(type, Enum.class)) {
            return new PolymorphicCodecWrapper(new EnumCodec(ReflectionHelper.extractRawClass(type)));
        }


        // fallback is BasicReflectionCodec
        return new BasicReflectionCodec(type, typeCodecRegistry, codecConfiguration);
    }

    private static class PolymorphicCodecWrapper<T> implements PolymorphicCodec<T> {
        final Codec<T> codec;

        public PolymorphicCodecWrapper(Codec<T> codec) {
            this.codec = codec;
        }

        @Override
        public T decodeFields(BsonReader reader, DecoderContext decoderContext, T instance) {
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String fieldName = reader.readName();
                if ("data".equals(fieldName)) {
                    return codec.decode(reader, decoderContext);
                } else {
                    reader.skipValue();
                }
            }
            return null;
        }

        @Override
        public void encodeFields(BsonWriter writer, T instance, EncoderContext encoderContext) {
            writer.writeName("data");
            codec.encode(writer, instance, encoderContext);
        }

        @Override
        public T newInstance() {
            return null;
        }

        @Override
        public void verifyFieldsNotNamedLikeAnyDiscriminatorKey(Set<String> discriminatorKeys) throws IllegalArgumentException {
            if (discriminatorKeys.contains("data")) {
                throw new IllegalArgumentException("One of the discriminator keys equals the reserved word 'data' " + discriminatorKeys);
            }
        }

        @Override
        public Class<T> getEncoderClass() {
            return codec.getEncoderClass();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("PolymorphicCodecWrapper{");
            sb.append("codec=").append(codec);
            sb.append('}');
            return sb.toString();
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

        // standard codec available ?
        codec = DEFAULT_TYPE_CODEC_PROVIDER.get(type, typeCodecRegistry);
        if (codec != null) {
            return codec;
        }

        Class rawClass = ReflectionHelper.extractRawClass(type);
        if (rawClass != null && List.class.isAssignableFrom(rawClass)) {
            // List ?
            Type listInterface = ReflectionHelper.findInterface(type, List.class);
            if (listInterface instanceof ParameterizedType && !TypeUtils.containsTypeVariables(listInterface)) {
                ParameterizedType parameterizedType = (ParameterizedType) listInterface;
                try {
                    codec = new ListTypeCodec(rawClass, parameterizedType.getActualTypeArguments()[0], typeCodecRegistry);
                } catch (CodecConfigurationException cce) {
                    // unfortunately there is no elegant way to figure out, if a codec for the valueType can be build within the codecRegistry
                    // if all codecs have been tested an exception will be raised, this then indicates, that the mongo java driver codecs for lists need to be chosen
                    LOGGER.debug("Can't create advanced (generic list) codec for {}. Fall back to mongo db driver defaults.", parameterizedType, cce.getMessage());
                }
            }
        } else if (rawClass != null && Set.class.isAssignableFrom(rawClass)) {
            // Set ?
            Type setInterface = ReflectionHelper.findInterface(type, Set.class);
            if (setInterface instanceof ParameterizedType && !TypeUtils.containsTypeVariables(setInterface)) {
                ParameterizedType parameterizedType = (ParameterizedType) setInterface;
                try {
                    codec = new SetTypeCodec(rawClass, parameterizedType.getActualTypeArguments()[0], typeCodecRegistry);
                } catch (CodecConfigurationException cce) {
                    // unfortunately there is no elegant way to figure out, if a codec for the valueType can be build within the codecRegistry
                    // if all codecs have been tested an exception will be raised, this then indicates, that the mongo java driver codecs for sets need to be chosen
                    LOGGER.debug("Can't create advanced (generic set) codec for {}. Fall back to mongo db driver defaults.", parameterizedType, cce.getMessage());
                }
            }
        } else if (Document.class.isAssignableFrom(rawClass)) {
            // if Document.class let mongo java driver handle this class directly
            codec = null;
        } else if (rawClass != null && Map.class.isAssignableFrom(rawClass)) {
            // Map ?
            Type mapInterface = ReflectionHelper.findInterface(type, Map.class);
            if (mapInterface instanceof ParameterizedType && !TypeUtils.containsTypeVariables(mapInterface)) {
                ParameterizedType parameterizedType = (ParameterizedType) mapInterface;
                Type keyType = parameterizedType.getActualTypeArguments()[0];
                Type valueType = parameterizedType.getActualTypeArguments()[1];
                try {
                    if (keyType.equals(String.class)) {
                        codec = new SimpleMapTypeCodec(rawClass, valueType, typeCodecRegistry);
                    } else {
                        codec = new ComplexMapTypeCodec(rawClass, keyType, valueType, typeCodecRegistry);
                    }
                } catch (CodecConfigurationException cce) {
                    // unfortunately there is no elegant way to figure out, if a codec for the valueType can be build within the codecRegistry
                    // if all codecs have been tested an exception will be raised, this then indicates, that the mongo java driver codecs for maps need to be chosen
                    LOGGER.debug("Can't create advanced (generic map) codec for {}. Fall back to mongo db driver defaults.", parameterizedType, cce.getMessage());
                }
            }
        } else {
            /*
             * now try pojo codec with potentially polymorphic structures
             */
            Set<Type> validTypesForType = typesModel.getAssignableTypesWithinClassHierarchy(type);

            if (validTypesForType == null || validTypesForType.isEmpty()) {
                LOGGER.debug("Could not find codec for type {}. Maybe another codec or codec provider is able to handle te class?!", type);
                return null;
            } else if (validTypesForType.size() > 1 || isPolymorphic(type, validTypesForType.iterator().next())) {
                LOGGER.debug("Creating polymorphic codec for type {} with valid types {}", type, validTypesForType);
                return new PolymorphicReflectionCodec<>(type, validTypesForType, typeCodecRegistry, this);
            } else {
                LOGGER.debug("Creating simple reflection based codec for type {} (generic type {}) as only one concrete implementation known.", type, validTypesForType);
                Type singleType = validTypesForType.iterator().next();
                codec = resolve(singleType, typeCodecRegistry);
            }
        }
        return codec;
    }


    /**
     * @param type            to be checked for polymorphic structure
     * @param singleValidType
     * @return true, if type represents a polymorphic type structure
     */
    private boolean isPolymorphic(Type type, Type singleValidType) {
        Class typeClazz = ReflectionHelper.extractRawClass(type);
        Class singleValidClass = ReflectionHelper.extractRawClass(singleValidType);
        return typeClazz.isInterface() || //if type is an interface and there is only one singleValidType -> polymorphic
                singleValidClass.getAnnotation(Polymorphic.class) != null ||    // marked as polymorphic (@inherited annotation)
                singleValidClass.getDeclaredAnnotation(Discriminator.class) != null || // explicit discriminator annotation (non @inherited)
                isClassPartOfPolymorphicStructureWithinTypesModel(typeClazz);
    }

    private boolean isClassPartOfPolymorphicStructureWithinTypesModel(Class clazz) {
        Class superclass = clazz.getSuperclass();
        // first check if superclass is part of typesModel, if so return true
        // Reason: for the current field, since only one valid type exists, non-polymorphic structure could be assumed, but since
        // there might be pojos, that declare its type further up the type hierarchy and want to decode the same data from the database, discriminators are needed
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
    private static class LazyCodec<T> implements TypeCodec<T>, DelegatingCodec<T> {
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

        @Override
        public Codec<T> getDelegate() {
            return getWrapped();
        }
    }
}