package de.bild.codec.objectmodel.model;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@EqualsAndHashCode
@Builder
public class Pojo {
    String aString;
    Float aFloat;
    AnotherEnum anotherEnum;
    OnlyOneImplementationInterface onlyOneImplementationInterface;
    NiceEnum niceEnum;
    SomeInterface someInterface;

    List<Object> objects;
    NiceEnum niceEnum2;
    SomeInterface someInterface2;
}