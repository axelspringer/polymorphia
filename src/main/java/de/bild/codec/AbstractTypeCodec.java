package de.bild.codec;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;

/**
 * Abstract base class for any type codec.
 * @param <T> the value type
 */
public abstract class AbstractTypeCodec<T> implements TypeCodec<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTypeCodec.class);

    final TypeCodecRegistry typeCodecRegistry;
    final Class<T> encoderClass;
    final Constructor<T> defaultConstructor;
    final Type type;

    public AbstractTypeCodec(Type type, TypeCodecRegistry typeCodecRegistry) {
        this.typeCodecRegistry = typeCodecRegistry;
        this.type = type;
        this.encoderClass = extractClass(type);
        defaultConstructor = getDefaultConstructor(encoderClass);
    }


    @SuppressWarnings("unchecked")
    static <T> Class<T> extractClass(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class<T>) parameterizedType.getRawType();
        } else if (type instanceof Class) {
            return (Class<T>) type;
        } else if (type instanceof WildcardType) {
            // wildcard types are difficult to handle, especially if upper bound is Object.java
            WildcardType wildcardType = (WildcardType) type;
            //only use the upper bound for now - not very useful for <? super XYZ> though
            return extractClass(wildcardType.getUpperBounds()[0]);
        }
        throw new IllegalArgumentException("Type is not supported." + type);
    }

    @SuppressWarnings("unchecked")
    protected Constructor<T> getDefaultConstructor(Class<T> clazz) {
        //resolve constructor
        try {
            Constructor constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException | SecurityException e) {
            if (!Modifier.isStatic(clazz.getModifiers()) && clazz.getEnclosingClass() != null) {
                throw new IllegalArgumentException("Currently only static inner classes are supported! Consider using a static inner class instead for " + clazz);
            }
            throw new IllegalArgumentException("Please provide a default constructor for " + clazz + "!", e);
        }
    }

    public T newInstance() {
        try {
            return defaultConstructor.newInstance();
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            LOGGER.warn("Could not instantiate new instance {}", defaultConstructor.getName());
            throw new InstanceCreationException("Could not instantiate new instance for constructor: " + defaultConstructor.getName() + " Please provide default constructor.", e);
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        return encoderClass;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "{");
        sb.append("typeCodecRegistry=").append(typeCodecRegistry);
        sb.append(", encoderClass=").append(encoderClass);
        sb.append(", defaultConstructor=").append(defaultConstructor);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}