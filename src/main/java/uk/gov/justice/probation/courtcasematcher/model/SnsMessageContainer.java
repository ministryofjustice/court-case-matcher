package uk.gov.justice.probation.courtcasematcher.model;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnsMessageContainer implements Serializable {

    @JsonProperty(value = "Type")
    private final String type;

    @JsonProperty(value = "MessageId")
    private final String messageId;

    @JsonProperty(value = "TopicArn")
    private final String topicArn;

    @NotBlank
    @JsonProperty(value = "Message")
    private final String message;

    @JsonProperty(value = "Timestamp")
    private final String timestamp;

    @JsonProperty(value = "SignatureVersion")
    private final String signatureVersion;

    @JsonProperty(value = "Signature")
    private final String signature;

    @JsonProperty(value = "SigningCertURL")
    private final String signingCertURL;

    @JsonProperty(value = "UnsubscribeURL")
    private final String unsubscribeURL;

}