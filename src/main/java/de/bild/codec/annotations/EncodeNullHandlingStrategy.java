package de.bild.codec.annotations;

import de.bild.codec.TypeCodec;

import java.lang.annotation.*;

/**
 * Use this annotation to specify the handling of null values prior to encoding to the database.
 * This annotation is thought for convenience.
 * You can use it at class level or at field level. If you use it at class level, you can override each field with
 * a field level annotation.
 *
 * A global default value can be set via {@link de.bild.codec.PojoCodecProvider.Builder#encodeNullHandlingStrategy(Strategy)}
 * If not set, default is {@link Strategy#CODEC} (due to historical behaviour of {@link de.bild.codec.PojoCodecProvider})
 *
 * You can e.g. make sure, that lists fields that are null are always encoded as empty lists, if desired.
 *
 * Right now, two strategies exist:
 * <ul>
 *     <li>{@link Strategy#CODEC} : If null is found then {@link TypeCodec#defaultInstance()} is being used to generate a default value e.g. empty list/set/map</li>
 *     <li>{@link Strategy#KEEP_NULL} : keep null</li>
 * </ul>
 *
 *
 *
 * For future improvements more strategies could be added, e.g. one strategy could be to register
 * a default-value-generator at the field.
 *
 *
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface EncodeNullHandlingStrategy {
    Strategy value();

    enum Strategy {
        CODEC,
        KEEP_NULL
    }
}