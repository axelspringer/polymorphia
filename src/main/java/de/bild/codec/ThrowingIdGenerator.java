package de.bild.codec;

import de.bild.codec.IdGenerator;
import de.bild.codec.annotations.Id;


/**
 * use with the {@link Id} annotation when <code>_id</code> generation must be triggered by an explicit call.
 *
 * <p>
 * The {@link IdGenerator} does not have access to the object instance. As a result computation of the <code>_id</code>
 * value can not depend on business key data. To work around this, specify the {@link ThrowingIdGenerator} and provide
 * an <code>initId()</code> instance method clients need to call by themselves.
 * </p>
 */
public class ThrowingIdGenerator implements IdGenerator<String> {
	@Override
	public String generate() {
		throw new UnsupportedOperationException("id must be assigned explicitly using initId()!");
	}
}
