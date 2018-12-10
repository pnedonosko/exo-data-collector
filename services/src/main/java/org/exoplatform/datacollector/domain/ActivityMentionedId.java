package org.exoplatform.datacollector.domain;

import javax.persistence.Id;

/**
 * The Class ActivityMentionedId.
 */
public class ActivityMentionedId extends ActivityId {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 3235629193550329959L;

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
    super(posterId, postId, updated);
    this.mentionedId = mentionedId;
  }

  /*
   * Gets the mentioned id
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

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    if (mentionedId != null) {
      return super.hashCode() * 31 + mentionedId.hashCode();
    }
    return super.hashCode();
  }

}
