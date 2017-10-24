package de.bild.codec.tutorial.model;

public class SubPojo extends Pojo {
    int intProperty;

    private SubPojo() {
    }

    public SubPojo(int intProperty) {
        this.intProperty = intProperty;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SubPojo{");
        sb.append("intProperty=").append(intProperty);
        sb.append(", id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", pojos=").append(pojos);
        sb.append('}');
        return sb.toString();
    }
}
