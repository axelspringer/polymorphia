package de.bild.backend.polymorphia.idexternal.model;

import de.bild.backend.polymorphia.IgnoreAnnotation;
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
