package org.exoplatform.datacollector.domain;

import java.io.Serializable;

public class RelevanceId implements Serializable {

	private static final long serialVersionUID = 1L;
	
	protected String userId;
	protected String activityId;

	public RelevanceId() {

	}

	public RelevanceId(String userId, String activityId) {
		super();
		this.userId = userId;
		this.activityId = activityId;
	}

	public String getUserId() {
		return userId;
	}

	@Override
	public int hashCode() {
		return (7 + userId.hashCode()) * 31 + activityId.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o != null) {
			if (RelevanceId.class.isAssignableFrom(o.getClass())) {
				RelevanceId other = RelevanceId.class.cast(o);
				return userId.equals(other.getUserId()) && activityId.equals(other.getActivityId());
			}
		}
		return false;
	}

	public String getActivityId() {
		return activityId;
	}

}
