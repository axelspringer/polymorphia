package de.bild.codec.annotations;

import java.lang.annotation.*;

/**
 * Use this annotation at classes that you want to declare as polymorphic
 * This is especially useful if at present only one single base class is yet defined for your desired hierarchy
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Polymorphic {
}
