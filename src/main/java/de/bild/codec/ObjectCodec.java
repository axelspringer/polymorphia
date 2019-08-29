package de.bild.codec;

import org.bson.*;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.UUID;

import static org.bson.assertions.Assertions.notNull;


/**
 * transcodes dynamically typed {@link Object} values.
 *
 * <p>
 * Works mostly the same as for {@link IterableCodec}.
 * </p>
 *
 * @param <T> of Object being transcoded.
 */
public class ObjectCodec<T> implements Codec<T> {
	private final Class<T> clazz;
	private final CodecRegistry registry;
	private final BsonTypeCodecMap bsonTypeCodecMap;
	private final Transformer valueTransformer;

	public ObjectCodec(final Class<T> clazz, final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap) {
		this(clazz, registry, bsonTypeClassMap, null);
	}

	public ObjectCodec(final Class<T> clazz, final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap, final Transformer valueTransformer) {
		this.clazz = clazz;
		this.registry = notNull("registry", registry);
		this.bsonTypeCodecMap = new BsonTypeCodecMap(notNull("bsonTypeClassMap", bsonTypeClassMap), registry);
		this.valueTransformer = valueTransformer != null ? valueTransformer : new Transformer() {
			@Override
			public Object transform(final Object objectToTransform) {
				return objectToTransform;
			}
		};
	}

	@Override
	public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
		if (value == null) {
			writer.writeNull();
			return;
		}

		final Codec encoder = registry.get(value.getClass());
		if (encoder == null) {
			throw new UnsupportedOperationException(value.getClass().toString());
		}
		encoderContext.encodeWithChildContext(encoder, writer, value);
	}

	@Override
	public Class<T> getEncoderClass() {
		return clazz;
	}

	@Override
	public T decode(final BsonReader reader, final DecoderContext decoderContext) {
		final BsonType bsonType = reader.getCurrentBsonType();
		if (bsonType == BsonType.NULL) {
			reader.readNull();
			return null;
		}

		final Decoder<?> decoder;
		if (bsonType == BsonType.BINARY && BsonBinarySubType.isUuid(reader.peekBinarySubType()) && reader.peekBinarySize() == 16) {
			decoder = registry.get(UUID.class);
		} else {
			decoder = bsonTypeCodecMap.get(bsonType);
		}

		if (decoder == null) {
			throw new UnsupportedOperationException(bsonType.toString());
		}
		return this.clazz.cast(valueTransformer.transform(decoderContext.decodeWithChildContext(decoder, reader)));
	}
}
