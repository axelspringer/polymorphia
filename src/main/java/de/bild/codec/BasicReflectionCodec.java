package de.bild.codec;

import de.bild.codec.annotations.Id;
import de.bild.codec.annotations.PostLoad;
import de.bild.codec.annotations.Transient;
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
    MappedField idField;

    /**
     * a list of the fields to map
     */
    final Map<String, MappedField> persistenceFields = new LinkedHashMap<>();
    final List<Method> postLoadMethods = new ArrayList<>();
    IdGenerator idGenerator;
    boolean isCollectible;

    public BasicReflectionCodec(Type type, TypeCodecRegistry typeCodecRegistry) {
        super(type, typeCodecRegistry);
        // resolve all persistable fields
        for (final FieldTypePair fieldTypePair : ReflectionHelper.getDeclaredAndInheritedFieldTypePairs(type, true)) {
            Field field = fieldTypePair.getField();
            if (!isIgnorable(field)) {
                MappedField mappedField = new MappedField(fieldTypePair, encoderClass, typeCodecRegistry);
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
                        throw new IllegalArgumentException("Id field is used again in class hierarchy! Class " + encoderClass);
                    }
                }
            }
        }


        // resolve lifecycle methods
        for (MethodTypePair methodTypePair : ReflectionHelper.getDeclaredAndInheritedMethods(encoderClass)) {
            Method method = methodTypePair.getMethod();
            if (method.isAnnotationPresent(PostLoad.class)) {
                postLoadMethods.add(method);
            }
        }
    }

    @Override
    public Map<String, MappedField> getPersistenceFields() {
        return persistenceFields;
    }

    public BasicReflectionCodec(Type type, TypeCodecRegistry typeCodecRegistry, Set<String> allDiscriminatorKeys) {
        this(type, typeCodecRegistry);
        for (MappedField persistenceField : persistenceFields.values()) {
            if (allDiscriminatorKeys.contains(persistenceField.getMappedFieldName())) {
                LOGGER.error("A field {} within class {} is named like one of the discriminator keys {}", persistenceField.getMappedFieldName(), encoderClass, allDiscriminatorKeys);
                throw new IllegalArgumentException("A field " + persistenceField.getMappedFieldName() + " within class " + encoderClass + " is named like one of the discriminator keys" + allDiscriminatorKeys);
            }
        }
    }

    protected boolean isIgnorable(final Field field) {
        return field.isAnnotationPresent(Transient.class)
                || Modifier.isTransient(field.getModifiers());
    }


    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        T newInstance;
        //if reader is in initial state (reader.getCurrentBsonType() == null) or DOCUMENT state
        if (reader.getCurrentBsonType() == null || reader.getCurrentBsonType() == BsonType.DOCUMENT) {
            reader.readStartDocument();
            newInstance = decodeFields(reader, decoderContext, newInstance());
            reader.readEndDocument();
            return newInstance;
        } else {
            LOGGER.error("Expected to read document but reader is in state {}. Skipping value!", reader.getCurrentBsonType());
            reader.skipValue();
            return null;
        }
    }

    @Override
    public T decodeFields(BsonReader reader, DecoderContext decoderContext, T instance) {
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            MappedField mappedField = persistenceFields.get(fieldName);
            if (mappedField != null) {
                mappedField.decode(reader, instance, decoderContext);
            } else {
                reader.skipValue();
            }
        }
        postDecode(instance);
        return instance;
    }

    @Override
    public void postDecode(T instance) {
        initializeDefaults(instance);
        for (Method postLoadMethod : postLoadMethods) {
            try {
                postLoadMethod.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.warn("@PostLoad method {} could not be called.", postLoadMethod, e);
            }
        }
    }

    @Override
    public void initializeDefaults(T instance) {
        for (MappedField persistenceField : persistenceFields.values()) {
            persistenceField.initializeDefault(instance);
        }
    }

    @Override
    public void encode(BsonWriter writer, T instance, EncoderContext encoderContext) {
        writer.writeStartDocument();
        encodeFields(writer, instance, encoderContext);
        writer.writeEndDocument();
    }

    @Override
    public void encodeFields(BsonWriter writer, T instance, EncoderContext encoderContext) {
        for (MappedField persistenceField : persistenceFields.values()) {
            persistenceField.encode(writer, instance, encoderContext);
        }
    }

    @Override
    public MappedField getMappedField(String mappedFieldName) {
        return persistenceFields.get(mappedFieldName);
    }

    @Override
    public MappedField getIdField() {
        return idField;
    }

    @Override
    public boolean isCollectible() {
        return isCollectible;
    }

    @Override
    public T generateIdIfAbsentFromDocument(T document) {
        if (idGenerator != null && !documentHasId(document)) {
            boolean couldGenerate = idField.setFieldValue(document, idGenerator.generate());
            if (!couldGenerate) {
                LOGGER.error("Could not set id!");
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