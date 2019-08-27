package de.bild.codec;

/**
 * IdGenerator that has no need to generate ids based on instance internals
 */
public interface IdGenerator<T> extends InstanceAwareIdGenerator<T, Object> {

    @Override
    default T generate(Object instance) {
        return generate();
    }

    /**
     * Generate id without knowledge of the instance
     * @return a new id
     */
    T generate();
}