package de.bild.backend.polymorphia.nonregisteredmodel.model;

import de.bild.backend.polymorphia.nonregisteredmodel.nonmodel.NonModelSomePropertyEntity;
import de.bild.codec.annotations.Id;
import org.bson.types.ObjectId;

public class Pojo {
    @Id(collectible = true)
    public ObjectId id;

    public int x;
    public NonModelSomePropertyEntity<Integer, Long> property;
}
