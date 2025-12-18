
package org.iadb.kic.kicsystem.integration.surveysreports.dto.responses.bulk;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "total_time",
    "href",
    "custom_variables",
    "ip_address",
    "id",
    "logic_path",
    "date_modified",
    "response_status",
    "language",
    "custom_value",
    "analyze_url",
    "pages",
    "page_path",
    "recipient_id",
    "collector_id",
    "date_created",
    "survey_id",
    "collection_mode",
    "edit_url",
    "metadata"
})
public class Datum implements Serializable
{

    @JsonProperty("total_time")
    private Integer totalTime;
    @JsonProperty("href")
    private String href;
    @JsonProperty("custom_variables")
    private CustomVariables customVariables;
    @JsonProperty("ip_address")
    private String ipAddress;
    @JsonProperty("id")
    private String id;
    @JsonProperty("logic_path")
    private LogicPath logicPath;
    @JsonProperty("date_modified")
    private String dateModified;
    @JsonProperty("response_status")
    private String responseStatus;
    @JsonProperty("language")
    private String responseLanguage;
    @JsonProperty("custom_value")
    private String customValue;
    @JsonProperty("analyze_url")
    private String analyzeUrl;
    @JsonProperty("pages")
    private List<Page> pages = null;
    @JsonProperty("page_path")
    private List<Object> pagePath = null;
    @JsonProperty("recipient_id")
    private String recipientId;
    @JsonProperty("collector_id")
    private String collectorId;
    @JsonProperty("date_created")
    private String dateCreated;
    @JsonProperty("survey_id")
    private String surveyId;
    @JsonProperty("collection_mode")
    private String collectionMode;
    @JsonProperty("edit_url")
    private String editUrl;
    @JsonProperty("metadata")
    private Metadata metadata;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 715002013372408532L;

    @JsonProperty("total_time")
    public Integer getTotalTime() {
        return totalTime;
    }

    @JsonProperty("total_time")
    public void setTotalTime(Integer totalTime) {
        this.totalTime = totalTime;
    }

    public Datum withTotalTime(Integer totalTime) {
        this.totalTime = totalTime;
        return this;
    }

    @JsonProperty("href")
    public String getHref() {
        return href;
    }

    @JsonProperty("href")
    public void setHref(String href) {
        this.href = href;
    }

    public Datum withHref(String href) {
        this.href = href;
        return this;
    }

    @JsonProperty("custom_variables")
    public CustomVariables getCustomVariables() {
        return customVariables;
    }

    @JsonProperty("custom_variables")
    public void setCustomVariables(CustomVariables customVariables) {
        this.customVariables = customVariables;
    }

    public Datum withCustomVariables(CustomVariables customVariables) {
        this.customVariables = customVariables;
        return this;
    }

    @JsonProperty("ip_address")
    public String getIpAddress() {
        return ipAddress;
    }

    @JsonProperty("ip_address")
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Datum withIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public Datum withId(String id) {
        this.id = id;
        return this;
    }

    @JsonProperty("logic_path")
    public LogicPath getLogicPath() {
        return logicPath;
    }

    @JsonProperty("logic_path")
    public void setLogicPath(LogicPath logicPath) {
        this.logicPath = logicPath;
    }

    public Datum withLogicPath(LogicPath logicPath) {
        this.logicPath = logicPath;
        return this;
    }

    @JsonProperty("date_modified")
    public String getDateModified() {
        return dateModified;
    }

    @JsonProperty("date_modified")
    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    public Datum withDateModified(String dateModified) {
        this.dateModified = dateModified;
        return this;
    }

    @JsonProperty("response_status")
    public String getResponseStatus() {
        return responseStatus;
    }

