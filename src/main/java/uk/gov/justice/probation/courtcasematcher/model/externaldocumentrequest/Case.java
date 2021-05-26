package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
@Valid
public class Case {

    @JacksonXmlProperty(localName = "c_id")
    private final Long caseId;

    @NotBlank
    @JacksonXmlProperty(localName = "caseno")
    private final String caseNo;
    @PositiveOrZero
    @JacksonXmlProperty(localName = "cseq")
    private final Integer seq;

    @JacksonXmlProperty(localName = "def_name_elements")
    private final Name name;

    @JacksonXmlProperty(localName = "def_name")
    private final String defendantName;
    @JacksonXmlProperty(localName = "def_type")
    private final String defendantType;
    @JacksonXmlProperty(localName = "def_sex")
    private final String defendantSex;

    @JacksonXmlProperty(localName = "def_dob")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", lenient = OptBoolean.TRUE)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private final LocalDate defendantDob;
    @JacksonXmlProperty(localName = "def_addr")
    private final Address defendantAddress;
    @JacksonXmlProperty(localName = "def_age")
    private final String defendantAge;

    @JacksonXmlProperty(localName = "cro_number")
    private final String cro;

    @JacksonXmlProperty(localName = "pnc_id")
    private final String pnc;

    @JacksonXmlProperty(localName = "listno")
    private final String listNo;

    @JacksonXmlProperty(localName = "nationality_1")
    private final String nationality1;

    @JacksonXmlProperty(localName = "nationality_2")
    private final String nationality2;

    @ToString.Exclude
    @JacksonXmlElementWrapper
    private final List<@Valid Offence> offences;

    @JsonBackReference
    private final Block block;

    private final String courtCode;
    private final String courtRoom;
    private final LocalDateTime sessionStartTime;

    public String getCourtCode() {
        return courtCode != null ? courtCode : block.getSession().getCourtCode();
    }

    public String getCourtRoom() {
        return courtRoom != null ? courtRoom : block.getSession().getCourtRoom();
    }

    public LocalDateTime getSessionStartTime() {
        return sessionStartTime != null ? sessionStartTime : block.getSession().getSessionStartTime();
    }
}
