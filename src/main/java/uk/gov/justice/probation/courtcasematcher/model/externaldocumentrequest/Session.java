package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.EXT_DOC_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Session {

    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "s_id")
    private Long id;

    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "doh")
    private String dateOfHearing; //<doh>19/02/2020</doh>
    private String lja; // >South West London Magistrates; Court</lja>
    private String cmu; // >Gl Management Unit 1</cmu>
    private String panel; // >Adult Panel</panel>
    private String court; //>West London</court>
    private String room; // >00</room>

    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "sstart")
    private String start; //>09:00</sstart>
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "send")
    private String end; // <send>12:00</send>

    @JacksonXmlElementWrapper
    private List<Block> blocks;
}
