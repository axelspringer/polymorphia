package de.bild.backend.polymorphia.enums.model;

public enum EnumA implements Displayable {
    TYPE1,
    TYPE2,
    TYPE3;

    @Override
    public String getLocalizationTag() {
        return "ohh";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " : " + name();
    }
}
