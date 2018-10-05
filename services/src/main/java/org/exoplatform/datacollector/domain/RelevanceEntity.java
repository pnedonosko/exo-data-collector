package org.exoplatform.datacollector.domain;

public class RelevanceEntity {

	private String userId;
	private String activityId;
	private Boolean relevant;
	
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
