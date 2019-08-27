package de.bild.codec;

import static org.bson.assertions.Assertions.notNull;

import org.bson.Transformer;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.IterableCodecProvider;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;


/**
 * transcodes dynamically typed {@link Object} values.
 *
 * <p>
 * Works mostly the same as for {@link IterableCodecProvider}.
 * </p>
 *
 * @see ObjectCodec
 */
public class ObjectCodecProvider implements CodecProvider {
	private final BsonTypeClassMap bsonTypeClassMap;
	private final Transformer valueTransformer;

	public ObjectCodecProvider() {
		this(new BsonTypeClassMap());
	}

	public ObjectCodecProvider(final Transformer valueTransformer) {
		this(new BsonTypeClassMap(), valueTransformer);
	}

	public ObjectCodecProvider(final BsonTypeClassMap bsonTypeClassMap) {
		this(bsonTypeClassMap, null);
	}

	public ObjectCodecProvider(final BsonTypeClassMap bsonTypeClassMap, final Transformer valueTransformer) {
		this.bsonTypeClassMap = notNull("bsonTypeClassMap", bsonTypeClassMap);
		this.valueTransformer = valueTransformer;
	}

	@Override
	public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
		if (clazz == Object.class) {
			return new ObjectCodec<>(clazz, registry, bsonTypeClassMap, valueTransformer);
		}
		return null;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final ObjectCodecProvider that = (ObjectCodecProvider) o;

		if (!bsonTypeClassMap.equals(that.bsonTypeClassMap)) {
			return false;
		}
		if (valueTransformer != null ? !valueTransformer.equals(that.valueTransformer) : that.valueTransformer != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = bsonTypeClassMap.hashCode();
		result = 31 * result + (valueTransformer != null ? valueTransformer.hashCode() : 0);
		return result;
	}
}
