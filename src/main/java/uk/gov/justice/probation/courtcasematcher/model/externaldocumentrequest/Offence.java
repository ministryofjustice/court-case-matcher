package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
public class Offence {

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "oseq")
    private final Integer seq;

    @JacksonXmlProperty(localName = "sum")
    private final String summary;
    private final String title;
    @JacksonXmlProperty(localName = "as")
    private final String act;
}
