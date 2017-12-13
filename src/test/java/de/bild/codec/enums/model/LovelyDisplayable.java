package de.bild.codec.enums.model;

import lombok.*;

@EqualsAndHashCode
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LovelyDisplayable implements Displayable {
    String identityProperty;
    @Override
    public String getLocalizationTag() {
        return "lovely";
    }

}
