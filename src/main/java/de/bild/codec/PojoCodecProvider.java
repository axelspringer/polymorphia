package de.bild.codec;


import de.bild.codec.annotations.DecodeUndefinedHandlingStrategy;
import de.bild.codec.annotations.EncodeNullHandlingStrategy;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Predicate;

/**
 * Provides a codec for Pojos
 * Use the internal builder to register classes and packages that can be handled by the codec
 */
public class PojoCodecProvider implements CodecProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PojoCodecProvider.class);
    private final TypesModel typesModel;
    private final PojoContext pojoContext;


    PojoCodecProvider(final Set<Class<?>> classes,
                      final Set<String> packages,
                      final Set<Class<? extends Annotation>> ignoreAnnotations,
                      Set<Predicate<String>> ignoreTypesMatchingClassNamePredicates,
                      Set<Class<?>> ignoreClasses, List<TypeCodecProvider> typeCodecProviders,
                      final List<CodecResolver> codecResolvers,
                      CodecConfiguration codecConfiguration, ClassResolver classResolver) {
        this.typesModel = new TypesModel(classes, packages, ignoreAnnotations, ignoreTypesMatchingClassNamePredicates, ignoreClasses, classResolver);
        this.pojoContext = new PojoContext(typesModel, codecResolvers, typeCodecProviders, codecConfiguration);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        // if clazz has type parameters, we warn the user that generic class definitions are problematic
        Codec<T> codec = pojoContext.get(clazz, registry);
        if (codec instanceof TypeCodec) {
            if (clazz != null && clazz.getTypeParameters().length > 0) {
                LOGGER.warn("Generic classes will only be encoded/decoded with their upper bounds! " +
                        "We could prohibit handling of the pojo codec for those generic classes, " +
                        "but then user would loose flexibility when subclassing such classes. Class: " + clazz.toGenericString());
            }
            TypeCodec typeCodec = (TypeCodec) codec;
            // generate dynamic proxy to add CollectibleCodec functionality
            if (typeCodec.isCollectible()) {
                LOGGER.debug("Enhancing {} to be collectible codec.", typeCodec);
                // For easy of use, adding all implemented interfaces to the proxy, so the proxy can be used for most use cases
                // Unfortunately the functionalities of the underlying concrete codec class will be missing.
                ArrayList<Class<?>> proxyInterfaceList = new ArrayList<>(Arrays.asList(typeCodec.getClass().getInterfaces()));
                proxyInterfaceList.add(CollectibleCodec.class);
                proxyInterfaceList.add(DelegatingCodec.class); // so users can retrieve the delegating codec form the proxy.

                CollectibleCodec collectibleCodec = (CollectibleCodec) Proxy.newProxyInstance(
                        PojoCodecProvider.class.getClassLoader(),
                        proxyInterfaceList.toArray(new Class<?>[1]),
                        new CollectibleCodecDelegator(typeCodec));

                return collectibleCodec;
            }
        }
        return codec;
    }

    /**
     * delegator for CollectibleCodec
     */
    private static class CollectibleCodecDelegator<T> implements InvocationHandler, CollectibleCodec<T>, DelegatingCodec<T> {
        private final TypeCodec<T> delegatingCodec;
        private static final Object[] NO_ARGS = {};

        public CollectibleCodecDelegator(TypeCodec<T> delegatingCodec) {
            this.delegatingCodec = delegatingCodec;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                if (method.getDeclaringClass() == CollectibleCodec.class || method.getDeclaringClass() == DelegatingCodec.class) {
                    return method.invoke(this, args);
                } else {
                    return method.invoke(delegatingCodec, args);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.warn("An exception was caught while invoking the delegate {} with args {}", method, args);
                LOGGER.debug("Original exception when invoking target.", e);
                // rethrowing cause instead of invocationexception
                throw e.getCause();
            }

        }

        @Override
        public T generateIdIfAbsentFromDocument(T document) {
            return delegatingCodec.generateIdIfAbsentFromDocument(document);
        }

        @Override
        public boolean documentHasId(T document) {
            return delegatingCodec.documentHasId(document);
        }

        @Override
        public BsonValue getDocumentId(T document) {
            return delegatingCodec.getDocumentId(document);
        }

        @Override
        public T decode(BsonReader reader, DecoderContext decoderContext) {
            return delegatingCodec.decode(reader, decoderContext);
        }

        @Override
        public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
            delegatingCodec.encode(writer, value, encoderContext);
        }

        @Override
        public Class<T> getEncoderClass() {
            return delegatingCodec.getEncoderClass();
        }

        @Override
        public TypeCodec<T> getDelegate() {
            return delegatingCodec;
        }
    }

    /**
     *
     */
    public static class Builder {
        private Set<String> packages = new HashSet<>();
        private Set<Class<?>> classes = new HashSet<>();
        private List<CodecResolver> codecResolvers = new ArrayList<>();
        private Set<Class<? extends Annotation>> ignoreAnnotations = new HashSet<>();
        private Set<Predicate<String>> ignoreTypesMatchingClassNamePredicates = new HashSet<>();
        private Set<Class<?>> ignoreClasses = new HashSet<>();
        private ClassResolver classResolver;
        private List<TypeCodecProvider> typeCodecProviders = new ArrayList<>();
        private EncodeNullHandlingStrategy.Strategy encodeNullHandlingStrategy = EncodeNullHandlingStrategy.Strategy.CODEC;
        private DecodeUndefinedHandlingStrategy.Strategy decodeUndefinedHandlingStrategy = DecodeUndefinedHandlingStrategy.Strategy.KEEP_POJO_DEFAULT;
        private boolean encodeNulls = false;

        public Builder setPackages(Set<String> packages) {
            this.packages = packages;
            return this;
        }

        public Builder register(String... packages) {
            this.packages.addAll(Arrays.asList(packages));
            return this;
        }

        public Builder register(Class<?>... classes) {
            this.classes.addAll(Arrays.asList(classes));
            return this;
        }

        /**
         * If you need to provide a mechanism to scan packages for model classes, register a {@link ClassResolver}
         * @param classResolver the resolver for classes within packages
         * @return this Builder
         */
        public Builder registerClassResolver(ClassResolver classResolver) {
            this.classResolver = classResolver;
            return this;
        }

        public Builder ignoreTypesAnnotatedWith(Class<? extends Annotation>... annotations) {
            this.ignoreAnnotations.addAll(Arrays.asList(annotations));
            return this;
        }

        /**
         * If you need to exclude private inner classes form the domain model, use a Predicate
         * @param ignoreTypesMatchingClassNamePredicates
         * @return the Builder
         */
        public Builder ignoreTypesMatchingClassNamePredicate(Predicate<String>... ignoreTypesMatchingClassNamePredicates) {
            this.ignoreTypesMatchingClassNamePredicates.addAll(Arrays.asList(ignoreTypesMatchingClassNamePredicates));
            return this;
        }

        /**
         * If ypu can point to the classes to be ignored, you can do this here
         * @return the Builder
         */
        public Builder ignoreClasses(Class<?>... ignoreClasses) {
            this.ignoreClasses.addAll(Arrays.asList(ignoreClasses));
            return this;
        }

        /**
         * In case you need to register
         *
         * @param typeCodecProviders
         * @return
         */
        public Builder register(TypeCodecProvider... typeCodecProviders) {
            this.typeCodecProviders.addAll(Arrays.asList(typeCodecProviders));
            return this;
        }

        public Builder encodeNullHandlingStrategy(EncodeNullHandlingStrategy.Strategy encodeNullHandlingStrategy) {
            if (encodeNullHandlingStrategy != null) {
                this.encodeNullHandlingStrategy = encodeNullHandlingStrategy;
            }
            return this;
        }

        public Builder decodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy decodeUndefinedHandlingStrategy) {
            if (decodeUndefinedHandlingStrategy != null) {
                this.decodeUndefinedHandlingStrategy = decodeUndefinedHandlingStrategy;
            }
            return this;
        }

        public Builder encodeNulls(boolean encodeNulls) {
            this.encodeNulls = encodeNulls;
            return this;
        }

        /**
         * A CodecResolver is supposed to provide specialized codecs in case the default implementation
         * {@link BasicReflectionCodec} is not sufficient
         *
         * @param codecResolvers a list of CodecResolvers to be registered
         * @return the builder
         */
        public Builder registerCodecResolver(CodecResolver... codecResolvers) {
            this.codecResolvers.addAll(Arrays.asList(codecResolvers));
            return this;
        }

        public PojoCodecProvider build() {
            CodecConfiguration codecConfiguration = CodecConfiguration.builder()
                    .decodeUndefinedHandlingStrategy(decodeUndefinedHandlingStrategy)
                    .encodeNullHandlingStrategy(encodeNullHandlingStrategy)
                    .encodeNulls(encodeNulls)
                    .build();
            return new PojoCodecProvider(classes, packages, ignoreAnnotations, ignoreTypesMatchingClassNamePredicates, ignoreClasses, typeCodecProviders, codecResolvers, codecConfiguration, classResolver);
        }
    }
}
