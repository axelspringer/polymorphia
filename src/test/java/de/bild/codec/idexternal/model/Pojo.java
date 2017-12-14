package de.bild.codec.idexternal.model;

import de.bild.codec.annotations.Id;
import de.bild.codec.idexternal.CustomId;
import de.bild.codec.idexternal.ExternalIdCodecProviderTest;
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
