package de.bild.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

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
            return super.getDefaultConstructor((Class) LinkedHashSet.class);
        }
        return super.getDefaultConstructor(clazz);
    }


    /**
     * Tries to find the implemented interface List with the correct argument and if found returns the correct codec
     * @param type the type to be examined
     * @param typeCodecRegistry codec registry for any type
     * @return returns a SetTypeCodec if type is a set type or null otherwise
     */
    public static SetTypeCodec getCodecIfApplicable(Type type, TypeCodecRegistry typeCodecRegistry) {
        Class rawClass = ReflectionHelper.extractRawClass(type);

        if (rawClass != null && Set.class.isAssignableFrom(rawClass)) {
            Type setInterface = ReflectionHelper.findInterface(type, Set.class);
            if (setInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) setInterface;
                return new SetTypeCodec(rawClass, parameterizedType.getActualTypeArguments()[0], typeCodecRegistry);
            }
        }
        return null;
    }
}