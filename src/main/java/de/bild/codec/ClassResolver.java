package de.bild.codec;

import java.util.List;

public interface ClassResolver {
    List<Class<?>> getClassesForPackage(String packageName);
}