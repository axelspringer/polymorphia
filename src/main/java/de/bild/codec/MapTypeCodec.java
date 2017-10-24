package de.bild.codec;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class MapTypeCodec<K, V> extends AbstractTypeCodec<Map<K, V>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexMapTypeCodec.class);
    final Codec<V> valueTypeCodec;

    public MapTypeCodec(Class<Map<K, V>> encoderClass, Type valueType, TypeCodecRegistry typeCodecRegistry) {
        super(encoderClass, typeCodecRegistry);
        this.valueTypeCodec = typeCodecRegistry.getCodec(valueType);
    }

    @Override
    protected Constructor<Map<K, V>> getDefaultConstructor(Class<Map<K, V>> clazz) {
        if (clazz.isInterface()) {
            return super.getDefaultConstructor((Class) LinkedHashMap.class);
        }
        return super.getDefaultConstructor(clazz);
    }

    @Override
    public Map<K, V> defaultInstance() {
        return newInstance();
    }

    @Override
    public Class<Map<K, V>> getEncoderClass() {
        return encoderClass;
    }

    public static MapTypeCodec getCodecIfApplicable(Type type, TypeCodecRegistry typeCodecRegistry) {
        Class rawClass = ReflectionHelper.extractRawClass(type);
        // @link{org.bson.Document} will be handled by java driver directly and not by this map implementation
        if (rawClass != null && !Document.class.isAssignableFrom(rawClass)) {
            if (Map.class.isAssignableFrom(rawClass)) {
                Type setInterface = ReflectionHelper.findInterface(type, Map.class);
                if (setInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) setInterface;
                    if (parameterizedType.getActualTypeArguments()[0].equals(String.class)) {
                        return new SimpleMapTypeCodec(rawClass, parameterizedType.getActualTypeArguments()[1], typeCodecRegistry);
                    } else {
                        return new ComplexMapTypeCodec(rawClass, parameterizedType.getActualTypeArguments()[0], parameterizedType.getActualTypeArguments()[1], typeCodecRegistry);
                    }
                }
            }
        }
        return null;
    }
}