    @JsonProperty("response_status")
    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Datum withResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
        return this;
    }

    @JsonProperty("language")
    public String getResponseLanguage() {
        return responseLanguage;
    }

    @JsonProperty("language")
    public void setResponseLanguage(String responseLanguage) {
        this.responseLanguage = responseLanguage;
    }

    public Datum withResponseLanguage(String responseLanguage) {
        this.responseLanguage = responseLanguage;
        return this;
    }

    @JsonProperty("custom_value")
    public String getCustomValue() {
        return customValue;
    }

    @JsonProperty("custom_value")
    public void setCustomValue(String customValue) {
        this.customValue = customValue;
    }

    public Datum withCustomValue(String customValue) {
        this.customValue = customValue;
        return this;
    }

    @JsonProperty("analyze_url")
    public String getAnalyzeUrl() {
        return analyzeUrl;
    }

    @JsonProperty("analyze_url")
    public void setAnalyzeUrl(String analyzeUrl) {
        this.analyzeUrl = analyzeUrl;
    }

    public Datum withAnalyzeUrl(String analyzeUrl) {
        this.analyzeUrl = analyzeUrl;
        return this;
    }

    @JsonProperty("pages")
    public List<Page> getPages() {
        return pages;
    }

    @JsonProperty("pages")
    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public Datum withPages(List<Page> pages) {
        this.pages = pages;
        return this;
    }

    @JsonProperty("page_path")
    public List<Object> getPagePath() {
        return pagePath;
    }

    @JsonProperty("page_path")
    public void setPagePath(List<Object> pagePath) {
        this.pagePath = pagePath;
    }

    public Datum withPagePath(List<Object> pagePath) {
        this.pagePath = pagePath;
        return this;
    }

    @JsonProperty("recipient_id")
    public String getRecipientId() {
        return recipientId;
    }

    @JsonProperty("recipient_id")
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public Datum withRecipientId(String recipientId) {
        this.recipientId = recipientId;
        return this;
    }

    @JsonProperty("collector_id")
    public String getCollectorId() {
        return collectorId;
    }

    @JsonProperty("collector_id")
    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public Datum withCollectorId(String collectorId) {
        this.collectorId = collectorId;
        return this;
    }

    @JsonProperty("date_created")
    public String getDateCreated() {
        return dateCreated;
    }

    @JsonProperty("date_created")
    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Datum withDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
        return this;
    }

    @JsonProperty("survey_id")
    public String getSurveyId() {
        return surveyId;
    }

    @JsonProperty("survey_id")
    public void setSurveyId(String surveyId) {
        this.surveyId = surveyId;
    }

    public Datum withSurveyId(String surveyId) {
        this.surveyId = surveyId;
        return this;
    }

    @JsonProperty("collection_mode")
    public String getCollectionMode() {
        return collectionMode;
    }

    @JsonProperty("collection_mode")
    public void setCollectionMode(String collectionMode) {
        this.collectionMode = collectionMode;
    }

    public Datum withCollectionMode(String collectionMode) {
        this.collectionMode = collectionMode;
        return this;
    }

    @JsonProperty("edit_url")
    public String getEditUrl() {
        return editUrl;
    }

    @JsonProperty("edit_url")
    public void setEditUrl(String editUrl) {
        this.editUrl = editUrl;
    }

    public Datum withEditUrl(String editUrl) {
        this.editUrl = editUrl;
        return this;
    }

    @JsonProperty("metadata")
    public Metadata getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Datum withMetadata(Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Datum withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("totalTime", totalTime).append("href", href).append("customVariables", customVariables).append("ipAddress", ipAddress).append("id", id).append("logicPath", logicPath).append("dateModified", dateModified).append("responseStatus", responseStatus).append("customValue", customValue).append("analyzeUrl", analyzeUrl).append("pages", pages).append("pagePath", pagePath).append("recipientId", recipientId).append("collectorId", collectorId).append("dateCreated", dateCreated).append("surveyId", surveyId).append("collectionMode", collectionMode).append("editUrl", editUrl).append("metadata", metadata).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(dateModified).append(logicPath).append(pages).append(editUrl).append(pagePath).append(id).append(customValue).append(recipientId).append(additionalProperties).append(responseStatus).append(customVariables).append(analyzeUrl).append(dateCreated).append(collectorId).append(surveyId).append(collectionMode).append(href).append(totalTime).append(metadata).append(ipAddress).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Datum) == false) {
            return false;
        }
        Datum rhs = ((Datum) other);
        return new EqualsBuilder().append(dateModified, rhs.dateModified).append(logicPath, rhs.logicPath).append(pages, rhs.pages).append(editUrl, rhs.editUrl).append(pagePath, rhs.pagePath).append(id, rhs.id).append(customValue, rhs.customValue).append(recipientId, rhs.recipientId).append(additionalProperties, rhs.additionalProperties).append(responseStatus, rhs.responseStatus).append(customVariables, rhs.customVariables).append(analyzeUrl, rhs.analyzeUrl).append(dateCreated, rhs.dateCreated).append(collectorId, rhs.collectorId).append(surveyId, rhs.surveyId).append(collectionMode, rhs.collectionMode).append(href, rhs.href).append(totalTime, rhs.totalTime).append(metadata, rhs.metadata).append(ipAddress, rhs.ipAddress).isEquals();
    }

}
