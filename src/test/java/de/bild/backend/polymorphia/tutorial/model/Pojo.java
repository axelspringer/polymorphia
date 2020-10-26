package de.bild.backend.polymorphia.tutorial.model;

import de.bild.codec.annotations.Id;
import lombok.ToString;
import org.bson.types.ObjectId;

import java.util.List;

@ToString
public class Pojo implements PolymorphicPojo {
    @Id(collectible = true)
    ObjectId id;

    AnEnum anEnum;
    String name;
    List<PolymorphicPojo> pojos;


    public ObjectId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PolymorphicPojo> getPojos() {
        return pojos;
    }

    public void setPojos(List<PolymorphicPojo> pojos) {
        this.pojos = pojos;
    }

    public AnEnum getAnEnum() {
        return anEnum;
    }

    public void setAnEnum(AnEnum anEnum) {
        this.anEnum = anEnum;
    }
}