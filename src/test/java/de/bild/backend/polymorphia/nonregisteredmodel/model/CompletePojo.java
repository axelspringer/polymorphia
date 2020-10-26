package de.bild.backend.polymorphia.nonregisteredmodel.model;

import de.bild.codec.annotations.Id;
import org.bson.types.ObjectId;

public class CompletePojo {
    @Id(collectible = true)
    public ObjectId id;
    public String aString;
    public int anInt;
}
