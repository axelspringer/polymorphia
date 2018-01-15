package de.bild.codec.annotations;

import java.lang.annotation.*;

/**
 * If you need nulls to be written out to the database, use this annotation either at pojo class level or at field level.
 * Please note: If you set EncodeNulls=false and decode entities with undefined values within the mongo database,
 * the {@link DecodeUndefinedHandlingStrategy} will influence the decoded value
 *
 * Global behaviour can be set during registration of {@link de.bild.codec.PojoCodecProvider.Builder#encodeNulls(boolean)}
 *
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface EncodeNulls {
    boolean value() default true;
}