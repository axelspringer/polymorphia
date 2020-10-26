package de.bild.backend.polymorphia.idtypemismatch.model;

import de.bild.backend.polymorphia.idtypemismatch.ExternalIdCodecProviderTest;
import de.bild.codec.annotations.Id;
import lombok.*;

@EqualsAndHashCode
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pojo {
    @Id(collectible = true, value = ExternalIdCodecProviderTest.WrongCustomIdGenerator.class)
    CustomId id;
    String someOtherProperty;
}
