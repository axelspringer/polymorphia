package de.bild.codec.annotations;

import org.bson.codecs.Codec;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface CodecToBeUsed {
    // a Class implementing Codec that needs to provide a constructor with the following signature
    // Constructor(TypeCodecRegistry typeCodecRegistry)
    Class<? extends Codec<?>> value();
}