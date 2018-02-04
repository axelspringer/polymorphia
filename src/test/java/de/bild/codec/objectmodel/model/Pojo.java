package de.bild.codec.objectmodel.model;

import de.bild.codec.objectmodel.AnyThingTest;
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
    AnyThingTest.NonModelThingProvidingPolymorphicCodec nonModelThingProvidingPolymorphicCodec;
    AnyThingTest.NonModelThingProvidingStandardCodec nonModelThingProvidingStandardCodec;
    AnotherEnum anotherEnum;
    OnlyOneImplementationInterface onlyOneImplementationInterface;
    NiceEnum niceEnum;
    SomeInterface someInterface;

    List<Object> objects;
    NiceEnum niceEnum2;
    SomeInterface someInterface2;
    AnyThingTest.NonModelThingProvidingPolymorphicCodec yetAnotherNonModelThingProvidingPolymorphicCodec;
    AnyThingTest.NonModelThingProvidingStandardCodec yetAnothernNonModelThingProvidingStandardCodec;
}