package de.bild.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
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


    /**
     * Tries to find the implemented interface List with the correct argument and if found returns the correct codec
     * @param type the type to be examined
     * @param typeCodecRegistry codec registry for any type
     * @return returns a ListTypeCodec if type is a list type or null otherwise
     */
    public static ListTypeCodec getCodecIfApplicable(Type type, TypeCodecRegistry typeCodecRegistry) {
        Class rawClass = ReflectionHelper.extractRawClass(type);
        if (rawClass != null && List.class.isAssignableFrom(rawClass)) {
            Type listInterface = ReflectionHelper.findInterface(type, List.class);
            if (listInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) listInterface;
                return new ListTypeCodec(rawClass, parameterizedType.getActualTypeArguments()[0], typeCodecRegistry);
            }
        }
        return null;
    }

}