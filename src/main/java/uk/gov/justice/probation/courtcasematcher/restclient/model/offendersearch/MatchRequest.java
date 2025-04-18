package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class MatchRequest {

    private String pncNumber;
    private String firstName;
    private String surname;
    private String dateOfBirth;

    @Slf4j
    @Component
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Factory {
        private static final String ERROR_NO_NAME = "No surname provided";

        @Setter
        @Value("${offender-search.use-dob-with-pnc:false}")
        private boolean useDobWithPnc;

        public MatchRequest buildFrom(String pnc, Name fullName, LocalDate dateOfBirth) throws IllegalArgumentException {
            if (fullName == null || isBlank(fullName.getSurname())) {
                log.error(ERROR_NO_NAME);
                throw new IllegalArgumentException(ERROR_NO_NAME);
            }

            MatchRequestBuilder builder = builder()
                                                .pncNumber(pnc)
                                                .surname(fullName.getSurname());

            if (!Objects.isNull(dateOfBirth) && !dateOfBirth.isBefore(LocalDate.now())){
                log.warn("Defendant date of birth is not in the past, setting to null");
                dateOfBirth = null;
            }

            if (!Objects.isNull(dateOfBirth)) {
                if (useDobWithPnc || isBlank(pnc)) {
                    builder.dateOfBirth(dateOfBirth.format(DateTimeFormatter.ISO_DATE));
                }
            }

            String forenames = fullName.getForenames();
            if (!isBlank(forenames)) {
                builder.firstName(forenames);
            }
            return builder.build();
        }

        public MatchRequest buildFrom(Defendant defendant) throws IllegalArgumentException {
            return buildFrom(defendant.getPnc(), defendant.getName(), defendant.getDateOfBirth());
        }
    }

    public static boolean isBlank(String string) {
        return Optional.ofNullable(string).map(String::isBlank).orElse(true);
    }
}
