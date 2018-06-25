package de.bild.codec;

import org.bson.codecs.Codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class MapTypeCodec<K, V> extends AbstractTypeCodec<Map<K, V>> {
    final Codec<V> valueTypeCodec;

    public MapTypeCodec(Class<Map<K, V>> encoderClass, Type valueType, TypeCodecRegistry typeCodecRegistry) {
        super(encoderClass, typeCodecRegistry);
        this.valueTypeCodec = typeCodecRegistry.getCodec(valueType);
    }

    @Override
    protected Constructor<Map<K, V>> getDefaultConstructor(Class<Map<K, V>> clazz) {
        if (clazz.isInterface()) {
            if (SortedMap.class.isAssignableFrom(clazz)) {
                return super.getDefaultConstructor((Class) TreeMap.class);
            }
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

}