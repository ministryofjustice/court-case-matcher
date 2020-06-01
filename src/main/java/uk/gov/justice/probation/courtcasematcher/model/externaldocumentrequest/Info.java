
package uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest;

import static uk.gov.justice.probation.courtcasematcher.messaging.GatewayMessageParser.EXT_DOC_NS;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Info
{

    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "contentType")
    private String contentType;
    private String dateOfHearing;
    private String courtHouse;
    private String area;
    @JacksonXmlProperty(namespace = EXT_DOC_NS, localName = "source_file_name")
    private String sourceFileName;

}
