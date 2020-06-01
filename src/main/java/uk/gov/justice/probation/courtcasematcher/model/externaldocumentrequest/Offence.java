package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.EXT_DOC_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Offence {

    private String adjdate;
    private String adjreason;

    private String code;
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "oseq")
    private Integer seq;
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "pleadate")
    private String pleaDate;

    private Long co_id; // ": 1183410,
    private String convdate; // ": "28/09/2016",
    private String sum; // "sum": "Blah",
    private String title; // "title": "Caused to be p"
    private String plea; // "plea": "NG"
    private String maxpen;
    private String as;
}
