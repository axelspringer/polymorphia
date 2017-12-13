package de.bild.codec.enums.model;

public enum ASecondEnum implements Displayable {
    SECOND_ENUM1,
    SECOND_ENUM2;

    @Override
    public String getLocalizationTag() {
        return "doh";
    }
}
