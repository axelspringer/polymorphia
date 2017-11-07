package de.bild.codec;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class holds a list of all classes relevant for the pojo model.
 * Additionally a class hierarchy model is build to detect polymorphic structures.
 */
public class TypesModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(TypesModel.class);

    protected final Set<Class<?>> allClasses = new HashSet<>();
    protected final Map<Class<?>, ClassHierarchyNode> classHierarchy;

    public TypesModel(final Set<Class<?>> classes, final Set<String> packages) {

        // first index all direct classes
        if (classes != null) {
            for (Class<?> aClass : classes) {
                indexClass(aClass);
            }
        }

        if (packages != null && !packages.isEmpty()) {
            Indexer indexer = getIndexer();
            for (String aPackage : packages) {
                for (Class<?> clazz : indexer.getClassesForPackage(aPackage)) {
                    indexClass(clazz);
                }
            }
        }
        this.classHierarchy = buildClassHierarchy(allClasses);
    }

    interface Indexer {
        List<Class<?>> getClassesForPackage(String packageName);

        default Class<?> loadClass(Pattern classPattern, String resourceName, ClassLoader classLoader) {
            try {
                String resourcePathWithDots = resourceName.replace('/', '.');
                Matcher matcher = classPattern.matcher(resourcePathWithDots);
                if (matcher.matches()) {
                    return classLoader.loadClass(matcher.group(1));
                }
            } catch (Exception e) {
                LOGGER.warn("Could not load class {}", resourceName, e);
            }
            return null;
        }

        default Pattern getPatternForPackage(String packageName) {
            return Pattern.compile(".*?(" + packageName.replace(".", "\\.") + "\\..+)\\.class.*");
        }
    }

    /**
     * Depending on the library to resolve classes in packages this method returns an adequate indexer
     *
     * @return an indexer or throws {@link IllegalStateException}
     */
    private Indexer getIndexer() {
        // now depending on library, index packages
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            classLoader.loadClass("org.springframework.core.io.Resource");
            return new Indexer() {
                @Override
                public List<Class<?>> getClassesForPackage(String packageName) {
                    List<Class<?>> classes = new ArrayList<>();
                    try {
                        PathMatchingResourcePatternResolver scanner = new PathMatchingResourcePatternResolver();
                        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(packageName) + "/**/*.class";
                        Resource[] resources = scanner.getResources(pattern);

                        Pattern classPattern = getPatternForPackage(packageName);
                        for (Resource resource : resources) {
                            Class<?> aClass = loadClass(classPattern, resource.toString(), classLoader);
                            if (aClass != null) {
                                classes.add(aClass);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Could not load classes for package {}", packageName, e);
                    }
                    return classes;
                }
            };
        } catch (ClassNotFoundException e) {
            LOGGER.info("Could not find spring-core to use indexing packages, trying org.reflections.Reflections...");
            try {
                classLoader.loadClass("org.reflections.Reflections");
                return new Indexer() {
                    @Override
                    public List<Class<?>> getClassesForPackage(String packageName) {
                        List<Class<?>> classes = new ArrayList<>();
                        Reflections reflections = new Reflections(new ConfigurationBuilder()
                                .addUrls(ClasspathHelper.forPackage(packageName))
                                .setScanners(new ResourcesScanner()));
                        Set<String> resources = reflections.getResources(Pattern.compile(".*\\.class"));
                        Pattern classPattern = getPatternForPackage(packageName);
                        for (String resource : resources) {
                            Class<?> aClass = loadClass(classPattern, resource, classLoader);
                            if (aClass != null) {
                                classes.add(aClass);
                            }
                        }
                        return classes;
                    }
                };
            } catch (ClassNotFoundException e1) {
                LOGGER.error("Could not find org.reflections.reflections library in class path. Please provide either org.reflections.reflections or spring-core.");
                throw new IllegalStateException("Could not find org.reflections.reflections library in class path. Please provide either org.reflections.reflections or spring-core.");
            }
        }
    }

    private Map<Class<?>, ClassHierarchyNode> buildClassHierarchy(Set<Class<?>> allClasses) {
        Map<Class<?>, ClassHierarchyNode> clazzHierarchy = new HashMap<>();
        for (Class<?> aClass : allClasses) {
            addClassToHierarchy(aClass, clazzHierarchy);
        }
        return clazzHierarchy;
    }

    private ClassHierarchyNode addClassToHierarchy(Class<?> classToBeAdded, Map<Class<?>, ClassHierarchyNode> clazzHierarchy) {
        ClassHierarchyNode classHierarchyNode = clazzHierarchy.get(classToBeAdded);
        if (classHierarchyNode == null && allClasses.contains(classToBeAdded)) {
            classHierarchyNode = new ClassHierarchyNode(classToBeAdded);
            clazzHierarchy.put(classToBeAdded, classHierarchyNode);
            ClassHierarchyNode superNode = addClassToHierarchy(classToBeAdded.getSuperclass(), clazzHierarchy);
            if (superNode != null) {
                superNode.addChild(classHierarchyNode);
            }
            for (Class<?> anInterface : classToBeAdded.getInterfaces()) {
                ClassHierarchyNode interfaceNode = addClassToHierarchy(anInterface, clazzHierarchy);
                if (interfaceNode != null) {
                    interfaceNode.addChild(classHierarchyNode);
                }
            }
        }
        return classHierarchyNode;
    }


    private void indexClass(Class<?> clazz) {
        if (clazz.getEnclosingClass() == null ||
                (clazz.getEnclosingClass() != null && Modifier.isStatic(clazz.getModifiers()))) {
            boolean added = allClasses.add(clazz);
            if (added) {
                LOGGER.debug("Adding class to index: {}", clazz);
                for (Class<?> innerClass : clazz.getDeclaredClasses()) {
                    indexClass(innerClass);
                }
            }
        }
    }

    /**
     * @param type a type
     * @return the type hierarchy (lower bound within types model bounds) for the given type
     */
    protected ClassHierarchyNode getClassHierarchyNodeForType(Type type) {
        ClassHierarchyNode classHierarchyNode = null;
        Class<?> currentClass = ReflectionHelper.extractRawClass(type);
        // walk up class hierarchy until a class within the class model is found
        while (classHierarchyNode == null && currentClass != null && !Object.class.equals(currentClass)) {
            classHierarchyNode = classHierarchy.get(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        return classHierarchyNode;
    }

    /**
     *
     */
    public static class ClassHierarchyNode {
        Class<?> clazz;
        Set<ClassHierarchyNode> children = new HashSet<>();

        public ClassHierarchyNode(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public Set<ClassHierarchyNode> getChildren() {
            return children;
        }

        /**
         * @return true, if more than one concrete implementation are available, hence we need a polymorphic codec
         */
        public boolean isPolymorphic() {
            return getAllConcreteChildren().size() > 1;
        }

        public Set<Class<?>> getAllConcreteChildren() {
            return getAllChildrenRecursive(new HashSet<>());
        }

        private Set<Class<?>> getAllChildrenRecursive(Set<Class<?>> currentChildren) {
            if (!getClazz().isInterface()) {
                currentChildren.add(getClazz());
            }

            for (ClassHierarchyNode child : children) {
                child.getAllChildrenRecursive(currentChildren);
            }

            return currentChildren;
        }


        public boolean addChild(ClassHierarchyNode child) {
            return children.add(child);
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }
    }

    /**
     * This method is the core functionality to find polymorphic type structures.
     * Given a type, the set of all known classes will be searched for matching types.
     * A matching type for a given type only matches if all generic parameter conditions are met, more precise if the
     * potential valid type is assignable the original type.
     *
     * @param type the type for with sub types should be found
     * @return a set of matching types within know set of registered model classes
     */
    public Set<Type> getAssignableTypesWithinClassHierarchy(Type type) {
        Set<Type> validTypes = new HashSet<>();
        getAssignableTypesWithinClassHierarchy(type, validTypes);
        return validTypes;
    }

    private void getAssignableTypesWithinClassHierarchy(Type type, Set<Type> validTypes) {
        ClassHierarchyNode classHierarchyNodeForType = getClassHierarchyNodeForType(type);
        type = downGradeType(type, classHierarchyNodeForType);
        getAssignableTypesWithinClassHierarchy(type, classHierarchyNodeForType, validTypes);
    }

    /**
     * If the requested type is not registered within the class hierarchy it may still be persistable if a superclass is
     * registered. But then we need to find the type that is in the set of registered types.
     * @param type
     * @param classHierarchyNodeForType
     * @return
     */
    private Type downGradeType(Type type, ClassHierarchyNode classHierarchyNodeForType) {
        if (classHierarchyNodeForType == null) {
            return type;
        }
        Class<?> clazz = classHierarchyNodeForType.getClazz();

        // if the type is directly assignable, we can simply return the type

        if (TypeUtils.isAssignable(clazz, type)) {
            return type;
        }

        // now we need to downgrade type to clazz
        if (clazz.getTypeParameters().length > 0) {
            //if clazz has type parameters, we need to figure out the correct types
            return TypeUtils.parameterize(clazz, TypeUtils.getTypeArguments(type, clazz));
        }
        else {
            return clazz;
        }
    }


    /**
     * @param type               the type for with sub types should be found
     * @param classHierarchyNode the hierarchy of sub types
     * @param validTypes         a Set of found valid types so far
     */
    private void getAssignableTypesWithinClassHierarchy(Type type, ClassHierarchyNode classHierarchyNode, Set<Type> validTypes) {
        if (classHierarchyNode != null) {
            Class<?> clazz = classHierarchyNode.getClazz();
            Type matchingType = null;

            // first check general assignability
            // this does not mean that the parameterized clazz would be assignable to type since the parameter bounds may be wrong!
            if (TypeUtils.isAssignable(clazz, type)) {
                if (type instanceof ParameterizedType) {
                    matchingType = getMatchingType((ParameterizedType) type, clazz);
                } else {
                    matchingType = clazz;
                }
            }

            if (!clazz.isInterface() && matchingType != null) {
                validTypes.add(matchingType);
            }

            // if type match, walk down children
            if (matchingType != null) {
                for (ClassHierarchyNode child : classHierarchyNode.getChildren()) {
                    getAssignableTypesWithinClassHierarchy(matchingType, child, validTypes);
                }
            }
        }
    }

    /**
     * @param parameterizedType the type to match to
     * @param clazz             the class for which the correct parametrization is to be found
     * @return the parameterized type of clazz or null if no match
     */
    private Type getMatchingType(ParameterizedType parameterizedType, Class<?> clazz) {
        Type matchingType = null;
        if (parameterizedType.getRawType().equals(clazz)) {
            matchingType = parameterizedType;
        } else {
            // first find the superclass...may be an interface though
            Type genericSuperclass = null;
            if (ReflectionHelper.extractRawClass(parameterizedType).isInterface()) {
                for (Type genericInterface : clazz.getGenericInterfaces()) {
                    if (TypeUtils.isAssignable(genericInterface, parameterizedType)) {
                        genericSuperclass = genericInterface;
                        break;
                    }
                }
            } else {
                genericSuperclass = clazz.getGenericSuperclass();
            }


            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType parameterizedSuperClassType = (ParameterizedType) genericSuperclass;

                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                Type[] superClassTypeArguments = parameterizedSuperClassType.getActualTypeArguments();
                Map<String, Type> parameter = new HashMap<>();
                for (int i = 0; i < superClassTypeArguments.length; i++) {
                    Type classTypeArgument = superClassTypeArguments[i];
                    if (classTypeArgument instanceof TypeVariable) {
                        if (actualTypeArguments[i] instanceof WildcardType) {
                            WildcardType wildcardType = (WildcardType) actualTypeArguments[i];
                            parameter.put(((TypeVariable) classTypeArgument).getName(), wildcardType.getUpperBounds()[0]);
                        } else {
                            parameter.put(((TypeVariable) classTypeArgument).getName(), actualTypeArguments[i]);
                        }
                    }
                }

                TypeVariable<? extends Class<?>>[] typeParameters = clazz.getTypeParameters();
                Type[] specifiedTypeArguments = new Type[typeParameters.length];
                for (int i = 0; i < typeParameters.length; i++) {
                    Type inferredType = inferRealType(typeParameters[i], parameter);

                    if (TypeUtils.isAssignable(inferredType, typeParameters[i].getBounds()[0])) {
                        specifiedTypeArguments[i] = inferredType;
                    } else {
                        return null;
                    }
                }

                if (specifiedTypeArguments.length > 0) {
                    matchingType = TypeUtils.parameterize(clazz, specifiedTypeArguments);
                } else {
                    matchingType = clazz;
                }
            } else {
                LOGGER.debug("Type {} will be ignored as it has no generic superclass, but should have.", clazz);
            }
        }
        return matchingType;
    }


    /**
     * Assume the following class declaration:
     * <pre>
     * {@code
     * static class AList<P1, P2 extends Map<P1, Long>> implements AInterface<P1> {}
     * }
     * </pre>
     * <p>
     * For a given clazzTypeParameter ({@link TypeVariable} P1) and the map P1 -> Integer.class
     * a new Type {@code Integer} is returned.
     * <p>
     * For a given clazzTypeParameter ({@link TypeVariable} P2) and the map P1 -> Integer.class
     * a new Type {@code Map<Integer, Long>} is returned.
     *
     * @param clazzTypeParameter The TypeVariable of a given class
     * @param superClassTypeMap  the map of resolved values
     * @return a recursively resolved type
     */
    private static Type inferRealType(Type clazzTypeParameter, Map<String, Type> superClassTypeMap) {
        if (clazzTypeParameter instanceof ParameterizedType) {
            ParameterizedType bound = (ParameterizedType) clazzTypeParameter;
            List<Type> typeList = new ArrayList<>();
            for (int i = 0; i < bound.getActualTypeArguments().length; i++) {
                typeList.add(inferRealType(bound.getActualTypeArguments()[i], superClassTypeMap));
            }
            return TypeUtils.parameterizeWithOwner(bound.getOwnerType(), (Class<?>) bound.getRawType(), typeList.toArray(new Type[0]));
        } else if (clazzTypeParameter instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) clazzTypeParameter;
            Type newType = superClassTypeMap.get(typeVariable.getName());
            if (newType != null) {
                return newType;
            } else {
                return inferRealType(typeVariable.getBounds()[0], superClassTypeMap);
            }
        }
        return clazzTypeParameter;
    }


}
