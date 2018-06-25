package de.bild.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @param <C> collection type extending list
 * @param <V> value type
 */
public class ListTypeCodec<C extends List<V>, V> extends CollectionTypeCodec<C, V> {

    public ListTypeCodec(Class<C> collectionClass, Type valueType, TypeCodecRegistry typeCodecRegistry) {
        super(collectionClass, valueType, typeCodecRegistry);
    }

    @Override
    protected Constructor<C> getDefaultConstructor(Class<C> clazz) {
        if (clazz.isInterface()) {
            return super.getDefaultConstructor((Class) ArrayList.class);
        }
        return super.getDefaultConstructor(clazz);
    }

}