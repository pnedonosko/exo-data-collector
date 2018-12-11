package org.exoplatform.datacollector.domain.id;

import java.io.Serializable;

public class ActivityPostedId implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -7102912072077643489L;

  /**  The activity id. */
  protected String          postId;

  /**  The poster id. */
  protected String          posterId;

  /** The updated date (timestamp). */
  protected Long            updated;

  public ActivityPostedId() {
  }

  public ActivityPostedId(String postId, String posterId, Long updated) {
    this.postId = postId;
    this.posterId = posterId;
    this.updated = updated;
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

  /**
   * {@inheritDoc}
   */
  public int hashCode() {
    int hc = 7 + posterId.hashCode() * 31;
    hc = hc * 31 + posterId.hashCode();
    hc = hc * 31 + updated.hashCode();
    return hc;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (o != null) {
      if (ActivityPostedId.class.isAssignableFrom(o.getClass())) {
        ActivityPostedId other = ActivityPostedId.class.cast(o);
        return posterId.equals(other.getPosterId()) && postId.equals(other.getPostId()) && updated.equals(other.getUpdated());
      }
    }
    return false;
  }
}
