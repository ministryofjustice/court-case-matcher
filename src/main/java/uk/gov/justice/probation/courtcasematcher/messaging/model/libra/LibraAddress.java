package uk.gov.justice.probation.courtcasematcher.messaging.model.libra;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Data
@Builder
public class LibraAddress {

    private final String line1;
    private final String line2;
    private final String line3;
    private final String line4;
    private final String line5;
    private final String pcode;

}
