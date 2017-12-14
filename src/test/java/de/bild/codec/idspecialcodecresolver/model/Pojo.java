package de.bild.codec.idspecialcodecresolver.model;

import de.bild.codec.annotations.Id;
import de.bild.codec.idspecialcodecresolver.ExternalIdCodecProviderTest;
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
