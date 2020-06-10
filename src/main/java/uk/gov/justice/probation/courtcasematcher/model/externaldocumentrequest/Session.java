package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Session {

    @JacksonXmlProperty(localName = "s_id")
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", lenient = OptBoolean.TRUE)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JacksonXmlProperty(localName = "doh")
    private LocalDate dateOfHearing;
    private String lja;
    private String cmu;
    private String panel;
    private String court;
    private String room;

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JacksonXmlProperty(localName = "sstart")
    private LocalTime start;

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JacksonXmlProperty(localName = "send")
    private LocalTime end;

    @JacksonXmlElementWrapper
    private List<Block> blocks;
}
