package de.bild.codec;


import com.google.common.reflect.AbstractInvocationHandler;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Provides a codec for Pojos
 * Use the internal builder to register classes and packages that can be handled by the codec
 */
public class PojoCodecProvider implements CodecProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PojoCodecProvider.class);
    private final TypesModel typesModel;
    private final PojoContext pojoContext;

    PojoCodecProvider(final Set<Class<?>> classes, final Set<String> packages, final Set<Class<? extends Annotation>> ignoreAnnotations, final List<CodecResolver> codecResolvers) {
        this.typesModel = new TypesModel(classes, packages, ignoreAnnotations);
        this.pojoContext = new PojoContext(typesModel, codecResolvers);
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
                Class[] proxyInterfaces = new Class[]{CollectibleCodec.class};
                CollectibleCodec collectibleCodec = (CollectibleCodec) Proxy.newProxyInstance(
                        PojoCodecProvider.class.getClassLoader(),
                        proxyInterfaces,
                        new CollectibleCodecDelegator(typeCodec));

                return collectibleCodec;
            }
        }
        return codec;
    }

    /**
     * delegator for CollectibleCodec
     */
    private static class CollectibleCodecDelegator<T> extends AbstractInvocationHandler implements CollectibleCodec<T> {
        private final TypeCodec<T> delegatingCodec;

        public CollectibleCodecDelegator(TypeCodec<T> delegatingCodec) {
            this.delegatingCodec = delegatingCodec;
        }


        @Override
        protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(this, args);
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
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
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

        private Builder() {
        }

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

        public Builder ignoreTypesAnnotatedWith(Class<? extends Annotation>... annotations) {
            this.ignoreAnnotations.addAll(Arrays.asList(annotations));
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
            return new PojoCodecProvider(classes, packages, ignoreAnnotations, codecResolvers);
        }
    }
}
