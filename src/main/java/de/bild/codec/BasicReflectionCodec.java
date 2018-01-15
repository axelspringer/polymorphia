package de.bild.codec;

import de.bild.codec.annotations.Id;
import de.bild.codec.annotations.PostLoad;
import de.bild.codec.annotations.PreSave;
import de.bild.codec.annotations.Transient;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

public class BasicReflectionCodec<T> extends AbstractTypeCodec<T> implements ReflectionCodec<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicReflectionCodec.class);
    MappedField<T, Object> idField;

    /**
     * a list of the fields to map
     */
    final Map<String, MappedField> persistenceFields = new LinkedHashMap<>();
    final List<Method> postLoadMethods = new ArrayList<>();
    final List<Method> preSaveMethods = new ArrayList<>();
    IdGenerator idGenerator;
    boolean isCollectible;

    public BasicReflectionCodec(Type type, TypeCodecRegistry typeCodecRegistry, CodecConfiguration codecConfiguration) {
        super(type, typeCodecRegistry);
        // resolve all persistable fields
        for (final FieldTypePair fieldTypePair : ReflectionHelper.getDeclaredAndInheritedFieldTypePairs(type, true)) {
            Field field = fieldTypePair.getField();
            if (!isIgnorable(field)) {
                MappedField<T, Object> mappedField = new MappedField<>(fieldTypePair, encoderClass, typeCodecRegistry, codecConfiguration);
                persistenceFields.put(mappedField.getMappedFieldName(), mappedField);
                if (mappedField.isIdField()) {
                    if (idField == null) {
                        idField = mappedField;
                        Id idAnnotation = idField.getAnnotation(Id.class);

                        Class<? extends IdGenerator> idGeneratorClass = idAnnotation.value();
                        isCollectible = idAnnotation.collectible();
                        try {
                            Constructor<? extends IdGenerator> idGeneratorConstructor = idGeneratorClass.getDeclaredConstructor();
                            idGeneratorConstructor.setAccessible(true);
                            idGenerator = idGeneratorConstructor.newInstance();
                        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                            throw new IllegalArgumentException("Could not create instance of IdGenerator for class " + type + " Generator class: " + idGeneratorClass, e);
                        }
                    } else {
                        throw new IllegalArgumentException("Id field is annotated multiple times in class hierarchy! Class " + encoderClass);
                    }
                }
            }
        }


        // resolve lifecycle methods
        for (MethodTypePair methodTypePair : ReflectionHelper.getDeclaredAndInheritedMethods(encoderClass)) {
            Method method = methodTypePair.getMethod();
            if (method.isAnnotationPresent(PostLoad.class)) {
                postLoadMethods.add(method);
            } else if (method.isAnnotationPresent(PreSave.class)) {
                preSaveMethods.add(method);
            }
        }
    }

    @Override
    public Map<String, MappedField> getPersistenceFields() {
        return persistenceFields;
    }


    protected boolean isIgnorable(final Field field) {
        return field.isAnnotationPresent(Transient.class)
                || Modifier.isTransient(field.getModifiers());
    }

    @Override
    public T decodeFields(BsonReader reader, DecoderContext decoderContext, T instance) {
        Set<String> fieldNames = new HashSet<>(persistenceFields.keySet());

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            MappedField mappedField = persistenceFields.get(fieldName);
            if (mappedField != null) {
                fieldNames.remove(fieldName);
                mappedField.decode(reader, instance, decoderContext);
            } else {
                reader.skipValue();
            }
        }

        // for all non-found (undefined) fields, run initialization
        for (String fieldName : fieldNames) {
            persistenceFields.get(fieldName).initializeUndefinedValue(instance);
        }
        postDecode(instance);
        return instance;
    }

    @Override
    public void postDecode(T instance) {
        for (Method postLoadMethod : postLoadMethods) {
            try {
                postLoadMethod.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.warn("@PostLoad method {} could not be called.", postLoadMethod, e);
            }
        }
    }

    @Override
    public void encodeFields(BsonWriter writer, T instance, EncoderContext encoderContext) {
        preEncode(instance);
        for (MappedField persistenceField : persistenceFields.values()) {
            persistenceField.encode(writer, instance, encoderContext);
        }
    }

    @Override
    public void preEncode(T instance) {
        for (Method preSaveMethod : preSaveMethods) {
            try {
                preSaveMethod.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.warn("@PreSave method {} could not be called.", preSaveMethod, e);
            }
        }
    }

    @Override
    public MappedField getMappedField(String mappedFieldName) {
        return persistenceFields.get(mappedFieldName);
    }

    @Override
    public boolean isCollectible() {
        return isCollectible;
    }

    @Override
    public T generateIdIfAbsentFromDocument(T document) {
        if (idGenerator != null && !documentHasId(document)) {
            Object generatedId = idGenerator.generate();
            try {
                if (!idField.setFieldValue(document, generatedId)) {
                   LOGGER.error("Id {} for pojo {} could not be set. Please watch the logs.", generatedId, document);
                   throw new IdGenerationException("Id could not be generated for pojo. See logs for details.");
                }
            } catch (TypeMismatchException e) {
                if (generatedId != null && !TypeUtils.isAssignable(generatedId.getClass(), idField.fieldTypePair.realType)) {
                    LOGGER.error("Your set id generator {} for the id field {} produces non-assignable values.", idGenerator, idField, e);
                }
                else {
                    LOGGER.error("Some unspecified error occurred while generating an id {} for your pojo {}", generatedId, document);
                }
                throw new IdGenerationException("Id could not be generated for pojo. See logs for details.", e);
            }
        }
        return document;
    }

    @Override
    public boolean documentHasId(T document) {
        return getPlainId(document) != null;
    }

    private Object getPlainId(T document) {
        return idField != null ? idField.getFieldValue(document) : null;
    }

    @Override
    public BsonValue getDocumentId(T document) {
        return idGenerator.asBsonValue(getPlainId(document), typeCodecRegistry);
    }
}