package de.bild.codec.nonregisteredmodel.model;

import de.bild.codec.annotations.Id;
import de.bild.codec.nonregisteredmodel.nonmodel.NonModelSomePropertyEntity;
import org.bson.types.ObjectId;

public class Pojo {
    @Id(collectible = true)
    public ObjectId id;

    public int x;
    public NonModelSomePropertyEntity<Integer, Long> property;
}
