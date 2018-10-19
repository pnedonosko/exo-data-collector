package org.exoplatform.datacollector.domain;

import java.io.Serializable;

public class RelevanceId implements Serializable {

	/** The serial version UID */
	private static final long serialVersionUID = 1L;

	/** The user id */
	protected String userId;

	/** The activity id */
	protected String activityId;

	/**
	 * Instantiates a new RelevanceId
	 */
	public RelevanceId() {

	}

	/**
	 * Instantiates a new RelevanceId
	 * 
	 * @param userId     the user id
	 * @param activityId the activity id
	 */
	public RelevanceId(String userId, String activityId) {
		super();
		this.userId = userId;
		this.activityId = activityId;
	}

	/**
	 * Gets the user id
	 * 
	 * @return user id
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Gets the activity id
	 * 
	 * @return activity id
	 */
	public String getActivityId() {
		return activityId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return (7 + userId.hashCode()) * 31 + activityId.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
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

}
