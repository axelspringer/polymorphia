package de.bild.backend.polymorphia.id;

import de.bild.codec.InstanceAwareIdGenerator;
import de.bild.codec.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PojoWithInstanceAwareIdGeneration {
    @Id(collectible = true, value = SomeIdGenerator.class)
    SomeId id;

    int useMeForIdGeneration;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class SomeId {
        int id;
        int version;
    }

    public static class SomeIdGenerator implements InstanceAwareIdGenerator<SomeId, PojoWithInstanceAwareIdGeneration> {
        @Override
        public SomeId generate(PojoWithInstanceAwareIdGeneration instance) {
            return new SomeId(instance.useMeForIdGeneration, instance.useMeForIdGeneration);
        }
    }
}
