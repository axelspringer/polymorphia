package de.bild.codec.nonregisteredmodel.nonmodel;

import de.bild.codec.nonregisteredmodel.model.SomePropertyEntity;

public class NonModelSomePropertyEntity<V extends Integer, S extends Long> extends SomePropertyEntity<V> {
    public  String nonPersistedProperty;
}