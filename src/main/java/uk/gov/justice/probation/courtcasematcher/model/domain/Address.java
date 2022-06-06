package uk.gov.justice.probation.courtcasematcher.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Address implements Comparable<Address> {
    private final String line1;
    private final String line2;
    private final String line3;
    private final String line4;
    private final String line5;
    private final String postcode;

    @Override
    public int compareTo(Address addressToCompare) {
        if (this.getLine1() == null && addressToCompare.getLine1() == null) {
            // pass
        } else if (this.getLine1() == null) {
            return -1;
        } else if (addressToCompare.getLine1() == null) {
            return 1;
        } else {
            int line1Comparison = this.getLine1().compareTo(addressToCompare.getLine1());
            if (line1Comparison != 0) {
                return line1Comparison < 0 ? -1 : 1;
            }
        }

        if (this.getLine2() == null && addressToCompare.getLine2() == null) {
            // pass
        } else if (this.getLine2() == null) {
            return -1;
        } else if (addressToCompare.getLine2() == null) {
            return 1;
        } else {
            int line2Comparison = this.getLine2().compareTo(addressToCompare.getLine2());
            if (line2Comparison != 0) {
                return line2Comparison < 0 ? -1 : 1;
            }
        }

        if (this.getLine3() == null && addressToCompare.getLine3() == null) {
            // pass
        } else if (this.getLine3() == null) {
            return -1;
        } else if (addressToCompare.getLine3() == null) {
            return 1;
        } else {
            int line3Comparison = this.getLine3().compareTo(addressToCompare.getLine3());
            if (line3Comparison != 0) {
                return line3Comparison < 0 ? -1 : 1;
            }
        }

        if (this.getLine4() == null && addressToCompare.getLine4() == null) {
            // pass
        } else if (this.getLine4() == null) {
            return -1;
        } else if (addressToCompare.getLine4() == null) {
            return 1;
        } else {
            int line4Comparison = this.getLine4().compareTo(addressToCompare.getLine4());
            if (line4Comparison != 0) {
                return line4Comparison < 0 ? -1 : 1;
            }
        }

        if (this.getLine5() == null && addressToCompare.getLine5() == null) {
            // pass
        } else if (this.getLine5() == null) {
            return -1;
        } else if (addressToCompare.getLine5() == null) {
            return 1;
        } else {
            int line5Comparison = this.getLine5().compareTo(addressToCompare.getLine5());
            if (line5Comparison != 0) {
                return line5Comparison < 0 ? -1 : 1;
            }
        }

        if (this.getPostcode() == null && addressToCompare.getPostcode() == null) {
            return 0;
        } else if (this.getPostcode() == null) {
            return -1;
        } else if (addressToCompare.getPostcode() == null) {
            return 1;
        } else {
            return this.getPostcode().compareTo(addressToCompare.getPostcode());
        }
    }
}
