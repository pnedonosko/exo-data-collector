package org.exoplatform.datacollector.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * The RelevanceEntity class that represents user attitude to the Activity The
 * Activity can be marked as relevant or irrelevant
 */
@Entity(name = "DataCollectorRelevance")
@ExoEntity
@Table(name = "ST_ACTIVITY_RELEVANCY")
@IdClass(RelevanceId.class)
public class RelevanceEntity {

	/**
	 * The user id
	 */
	@Id
	@Column(name = "USER_ID")
	protected String userId;

	/**
	 * The activity id
	 */
	@Id
	@Column(name = "ACTIVITY_ID")
	protected String activityId;

	/**
	 * Indicates relevance of the Activity
	 */
	@Column(name = "IS_RELEVANT")
	protected Boolean relevant;
	
	@Column(name = "WEIGHT")
	protected Double weight;

	/**
	 * Gets the user id
	 * 
	 * @return user id
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Gets the weight
	 * 
	 * @return weight
	 */
	public Double getWeight() {
		return weight;
	}

	/**
	 * Sets the weight
	 * 
	 * @param weight
	 */
	public void setWeight(Double weight) {
		this.weight = weight;
	}

	/**
	 * Sets the user id
	 * 
	 * @param userId
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Gets the activity id
	 * 
	 * @return
	 */
	public String getActivityId() {
		return activityId;
	}

	/**
	 * Sets the activity id
	 * 
	 * @param activityId
	 */
	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	/**
	 * Gets the relevance
	 * 
	 * @return
	 */
	public Boolean getRelevant() {
		return relevant;
	}

	/**
	 * Sets the relevance
	 * 
	 * @param relevant
	 */
	public void setRelevant(Boolean relevant) {
		this.relevant = relevant;
	}

	/**
	 * Converts the RelevanceEntity to the String
	 */
	@Override
	public String toString() {
		return "RelevanceEntity [userId=" + userId + ", activityId=" + activityId + ", relevant=" + relevant
				+ ", weight=" + weight + "]";
	}

}
