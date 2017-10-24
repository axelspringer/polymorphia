package de.bild.codec;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class MethodTypePair {
    final Method method;
    final Type realType;

    public MethodTypePair(Method method, Type realType) {
        this.method = method;
        this.realType = realType;
    }

    public Method getMethod() {
        return method;
    }

    public Type getRealType() {
        return realType;
    }
}
