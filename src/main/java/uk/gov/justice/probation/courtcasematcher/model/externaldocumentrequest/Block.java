package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Block {

    @JacksonXmlProperty(localName = "sb_id")
    private Long id;
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JacksonXmlProperty(localName = "bstart")
    private LocalTime start;
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JacksonXmlProperty(localName = "bend")
    private LocalTime end;
    private String desc;

    @JacksonXmlElementWrapper
    private List<Case> cases;
}
