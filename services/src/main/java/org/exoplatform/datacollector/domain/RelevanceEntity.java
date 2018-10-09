package org.exoplatform.datacollector.domain;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "DataCollectorRelevance")
@ExoEntity
@Table(name = "DTC_RELEVANCES")
@IdClass(RelevanceId.class)
public class RelevanceEntity {

	@Id
	@Column(name = "USER_ID")
	protected String userId;
	
	@Id
	@Column(name = "ACTIVITY_ID")
	protected String activityId;
	
	@Column(name = "IS_RELEVANT")
	protected Boolean relevant;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getActivityId() {
		return activityId;
	}
	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}
	public Boolean getRelevant() {
		return relevant;
	}
	public void setRelevant(Boolean relevant) {
		this.relevant = relevant;
	}
	
	@Override
	public String toString() {
		return "RelevanceEntity [userId=" + userId + ", activityId=" + activityId + ", relevant=" + relevant + "]";
	}
	
}
