package de.bild.backend.polymorphia.idexternal.model;

import de.bild.backend.polymorphia.idexternal.ExternalIdCodecProviderTest;
import de.bild.codec.annotations.Id;
import lombok.*;

@EqualsAndHashCode
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pojo {
    @Id(collectible = true, value = ExternalIdCodecProviderTest.CustomIdGenerator.class)
    CustomId id;
    String someOtherProperty;
}
