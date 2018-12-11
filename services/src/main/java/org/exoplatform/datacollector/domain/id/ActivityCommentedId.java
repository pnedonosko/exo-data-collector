package org.exoplatform.datacollector.domain.id;

import java.io.Serializable;

public class ActivityCommentedId implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 6220994676574477372L;

  /**  The activity id. */
  protected String          postId;

  /**  The poster id. */
  protected String          posterId;

  /** The updated date (timestamp). */
  protected Long            updated;

  /** The comment updated (timestamp) */
  protected Long          commentUpdated;

  /**
   * Instantiates a new ActivityCommentedId
   */
  public ActivityCommentedId() {

  }

  /**
   * Instantiates a new ActivityCommentedId
   * @param posterId
   * @param postId
   * @param updated
   * @param mentionedId
   */
  public ActivityCommentedId(String posterId, String postId, Long updated, Long commentUpdated) {
    this.postId = postId;
    this.posterId = posterId;
    this.updated = updated;
    this.commentUpdated = commentUpdated;
  }

  /**
   * Gets the poster id.
   *
   * @return the poster id
   */
  public String getPosterId() {
    return posterId;
  }

  /**
   * Gets the post id.
   *
   * @return the post id
   */
  public String getPostId() {
    return postId;
  }

  /**
   * Gets the updated.
   *
   * @return the updated
   */
  public Long getUpdated() {
    return updated;
  }

  /**
   * Gets the commentUpdated
   *
   * @return the commentUpdated
   */
  public Long getCommentUpdated() {
    return commentUpdated;
  }

  /**
   * Sets the commentUpdated
   * @param commentUpdated
   */
  public void commentUpdated(Long commentUpdated) {
    this.commentUpdated = commentUpdated;
  }

  /**
   * {@inheritDoc}
   */
  public int hashCode() {
    int hc = 7 + posterId.hashCode() * 31;
    hc = hc * 31 + posterId.hashCode();
    hc = hc * 31 + updated.hashCode();
    hc = hc * 31 + commentUpdated.hashCode();
    return hc;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (o != null) {
      if (ActivityCommentedId.class.isAssignableFrom(o.getClass())) {
        ActivityCommentedId other = ActivityCommentedId.class.cast(o);
        return posterId.equals(other.getPosterId()) && postId.equals(other.getPostId()) && updated.equals(other.getUpdated())
            && commentUpdated.equals(other.getCommentUpdated());
      }
    }
    return false;
  }

}
