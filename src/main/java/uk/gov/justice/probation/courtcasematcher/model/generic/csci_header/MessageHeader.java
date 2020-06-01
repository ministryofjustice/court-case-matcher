
package uk.gov.justice.probation.courtcasematcher.model.generic.csci_header;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MessageHeader {

    @JacksonXmlProperty(namespace = "", localName = "MessageID")
    protected MessageID messageID;

    @JacksonXmlProperty(namespace = "http://www.justice.gov.uk/magistrates/generic/CSCI_Header", localName = "TimeStamp")
    protected String timeStamp;

    @JacksonXmlProperty(namespace = "http://www.justice.gov.uk/magistrates/generic/CSCI_Header", localName = "MessageType")
    protected String messageType;

    @JacksonXmlProperty(namespace = "http://www.justice.gov.uk/magistrates/generic/CSCI_Header", localName = "From")
    protected String from;
    @JacksonXmlProperty(namespace = "http://www.justice.gov.uk/magistrates/generic/CSCI_Header", localName = "To")
    protected String to;

    /**
     * Gets the value of the messageID property.
     *
     * @return
     *     possible object is
     *     {@link MessageID }
     *
     */
    public MessageID getMessageID() {
        return messageID;
    }

    /**
     * Sets the value of the messageID property.
     *
     * @param value
     *     allowed object is
     *     {@link MessageID }
     *
     */
    public void setMessageID(MessageID value) {
        this.messageID = value;
    }

    /**
     * Gets the value of the timeStamp property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets the value of the timeStamp property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setTimeStamp(String value) {
        this.timeStamp = value;
    }

    /**
     * Gets the value of the messageType property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Sets the value of the messageType property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setMessageType(String value) {
        this.messageType = value;
    }

    /**
     * Gets the value of the from property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the value of the from property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setFrom(String value) {
        this.from = value;
    }

    /**
     * Gets the value of the to property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the value of the to property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setTo(String value) {
        this.to = value;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class MessageID {

        @JacksonXmlProperty(localName = "UUID")
        protected String uuid;
        @JacksonXmlProperty(localName = "RelatesTo")
        protected String relatesTo;

        /**
         * Gets the value of the uuid property.
         *
         * @return
         *     possible object is
         *     {@link String }
         *
         */
        public String getUUID() {
            return uuid;
        }

        /**
         * Sets the value of the uuid property.
         *
         * @param value
         *     allowed object is
         *     {@link String }
         *
         */
        public void setUUID(String value) {
            this.uuid = value;
        }

        /**
         * Gets the value of the relatesTo property.
         *
         * @return
         *     possible object is
         *     {@link String }
         *
         */
        public String getRelatesTo() {
            return relatesTo;
        }

        /**
         * Sets the value of the relatesTo property.
         *
         * @param value
         *     allowed object is
         *     {@link String }
         *
         */
        public void setRelatesTo(String value) {
            this.relatesTo = value;
        }

    }

}
