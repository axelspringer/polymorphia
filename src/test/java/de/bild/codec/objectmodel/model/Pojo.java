package de.bild.codec.objectmodel.model;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@EqualsAndHashCode
public class Pojo {
    String aString;
    Float aFloat;
    List<Object> objects;
}