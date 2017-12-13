package de.bild.codec.enums.model;

public enum MyEnumType implements Displayable {
    TYPE1,
    TYPE2;

    public String getLocalizationTag() {
        return "localizedTag";
    }
}
