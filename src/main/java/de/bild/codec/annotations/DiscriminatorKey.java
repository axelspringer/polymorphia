package de.bild.codec.annotations;

import java.lang.annotation.*;

/**
 * A discriminator key is useful if you want to control the property name for which the discriminator is persisted.
 * Attention: Make sure you annotate a class in the hierarchy not an interface.
 * Annotating an interface (there could be more than just one for any given implementor) would be confusing and is therefore not supported.
 *
 * If you want to assign a single discriminator key to a polymorphic class structure starting from an interface you have two options.
 * 1) annotate all direct implementors of the interface with the same discriminator key (fault prone due to missing annotations)
 * 2) implement an abstract base class and annotate this with {@link DiscriminatorKey} (better option)
 * <p>
 * Additional note: If you use more than just one discriminator key to persist pojo classes of the same polymorphic hierarchy
 * pay attention not to use properties that are named by one of those discriminator keys.
 * This may lead to wrong decodings - especially if the value for the given key matches one of the discriminator values of the hierarchy!
 *
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiscriminatorKey {
    String value() default "_t";
}