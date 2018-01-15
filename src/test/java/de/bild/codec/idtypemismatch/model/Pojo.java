package de.bild.codec.idtypemismatch.model;

import de.bild.codec.annotations.Id;
import de.bild.codec.idtypemismatch.ExternalIdCodecProviderTest;
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
