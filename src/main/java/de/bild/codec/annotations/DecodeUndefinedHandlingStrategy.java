package de.bild.codec.annotations;

import de.bild.codec.TypeCodec;

import java.lang.annotation.*;


/**
 * This strategy can be used to influence field values while decoding if no value is found in the database.
 * This comes in quite handy if your data model evolves and you find properties not to be encoded for old Pojos.
 *
 * A global default value can be set via {@link de.bild.codec.PojoCodecProvider.Builder#decodeUndefinedHandlingStrategy(Strategy)}
 * If not set, default is {@link DecodeUndefinedHandlingStrategy.Strategy#KEEP_POJO_DEFAULT} (due to historical behaviour of {@link de.bild.codec.PojoCodecProvider})
 *
 *
 * You can use this annotation at class level or at field level. If you use it at class level, you can override each field with
 * a field level annotation.
 *
 * <ul>
 *     <li>{@link Strategy#SET_TO_NULL} sets the value of the field to null, even if the pojo provides a default value</li>
 *     <li>{@link Strategy#CODEC} {@link TypeCodec#defaultInstance()} is being used to initialize the field, even if the pojo provides a default value</li>
 *     <li>{@link Strategy#KEEP_POJO_DEFAULT} keeps the pojo default, could be null or anything set during creation (constructor or default initialization of the field)</li>
 * </ul>
 *

 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface DecodeUndefinedHandlingStrategy {
    Strategy value();

    enum Strategy {
        SET_TO_NULL,
        CODEC,
        KEEP_POJO_DEFAULT;
    }

}