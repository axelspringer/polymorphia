package de.bild.codec;

import de.bild.codec.annotations.Id;
import de.bild.codec.annotations.LockingVersion;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ReflectionHelper {

    /**
     * calculates all fields of class hierarchy
     *
     * @param type              the value type
     * @param returnFinalFields indicate if final fields should be retuned as well
     * @return all declared and all inherited declared fields of given type
     */
    public static List<FieldTypePair> getDeclaredAndInheritedFieldTypePairs(final Type type, final boolean returnFinalFields) {
        ArrayList<FieldTypePair> list = new ArrayList<>();
        getFieldTypePairsRecursive(type, returnFinalFields, list, null);
        return list;
    }

    /**
     * Finds a field with fully resolved type within the given type
     * @param type any type generic or simply a class
     * @param fieldName a name of a field within that type
     * @return null, or the resolved FieldTypePair
     */
    public static FieldTypePair getDeclaredAndInheritedFieldTypePair(final Type type, String fieldName) {
        List<FieldTypePair> declaredAndInheritedFieldTypePairs = getDeclaredAndInheritedFieldTypePairs(type, true);
        for (FieldTypePair declaredAndInheritedFieldTypePair : declaredAndInheritedFieldTypePairs) {
            if (fieldName.equals(declaredAndInheritedFieldTypePair.getField().getName())) {
                return declaredAndInheritedFieldTypePair;
            }
        }
        return null;
    }

    private static void getFieldTypePairsRecursive(final Type type, final boolean returnFinalFields, List<FieldTypePair> currentList, Map<String, Type> realTypeMap) {
        if (type instanceof ParameterizedType) {
            getFieldTypePairsRecursive((ParameterizedType) type, returnFinalFields, currentList, realTypeMap);
        } else if (type instanceof Class) {
            getFieldTypePairsRecursive(TypeUtils.parameterize((Class) type, ((Class) type).getTypeParameters()), returnFinalFields, currentList, realTypeMap);
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type upperBoundType = wildcardType.getUpperBounds()[0];
            getFieldTypePairsRecursive(upperBoundType, returnFinalFields, currentList, realTypeMap);
        } else {
            throw new IllegalArgumentException("Unknown type." + type);
        }
    }

    /**
     * @param type
     * @param returnFinalFields
     * @param currentList
     * @param realTypeMap
     */
    private static void getFieldTypePairsRecursive(final ParameterizedType type, final boolean returnFinalFields, List<FieldTypePair> currentList, Map<String, Type> realTypeMap) {
        if (type.getRawType() == Object.class) {
            return;
        }

        Type[] actualTypeArguments = type.getActualTypeArguments();
        Type[] inferredTypeArguments = new Type[actualTypeArguments.length];
        for (int i = 0; i < actualTypeArguments.length; i++) {
            Type currentTypeParameter = actualTypeArguments[i];
            inferredTypeArguments[i] = inferRealType(currentTypeParameter, realTypeMap);
        }

        Class rawClass = (Class) type.getRawType();
        // we need to calculate the parameter map for the superclass
        Map<String, Type> clazzTypeParameterMap = calculateInferredTypeParameters(rawClass, inferredTypeArguments);

        getFieldTypePairsRecursive(rawClass.getGenericSuperclass(), returnFinalFields, currentList, clazzTypeParameterMap);

        List<Field> validFields = getValidFields(rawClass.getDeclaredFields(), returnFinalFields);
        for (Field validField : validFields) {
            currentList.add(new FieldTypePair(validField, inferRealType(validField.getGenericType(), clazzTypeParameterMap)));
        }
    }

    /**
     * Calculates the map of inferred type parameters for the given class based upon the given array of known type parameters from the class hierarchy
     *
     * @param clazz                the class to work on
     * @param actualTypeParameters array of type parameters
     * @return a Map of inferred types, that maps type variable names to real types
     */
    public static Map<String, Type> calculateInferredTypeParameters(Class clazz, Type[] actualTypeParameters) {
        Map<String, Type> newRealTypeMap = new HashMap<>();
        TypeVariable[] typeParameters = clazz.getTypeParameters();
        for (int i = 0; i < typeParameters.length; i++) {
            TypeVariable typeVariable = typeParameters[i];
            Type bound = typeVariable.getBounds()[0];
            if (actualTypeParameters != null) {
                bound = actualTypeParameters[i];
            }

            newRealTypeMap.put(typeVariable.getName(), inferRealType(bound, newRealTypeMap));
        }
        return newRealTypeMap;
    }

    public static Type inferRealType(Type type, Map<String, Type> realTypeMap) {
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type genericComponentType = genericArrayType.getGenericComponentType();
            return TypeUtils.genericArrayType(inferRealType(genericComponentType, realTypeMap));
        } else if (type instanceof ParameterizedType) {
            ParameterizedType bound = (ParameterizedType) type;
            List<Type> typeList = new ArrayList<>();
            for (int i = 0; i < bound.getActualTypeArguments().length; i++) {
                typeList.add(inferRealType(bound.getActualTypeArguments()[i], realTypeMap));
            }
            return TypeUtils.parameterizeWithOwner(bound.getOwnerType(), (Class<?>) bound.getRawType(), typeList.toArray(new Type[0]));
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            return TypeUtils.wildcardType()
                    .withLowerBounds(inferRealTypes(wildcardType.getLowerBounds(), realTypeMap))
                    .withUpperBounds(inferRealTypes(wildcardType.getUpperBounds(), realTypeMap)).build();
        } else if (type instanceof TypeVariable) {
            if (realTypeMap == null) {
                return ((TypeVariable) type).getBounds()[0];
            }

            Type typeFoundInMap = realTypeMap.get(type.getTypeName());
            if (typeFoundInMap != null) {
                return typeFoundInMap;
            }

            return type;
        }
        return type;
    }

    private static Type[] inferRealTypes(Type[] bounds, Map<String, Type> realTypeMap) {
        Type[] newBounds = new Type[bounds.length];
        for (int i = 0; i < bounds.length; i++) {
            newBounds[i] = inferRealType(bounds[i], realTypeMap);
        }
        return newBounds;
    }


    /**
     * Scans the array fields and returns any fields that are not static or (optionally) final.
     *
     * @param fields            the fields to process
     * @param returnFinalFields include final fields in the results
     * @return the valid fields
     */
    private static List<Field> getValidFields(final Field[] fields, final boolean returnFinalFields) {
        final List<Field> validFields = new ArrayList<Field>();

        for (final Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) && (returnFinalFields || !Modifier.isFinal(field.getModifiers()))) {
                validFields.add(field);
            }
        }
        return validFields;
    }

    public static List<MethodTypePair> getDeclaredAndInheritedMethods(final Type type) {
        List<MethodTypePair> list = new ArrayList<>();
        getMethodTypePairsRecursive(type, list, null);
        return list;
    }

    private static void getMethodTypePairsRecursive(final Type type, List<MethodTypePair> currentList, Map<String, Type> realTypeMap) {
        if (type instanceof ParameterizedType) {
            getMethodTypePairsRecursive((ParameterizedType) type, currentList, realTypeMap);
        } else if (type instanceof Class) {
            getMethodTypePairsRecursive(TypeUtils.parameterize((Class) type, ((Class) type).getTypeParameters()), currentList, realTypeMap);
        } else {
            throw new IllegalArgumentException("Unknown type." + type);
        }
    }

    private static void getMethodTypePairsRecursive(ParameterizedType type, List<MethodTypePair> currentList, Map<String, Type> realTypeMap) {
        if (type.getRawType() == Object.class) {
            return;
        }

        Type[] actualTypeArguments = type.getActualTypeArguments();
        Type[] inferredTypeArguments = new Type[actualTypeArguments.length];
        for (int i = 0; i < actualTypeArguments.length; i++) {
            Type currentTypeParameter = actualTypeArguments[i];
            inferredTypeArguments[i] = inferRealType(currentTypeParameter, realTypeMap);
        }

        Class rawClass = (Class) type.getRawType();
        // we need to calculate the parameter map for the superclass
        Map<String, Type> clazzTypeParameterMap = calculateInferredTypeParameters(rawClass, inferredTypeArguments);

        getMethodTypePairsRecursive(rawClass.getGenericSuperclass(), currentList, clazzTypeParameterMap);

        for (final Method method : rawClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                currentList.add(new MethodTypePair(method, inferRealType(method.getGenericReturnType(), clazzTypeParameterMap)));
            }
        }
    }

    /**
     * Returns the field with the given annotation if found in the given class, null otherwise
     *
     * @param clazz
     * @param annotationClass
     * @return the field with the given annotation
     */
    private static FieldTypePair getAnnotatedFieldIfPresent(final Class clazz, Class<? extends Annotation> annotationClass) {
        for (final FieldTypePair fieldTypePair : getDeclaredAndInheritedFieldTypePairs(clazz, true)) {
            Field field = fieldTypePair.getField();
            if (field.isAnnotationPresent(annotationClass)) {
                return fieldTypePair;
            }
        }
        return null;
    }

    public static FieldTypePair getIdFieldIfPresent(final Class clazz) {
        return getAnnotatedFieldIfPresent(clazz, Id.class);
    }

    public static FieldTypePair getLockingVersionFieldIfPresent(final Class clazz) {
        return getAnnotatedFieldIfPresent(clazz, LockingVersion.class);
    }

    /**
     * Example:
     * <pre>
     * {@code
     *      static class ValueClass {}
     *      static class SomeOtherClass<V extends ValueClass> extends ArrayList<V> implements List<V> {}
     *      static class SomeSpecializedList extends SomeOtherClass<ValueClass> {}
     *  }
     * </pre>
     *
     * @param type e.g. SomeSpecializedList
     * @param interfaceToFind example List.class
     * @return the parameterized interface, e.g. {@code List<ValueClass>}
     */
    public static Type findInterface(Type type, Class interfaceToFind) {
        return findInterface(type, interfaceToFind, null);
    }

    private static Type findInterface(Type type, Class interfaceToFind, Map<String, Type> realTypeMap) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return findInterface(parameterizedType, interfaceToFind, realTypeMap);
        } else if (type instanceof Class) {
            Class clazz = (Class) type;
            return findInterface(TypeUtils.parameterize(clazz, clazz.getTypeParameters()), interfaceToFind, realTypeMap);
        }
        return null;
    }

    private static Type findInterface(ParameterizedType type, Class interfaceToFind, Map<String, Type> realTypeMap) {
        if (type.getRawType() == Object.class) {
            return null;
        }

        Class rawClass = (Class) type.getRawType();

        if (rawClass == interfaceToFind) {
            return inferRealType(type, realTypeMap);
        } else {
            Type[] actualTypeArguments = type.getActualTypeArguments();
            Type[] inferredTypeArguments = new Type[actualTypeArguments.length];
            for (int i = 0; i < actualTypeArguments.length; i++) {
                Type currentTypeParameter = actualTypeArguments[i];
                inferredTypeArguments[i] = inferRealType(currentTypeParameter, realTypeMap);
            }
            // we need to calculate the parameter map for the superclass
            Map<String, Type> clazzTypeParameterMap = calculateInferredTypeParameters(rawClass, inferredTypeArguments);

            for (Type genericInterface : rawClass.getGenericInterfaces()) {
                Type foundInterface = findInterface(genericInterface, interfaceToFind, clazzTypeParameterMap);
                if (foundInterface != null) {
                    return foundInterface;
                }
            }
            return findInterface(rawClass.getGenericSuperclass(), interfaceToFind, clazzTypeParameterMap);
        }
    }

    /**
     * @param type type to be examined
     * @return The raw class of the given type or if type is a class, the class itself will be returned
     */
    public static Class extractRawClass(Type type) {
        if (type == null) {
            return null;
        }
        return TypeUtils.getRawType(type, null);
    }
}
