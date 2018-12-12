package org.exoplatform.datacollector.domain.id;

import java.io.Serializable;
import java.util.Date;

public class ActivityLikedId implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 8603745083347311938L;

  /**  The activity id. */
  protected String          postId;

  /**  The poster id. */
  protected String          posterId;

  /** The updated date (timestamp). */
  protected Long            updated;

  /** The liker id */
  protected String          likerId;

  /** The liked date */
  protected Date            likedDate;

  /**
   * Initializes a new ActivityLikedId
   */
  public ActivityLikedId() {

  }

  /**
   * Initializes a new ActivityLikedId
   * @param postId
   * @param posterId
   * @param updated
   * @param likerId
   */
  public ActivityLikedId(String postId, String posterId, Long updated, String likerId, Date likedDate) {
    super();
    this.postId = postId;
    this.posterId = posterId;
    this.updated = updated;
    this.likerId = likerId;
    this.likedDate = likedDate;
  }

  public String getPostId() {
    return postId;
  }

  public void setPostId(String postId) {
    this.postId = postId;
  }

  public String getPosterId() {
    return posterId;
  }

  public void setPosterId(String posterId) {
    this.posterId = posterId;
  }

  public Long getUpdated() {
    return updated;
  }

  public void setUpdated(Long updated) {
    this.updated = updated;
  }

  public String getLikerId() {
    return likerId;
  }

  public void setLikerId(String likerId) {
    this.likerId = likerId;
  }
  

  public Date getLikedDate() {
    return likedDate;
  }

  public void setLikedDate(Date likedDate) {
    this.likedDate = likedDate;
  }

  /**
   * {@inheritDoc}
   */
  public int hashCode() {
    int hc = 7 + posterId.hashCode() * 31;
    hc = hc * 31 + posterId.hashCode();
    hc = hc * 31 + updated.hashCode();
    hc = hc * 31 + likerId.hashCode();
    hc = hc * 31 + likedDate.hashCode();
    return hc;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (o != null) {
      if (ActivityLikedId.class.isAssignableFrom(o.getClass())) {
        ActivityLikedId other = ActivityLikedId.class.cast(o);
        return posterId.equals(other.getPosterId()) && postId.equals(other.getPostId()) && updated.equals(other.getUpdated())
            && likerId.equals(other.getLikerId()) && likedDate.equals(other.getLikedDate());
      }
    }
    return false;
  }

}
