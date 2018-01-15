package de.bild.codec.idtypemismatch.model;

import de.bild.codec.annotations.IgnoreType;
import lombok.*;

@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@IgnoreType
public class CustomId {
    String aStringProperty;
}
