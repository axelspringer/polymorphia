package de.bild.codec.id;

import de.bild.codec.annotations.Id;

public class EntityWithMissingGenerator {
    @Id(collectible = true)
    String id;
}
