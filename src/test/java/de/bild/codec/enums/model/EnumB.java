package de.bild.codec.enums.model;

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
