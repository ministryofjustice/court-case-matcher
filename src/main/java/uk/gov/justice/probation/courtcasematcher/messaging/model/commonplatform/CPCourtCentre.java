package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CPCourtCentre {
    @NotBlank
    private final String id;
    @NotBlank
    private final String roomId;
    @NotBlank
    private final String roomName;
    @NotBlank
    @Size(min = 5)
    private final String code;

    public String getNormalisedCode() {
        return code.substring(0,5);
    }
}
