package de.bild.backend.polymorphia.enums.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Pojo {
    EnumA simpleEnumProperty;
    List<Displayable> displayable;
}
