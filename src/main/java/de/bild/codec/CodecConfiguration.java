package de.bild.codec;

import de.bild.codec.annotations.DecodeUndefinedHandlingStrategy;
import de.bild.codec.annotations.EncodeNullHandlingStrategy;

/**
 * Helper class holding configurations for the PojoCodecProvider
 */

public class CodecConfiguration {
    private boolean encodeNulls;
    private EncodeNullHandlingStrategy.Strategy encodeNullHandlingStrategy;
    private DecodeUndefinedHandlingStrategy.Strategy decodeUndefinedHandlingStrategy;

    @java.beans.ConstructorProperties({"encodeNulls", "encodeNullHandlingStrategy", "decodeUndefinedHandlingStrategy"})
    public CodecConfiguration(boolean encodeNulls, EncodeNullHandlingStrategy.Strategy encodeNullHandlingStrategy, DecodeUndefinedHandlingStrategy.Strategy decodeUndefinedHandlingStrategy) {
        this.encodeNulls = encodeNulls;
        this.encodeNullHandlingStrategy = encodeNullHandlingStrategy;
        this.decodeUndefinedHandlingStrategy = decodeUndefinedHandlingStrategy;
    }

    public boolean isEncodeNulls() {
        return this.encodeNulls;
    }

    public EncodeNullHandlingStrategy.Strategy getEncodeNullHandlingStrategy() {
        return this.encodeNullHandlingStrategy;
    }

    public DecodeUndefinedHandlingStrategy.Strategy getDecodeUndefinedHandlingStrategy() {
        return this.decodeUndefinedHandlingStrategy;
    }
}
