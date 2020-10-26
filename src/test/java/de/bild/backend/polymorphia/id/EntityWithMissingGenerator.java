package de.bild.backend.polymorphia.id;

import de.bild.codec.annotations.Id;

public class EntityWithMissingGenerator {
    @Id(collectible = true)
    String id;
}
