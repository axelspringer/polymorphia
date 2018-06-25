package de.bild.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @param <C> collection type extending set
 * @param <V> value type
 */
public class SetTypeCodec<C extends Set<V>, V> extends CollectionTypeCodec<C, V> {
    public SetTypeCodec(Class<C> collectionClass, Type valueType, TypeCodecRegistry typeCodecRegistry) {
        super(collectionClass, valueType, typeCodecRegistry);
    }

    @Override
    protected Constructor<C> getDefaultConstructor(Class<C> clazz) {
        if (clazz.isInterface()) {
            if (SortedSet.class.isAssignableFrom(clazz)) {
                return super.getDefaultConstructor((Class) TreeSet.class);
            }
            return super.getDefaultConstructor((Class) LinkedHashSet.class);
        }
        return super.getDefaultConstructor(clazz);
    }
}