package de.bild.backend.polymorphia.enums.model;

public enum EnumB implements Displayable {
    TYPE1,
    TYPE2;

    public String getLocalizationTag() {
        return "doh";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " : " + name();
    }

}
