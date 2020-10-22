package uk.gov.justice.probation.courtcasematcher.model.offendersearch;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.util.StringUtils;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Slf4j
public class MatchRequest {
    private static final String ERROR_NO_DATE_OF_BIRTH = "No dateOfBirth provided";
    private static final String ERROR_NO_NAME = "No surname provided";

    private String pncNumber;
    private String firstName;
    private String surname;
    private String dateOfBirth;

    public static MatchRequest from(String pnc, Name fullName, LocalDate dateOfBirth) throws IllegalArgumentException {
        if (dateOfBirth == null) {
            log.error(ERROR_NO_DATE_OF_BIRTH);
            throw new IllegalArgumentException(ERROR_NO_DATE_OF_BIRTH);
        }

        if (fullName == null || StringUtils.isEmpty(fullName.getSurname())) {
            log.error(ERROR_NO_NAME);
            throw new IllegalArgumentException(ERROR_NO_NAME);
        }

        MatchRequestBuilder builder = builder()
                                                    .pncNumber(pnc)
                                                    .surname(fullName.getSurname())
                                                    .dateOfBirth(dateOfBirth.format(DateTimeFormatter.ISO_DATE));
        String forenames = fullName.getForenames();
        if (!StringUtils.isEmpty(forenames)) {
            builder.firstName(forenames);
        }
        return builder.build();
    }
}
