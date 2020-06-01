
package uk.gov.justice.probation.courtcasematcher.model.generic.schemaheader;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Schema Header Type - Standard Schema Header Elements imported across schemas
 *
 * <p>Java class for SchemaHeaderType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SchemaHeaderType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DateOfHearing" minOccurs="0"&gt;
 *           &lt;simpleType&gt;
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *               &lt;maxLength value="10"/&gt;
 *             &lt;/restriction&gt;
 *           &lt;/simpleType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="TimeOfHearing" minOccurs="0"&gt;
 *           &lt;simpleType&gt;
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *               &lt;maxLength value="5"/&gt;
 *             &lt;/restriction&gt;
 *           &lt;/simpleType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="CourtHouseName" minOccurs="0"&gt;
 *           &lt;simpleType&gt;
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *               &lt;maxLength value="50"/&gt;
 *             &lt;/restriction&gt;
 *           &lt;/simpleType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SchemaHeaderType", propOrder = {
    "dateOfHearing",
    "timeOfHearing",
    "courtHouseName"
})
public class SchemaHeaderType {

    @XmlElement(name = "DateOfHearing")
    protected String dateOfHearing;
    @XmlElement(name = "TimeOfHearing")
    protected String timeOfHearing;
    @XmlElement(name = "CourtHouseName")
    protected String courtHouseName;

    /**
     * Gets the value of the dateOfHearing property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDateOfHearing() {
        return dateOfHearing;
    }

    /**
     * Sets the value of the dateOfHearing property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDateOfHearing(String value) {
        this.dateOfHearing = value;
    }

    /**
     * Gets the value of the timeOfHearing property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getTimeOfHearing() {
        return timeOfHearing;
    }

    /**
     * Sets the value of the timeOfHearing property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setTimeOfHearing(String value) {
        this.timeOfHearing = value;
    }

    /**
     * Gets the value of the courtHouseName property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCourtHouseName() {
        return courtHouseName;
    }

    /**
     * Sets the value of the courtHouseName property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCourtHouseName(String value) {
        this.courtHouseName = value;
    }

}
