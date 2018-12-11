package org.exoplatform.datacollector.domain.id;

import java.io.Serializable;

/**
 * The Class ActivityMentionedId.
 */
public class ActivityMentionedId implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 3235629193550319959L;

  /**  The activity id. */
  protected String          postId;

  /**  The poster id. */
  protected String          posterId;

  /** The updated date (timestamp). */
  protected Long            updated;

  /** The mentioned id */
  protected String          mentionedId;

  /**
   * Instantiates a new ActivityMentionedId
   */
  public ActivityMentionedId() {
    super();
  }

  /**
   * Instantiates a new ActivityMentionedId
   * @param posterId
   * @param postId
   * @param updated
   * @param mentionedId
   */
  public ActivityMentionedId(String posterId, String postId, Long updated, String mentionedId) {
    this.postId = postId;
    this.posterId = posterId;
    this.updated = updated;
    this.mentionedId = mentionedId;
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
   * Gets the mentionedId
   *
   * @return the mentionedId
   */
  public String getMentionedId() {
    return mentionedId;
  }

  /**
   * Sets the mentioned id
   * @param mentionedId
   */
  public void setMentionedId(String mentionedId) {
    this.mentionedId = mentionedId;
  }

  /**
   * {@inheritDoc}
   */
  public int hashCode() {
    int hc = 7 + posterId.hashCode() * 31;
    hc = hc * 31 + posterId.hashCode();
    hc = hc * 31 + updated.hashCode();
    hc = hc * 31 + mentionedId.hashCode();
    return hc;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (o != null) {
      if (ActivityMentionedId.class.isAssignableFrom(o.getClass())) {
        ActivityMentionedId other = ActivityMentionedId.class.cast(o);
        return posterId.equals(other.getPosterId()) && postId.equals(other.getPostId()) && updated.equals(other.getUpdated())
            && mentionedId.equals(other.getMentionedId());
      }
    }
    return false;
  }

}
