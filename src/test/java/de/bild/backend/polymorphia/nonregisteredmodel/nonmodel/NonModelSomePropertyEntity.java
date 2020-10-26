package de.bild.backend.polymorphia.nonregisteredmodel.nonmodel;

import de.bild.backend.polymorphia.nonregisteredmodel.model.SomePropertyEntity;

public class NonModelSomePropertyEntity<V extends Integer, S extends Long> extends SomePropertyEntity<V> {
    public  String nonPersistedProperty;
}