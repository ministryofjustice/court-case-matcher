package uk.gov.justice.probation.courtcasematcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableJms
public class CourtCaseMatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(CourtCaseMatcherApplication.class, args);
	}

}
