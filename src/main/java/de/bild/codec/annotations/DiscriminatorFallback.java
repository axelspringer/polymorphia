package de.bild.codec.annotations;


import java.lang.annotation.*;

/**
 * This annotation can be used to decode objects from the database that do not provide a discriminator. Usually this is
 * the case when you persist entities in the db that evolve into polymorphic structures but weren't at the beginning.
 * If you already know your structure will eventually grow into polymorphic structures, use the {@link Polymorphic}
 * annotation to make your intent clear. Then always a discriminator is persisted to the db.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiscriminatorFallback {
}
