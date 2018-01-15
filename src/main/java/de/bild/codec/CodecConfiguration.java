package de.bild.codec;

import de.bild.codec.annotations.DecodeUndefinedHandlingStrategy;
import de.bild.codec.annotations.EncodeNullHandlingStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Helper class holding configurations for the PojoCodecProvider
 */
@Builder
@AllArgsConstructor
@Getter
public class CodecConfiguration {
    private boolean encodeNulls;
    private EncodeNullHandlingStrategy.Strategy encodeNullHandlingStrategy;
    private DecodeUndefinedHandlingStrategy.Strategy decodeUndefinedHandlingStrategy;
}
