package de.bild.codec.stream.model;

import de.bild.codec.annotations.DecodingFieldFailureStrategy;
import de.bild.codec.annotations.DecodingPojoFailureStrategy;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@EqualsAndHashCode
@Builder
//@DecodingPojoFailureStrategy(DecodingPojoFailureStrategy.Strategy.NULL)
//@DecodingFieldFailureStrategy(DecodingFieldFailureStrategy.Strategy.SKIP)
public class Pojo {
    Integer number = 44;
    //@DecodingFieldFailureStrategy(DecodingFieldFailureStrategy.Strategy.SET_TO_NULL)
    List<Pojo> pojoList;
    List<Integer> integerList;
}