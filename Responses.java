/*
 * Copyright 2019 xtecuan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iadb.kic.kicsystem.integration.surveysreports.jpa.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author xtecuan
 */
@Entity
@Table(name = "responses")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Responses.findAll", query = "SELECT r FROM Responses r"),
    @NamedQuery(name = "Responses.findByRid", query = "SELECT r FROM Responses r WHERE r.rid = :rid"),
    @NamedQuery(name = "Responses.findByResponseId", query = "SELECT r FROM Responses r WHERE r.responseId = :responseId"),
    @NamedQuery(name = "Responses.findBySmId", query = "SELECT r FROM Responses r WHERE r.smId = :smId"),
    @NamedQuery(name = "Responses.findByRcptId", query = "SELECT r FROM Responses r WHERE r.rcptId = :rcptId"),
    @NamedQuery(name = "Responses.findByCollectorId", query = "SELECT r FROM Responses r WHERE r.collectorId = :collectorId"),
    @NamedQuery(name = "Responses.findByDateModified", query = "SELECT r FROM Responses r WHERE r.dateModified = :dateModified"),
    @NamedQuery(name = "Responses.findByDateCreated", query = "SELECT r FROM Responses r WHERE r.dateCreated = :dateCreated"),
    @NamedQuery(name = "Responses.findByIpAddress", query = "SELECT r FROM Responses r WHERE r.ipAddress = :ipAddress"),
    @NamedQuery(name = "Responses.findByResponseStatus", query = "SELECT r FROM Responses r WHERE r.responseStatus = :responseStatus"),
    @NamedQuery(name = "Responses.findByTotalTime", query = "SELECT r FROM Responses r WHERE r.totalTime = :totalTime"),
    @NamedQuery(name = "Responses.findByResponseHref", query = "SELECT r FROM Responses r WHERE r.responseHref = :responseHref"),
    @NamedQuery(name = "Responses.findByAnalyzeUrl", query = "SELECT r FROM Responses r WHERE r.analyzeUrl = :analyzeUrl"),
    @NamedQuery(name = "Responses.findByEditUrl", query = "SELECT r FROM Responses r WHERE r.editUrl = :editUrl"),
    @NamedQuery(name = "Responses.findByUpdateDate", query = "SELECT r FROM Responses r WHERE r.updateDate = :updateDate")})
public class Responses implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "rid")
    private Long rid;
    @Basic(optional = false)
    @Column(name = "response_id")
    private String responseId;
    @Basic(optional = false)
    @Column(name = "sm_id")
    private String smId;
    @Basic(optional = false)
    @Column(name = "rcpt_id")
    private String rcptId;
    @Column(name = "collector_id")
    private String collectorId;
    @Column(name = "collector_status")
    private String collectorStatus;
    @Column(name = "date_modified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateModified;
    @Column(name = "date_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreated;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "response_status")
    private String responseStatus;
    @Column(name = "response_language")
    private String responseLanguage;
    @Column(name = "total_time")
    private Integer totalTime;
    @Column(name = "response_href")
    private String responseHref;
    @Column(name = "analyze_url")
    private String analyzeUrl;
    @Column(name = "edit_url")
    private String editUrl;
    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    public Responses() {
    }

    public Responses(Long rid) {
        this.rid = rid;
    }

    public Responses(Long rid, String responseId, String smId, String rcptId) {
        this.rid = rid;
        this.responseId = responseId;
        this.smId = smId;
        this.rcptId = rcptId;
    }

    public Long getRid() {
        return rid;
    }

    public void setRid(Long rid) {
        this.rid = rid;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getSmId() {
        return smId;
    }

    public void setSmId(String smId) {
        this.smId = smId;
    }

    public String getRcptId() {
        return rcptId;
    }

    public void setRcptId(String rcptId) {
        this.rcptId = rcptId;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public String getCollectorStatus() {
        return collectorStatus;
    }

    public void setCollectorStatus(String collectorStatus) {
        this.collectorStatus = collectorStatus;
    }

    public Date getDateModified() {
        return dateModified;
    }

    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseLanguage() {
        return responseLanguage;
    }

    public void setResponseLanguage(String responseLanguage) {
        this.responseLanguage = responseLanguage;
    }

    public Integer getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Integer totalTime) {
        this.totalTime = totalTime;
    }

    public String getResponseHref() {
        return responseHref;
    }

    public void setResponseHref(String responseHref) {
        this.responseHref = responseHref;
    }

    public String getAnalyzeUrl() {
        return analyzeUrl;
    }

    public void setAnalyzeUrl(String analyzeUrl) {
        this.analyzeUrl = analyzeUrl;
    }

    public String getEditUrl() {
        return editUrl;
    }

    public void setEditUrl(String editUrl) {
        this.editUrl = editUrl;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (rid != null ? rid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Responses)) {
            return false;
        }
        Responses other = (Responses) object;
        if ((this.rid == null && other.rid != null) || (this.rid != null && !this.rid.equals(other.rid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.iadb.kic.kicsystem.integration.surveysreports.jpa.entities.Responses[ rid=" + rid + " ]";
    }
    
}
