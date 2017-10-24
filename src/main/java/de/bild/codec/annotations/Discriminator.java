package de.bild.codec.annotations;

import java.lang.annotation.*;


/**
 * If you want to choose your discriminator manually you can annotate your pojo classes with this annotation.
 * Within the value array, provide ALL discriminators that identify your pojo
 * (if you have used different ones within mongo and do not want to change existing ones)
 * The first entry in this list will be used for current encodings to the database.
 * <p>
 * If no discriminator annotation is present at your pojo class, the {@link Class#getSimpleName()} will
 * be used in case a polymorphic data structure is found.
 * <p>
 * Please be aware that using the same discriminator could potentially lead to ambiguities, IF such classes
 * are assignable to each other.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Discriminator {
    String value();

    String[] aliases() default {};
}
