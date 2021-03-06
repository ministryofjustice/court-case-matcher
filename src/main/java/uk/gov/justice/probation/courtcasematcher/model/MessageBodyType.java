
package uk.gov.justice.probation.courtcasematcher.model;

import static uk.gov.justice.probation.courtcasematcher.messaging.MessageParser.GW_MSG_SCHEMA;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Getter
@Builder
public class MessageBodyType {

    @JacksonXmlProperty(namespace = GW_MSG_SCHEMA, localName = "GatewayOperationType")
    @NotNull
    @Valid
    private final GatewayOperationType gatewayOperationType;

}
