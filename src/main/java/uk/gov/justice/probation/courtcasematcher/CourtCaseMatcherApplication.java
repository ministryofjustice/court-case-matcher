package uk.gov.justice.probation.courtcasematcher;

import io.awspring.cloud.messaging.config.annotation.EnableSqs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableSqs
@SpringBootApplication
public class CourtCaseMatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(CourtCaseMatcherApplication.class, args);
	}

}
