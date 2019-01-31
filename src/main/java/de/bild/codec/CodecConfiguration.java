package de.bild.codec;

import de.bild.codec.annotations.DecodeUndefinedHandlingStrategy;
import de.bild.codec.annotations.DecodingFieldFailureStrategy;
import de.bild.codec.annotations.DecodingPojoFailureStrategy;
import de.bild.codec.annotations.EncodeNullHandlingStrategy;

/**
 * Helper class holding configurations for the PojoCodecProvider
 */

public class CodecConfiguration {
    private boolean encodeNulls;
    private EncodeNullHandlingStrategy.Strategy encodeNullHandlingStrategy;
    private DecodeUndefinedHandlingStrategy.Strategy decodeUndefinedHandlingStrategy;
    private DecodingFieldFailureStrategy.Strategy decodingFieldFailureStrategy;
    private DecodingPojoFailureStrategy.Strategy decodingPojoFailureStrategy;

    public CodecConfiguration(boolean encodeNulls,
                              EncodeNullHandlingStrategy.Strategy encodeNullHandlingStrategy,
                              DecodeUndefinedHandlingStrategy.Strategy decodeUndefinedHandlingStrategy,
                              DecodingFieldFailureStrategy.Strategy decodingFieldFailureStrategy,
                              DecodingPojoFailureStrategy.Strategy decodingPojoFailureStrategy) {
        this.encodeNulls = encodeNulls;
        this.encodeNullHandlingStrategy = encodeNullHandlingStrategy;
        this.decodeUndefinedHandlingStrategy = decodeUndefinedHandlingStrategy;
        this.decodingFieldFailureStrategy = decodingFieldFailureStrategy;
        this.decodingPojoFailureStrategy = decodingPojoFailureStrategy;
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

    public DecodingFieldFailureStrategy.Strategy getDecodingFieldFailureStrategy() {
        return decodingFieldFailureStrategy;
    }

    public DecodingPojoFailureStrategy.Strategy getDecodingPojoFailureStrategy() {
        return decodingPojoFailureStrategy;
    }
}
