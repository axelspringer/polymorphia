package de.bild.codec;

import de.bild.codec.annotations.Discriminator;
import de.bild.codec.annotations.DiscriminatorFallback;
import de.bild.codec.annotations.DiscriminatorKey;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;


public class PolymorphicReflectionCodec<T> implements TypeCodec<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolymorphicReflectionCodec.class);
    final Class<T> clazz;
    final Map<String, ReflectionCodec> discriminatorToCodec = new HashMap<>();
    final Map<Class<?>, ReflectionCodec<T>> classToCodec = new HashMap<>();
    final Map<Class<?>, String> mainDiscriminators = new HashMap<>();
    final Map<Class<?>, String> discriminatorKeys = new HashMap<>();
    final Set<String> allDiscriminatorKeys = new HashSet<>();
    ReflectionCodec fallBackCodec;
    final boolean isCollectible;

    public PolymorphicReflectionCodec(Type type, Set<Type> validTypes, TypeCodecRegistry typeCodecRegistry, PojoContext pojoContext) {
        this.clazz = AbstractTypeCodec.extractClass(type);
        boolean isAnyCodecCollectible = false;

        for (Type validType : validTypes) {
            Class<T> clazz = AbstractTypeCodec.extractClass(validType);
            if (!clazz.isInterface()) {
                String discriminatorKey = getDiscriminatorKeyForClass(clazz);
                boolean isFallBack = clazz.getDeclaredAnnotation(DiscriminatorFallback.class) != null;

                discriminatorKeys.putIfAbsent(clazz, discriminatorKey);
                allDiscriminatorKeys.add(discriminatorKey);

                ReflectionCodec codecFor = pojoContext.resolve(validType, typeCodecRegistry);

                if (isFallBack) {
                    if (fallBackCodec != null) {
                        LOGGER.error("It is not allowed to declare more han one class within hierarchy as fallback. {} found already {}", clazz, codecFor.getEncoderClass());
                        throw new IllegalArgumentException("It is not allowed to declare more han one class within hierarchy as fallback." + clazz);
                    } else {
                        fallBackCodec = codecFor;
                        LOGGER.debug("Found fallback discriminator at class {}", clazz);
                    }
                }

                isAnyCodecCollectible |= codecFor.isCollectible();

                classToCodec.put(clazz, codecFor);

                Discriminator discriminatorAnnotation = clazz.getDeclaredAnnotation(Discriminator.class);
                String mainDiscriminator = clazz.getSimpleName();
                List<String> allDiscriminators = new ArrayList<>();
                if (discriminatorAnnotation != null) {
                    if (discriminatorAnnotation.value() != null) {
                        mainDiscriminator = discriminatorAnnotation.value();
                    }
                    allDiscriminators.add(mainDiscriminator);
                    for (String alias : discriminatorAnnotation.aliases()) {
                        allDiscriminators.add(alias);
                    }
                } else {
                    allDiscriminators.add(mainDiscriminator);
                }


                for (String discriminator : allDiscriminators) {
                    ReflectionCodec registeredCodec = this.discriminatorToCodec.putIfAbsent(discriminator, codecFor);
                    if (registeredCodec != null) {
                        LOGGER.warn("Cannot register multiple classes ({}, {}) for the same discriminator {} ", clazz, registeredCodec.getEncoderClass(), discriminator);
                        throw new IllegalArgumentException("Cannot register multiple classes (" + clazz + ", " + registeredCodec.getEncoderClass() + ") for the same discriminator " + discriminator);
                    }
                }
                mainDiscriminators.put(clazz, mainDiscriminator);
            }
        }

        //check for properties within classes that are named exacly like one of the used main discrimimnator keys
        for (ReflectionCodec<T> usedCodec : classToCodec.values()) {
            for (String allDiscriminatorKey : allDiscriminatorKeys) {
                MappedField mappedField = usedCodec.getMappedField(allDiscriminatorKey);
                if (mappedField != null) {
                    LOGGER.error("A field {} within {} is named like one of the discriminator keys {}", mappedField.getMappedFieldName(), usedCodec.getEncoderClass(), allDiscriminatorKeys);
                    throw new IllegalArgumentException("A field " + mappedField.getMappedFieldName() + " within " + usedCodec.getEncoderClass() + " is named like one of the discriminator keys " + allDiscriminatorKeys);

                }
            }
        }


        this.isCollectible = isAnyCodecCollectible;

        LOGGER.debug("Type {} -> Found the following matching types {}", type, discriminatorToCodec);
    }

    private String getDiscriminatorKeyForClass(Class<?> clazz) {
        DiscriminatorKey discriminatorKey = clazz.getAnnotation(DiscriminatorKey.class);
        if (discriminatorKey != null && discriminatorKey.value() != null && discriminatorKey.value().length() > 0) {
            return discriminatorKey.value();
        }
        return "_t";
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        String discriminator = null;
        reader.mark();
        reader.readStartDocument();
        ReflectionCodec<T> codec = null;
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            if (allDiscriminatorKeys.contains(fieldName)) {
                discriminator = reader.readString();
                codec = getCodecForDiscriminator(discriminator);
                if (codec != null) {
                    //now check that the codec found actually has the correct
                    String discriminatorKeyForClass = discriminatorKeys.get(codec.getEncoderClass());
                    if (fieldName.equals(discriminatorKeyForClass)) {
                        break;
                    } else {
                        discriminator = null;
                        codec = null;
                        LOGGER.warn("Confusing. Skipping discriminator {} encoded in discriminator key {} since the " +
                                        "destination class is declaring a different discriminator key {}.",
                                discriminator, fieldName, discriminatorKeyForClass);
                    }
                }
            } else {
                reader.skipValue();
            }
        }

        reader.reset();

        // try fallback and legacy handling
        if (codec == null) {
            if (discriminator != null) {
                LOGGER.warn("At least one valid discriminator {} was found in database, but no matching codec found at all.", discriminator);
                // todo not sure if it is safe to proceed here and assume something about the correct destination entitiy...better skip or die?
            }
            LOGGER.debug("No discriminator found in db for entity. Trying fallback. Fallback is {}", fallBackCodec);
            codec = fallBackCodec;
            if (codec == null) {
                LOGGER.debug("FallbackCodec is null. Still no matching codec found for discriminator {} within discriminatorToCodec {}", discriminator, discriminatorToCodec);
                if (classToCodec.values().size() == 1) {
                    codec = classToCodec.values().iterator().next();
                }
                else {
                    LOGGER.warn("Legacy handling to resolve entities in db without discriminator failed as there are (now?) more than one codecs available {}. One option is to use @DiscriminatrFallback at the legacy class or to add discriminators to the entities within the database. For now, skipping value.", classToCodec);
                    // TODO is skipping the right way to handle this? This might lead to lost data if a read object is rewritten to the database again...
                    reader.skipValue();
                    return null;
                }
            }
        }


        return decodeWithType(reader, decoderContext, codec);
    }


    protected T decodeWithType(BsonReader reader, DecoderContext decoderContext, ReflectionCodec<T> typeCodec) {
        return typeCodec.decode(reader, decoderContext);
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        encodeFields(writer, value, encoderContext);
        writer.writeEndDocument();
    }

    public void encodeFields(BsonWriter writer, T value, EncoderContext encoderContext) {
        ReflectionCodec<T> codecForValue = getCodecForClass(value.getClass());
        if (codecForValue != null) {
            writer.writeName(discriminatorKeys.get(codecForValue.getEncoderClass()));
            writer.writeString(mainDiscriminators.get(codecForValue.getEncoderClass()));
            encodeType(writer, value, encoderContext, codecForValue);
        } else {
            LOGGER.warn("The value to be encoded has the wrong type {}. This codec can only handle {}", value.getClass(), discriminatorToCodec);
        }
    }


    private ReflectionCodec getCodecForDiscriminator(String discriminator) {
        if (discriminator == null) {
            LOGGER.warn("Discriminator key cannot be null.");
            return null;
        }
        return discriminatorToCodec.get(discriminator);
    }

    /**
     * Walks up class hierarchy until a registered codec (in the context of registered model classes) is found
     *
     * @return a codec responsible for a valid class within the class hierarchy
     */
    public ReflectionCodec<T> getCodecForClass(Class<?> clazz) {
        if (clazz == null || Object.class.equals(clazz)) {
            return null;
        }
        ReflectionCodec<T> codec = classToCodec.get(clazz);
        if (codec != null) {
            return codec;
        }
        return getCodecForClass(clazz.getSuperclass());
    }

    private ReflectionCodec<T> getCodecForValue(T document) {
        return getCodecForClass(document.getClass());
    }


    protected void encodeType(BsonWriter writer, T value, EncoderContext encoderContext, ReflectionCodec<T> typeCodec) {
        typeCodec.encodeFields(writer, value, encoderContext);
    }

    @Override
    public Class<T> getEncoderClass() {
        return clazz;
    }

    @Override
    public boolean isCollectible() {
        return isCollectible;
    }

    @Override
    public T generateIdIfAbsentFromDocument(T document) {
        ReflectionCodec<T> codecForValue = getCodecForValue(document);
        if (codecForValue != null) {
            codecForValue.generateIdIfAbsentFromDocument(document);
        }
        return document;
    }

    @Override
    public boolean documentHasId(T document) {
        ReflectionCodec<T> codecForValue = getCodecForValue(document);
        if (codecForValue != null) {
            return codecForValue.documentHasId(document);
        }
        return false;
    }

    @Override
    public BsonValue getDocumentId(T document) {
        ReflectionCodec<T> codecForValue = getCodecForValue(document);
        if (codecForValue != null) {
            return codecForValue.getDocumentId(document);
        }
        return null;
    }
}