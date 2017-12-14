package de.bild.codec.idexternal.model;

import de.bild.codec.IgnoreAnnotation;
import lombok.*;

@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@IgnoreAnnotation
public class CustomId {
    String aStringProperty;
}
