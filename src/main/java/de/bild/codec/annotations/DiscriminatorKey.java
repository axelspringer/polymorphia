package de.bild.codec.annotations;

import java.lang.annotation.*;

/**
 * A discriminator key is useful if you want to control the property name for which the discriminator is persisted.
 * Attention: Make sure you annotate the class hierarchy.
 * Annotating an interface (there could be more than just one) would be confusing and is therefore not supported.
 * <p>
 * Additional note: If you use more than just one discriminator key to persist pojo classes of the same polymorphic hierarchy
 * pay attention not to use properties that are named by one of those discriminator keys. This may lead to wrong decodings!
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiscriminatorKey {
    String value() default "_t";
}