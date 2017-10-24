package de.bild.codec.tutorial.model;

import de.bild.codec.annotations.Id;
import org.bson.types.ObjectId;

import java.util.List;

public class Pojo implements PolymorphicPojo {
    @Id(collectible = true)
    ObjectId id;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Pojo{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", pojos=").append(pojos);
        sb.append('}');
        return sb.toString();
    }
}