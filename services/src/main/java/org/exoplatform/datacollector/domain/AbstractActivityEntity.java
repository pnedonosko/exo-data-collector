package org.exoplatform.datacollector.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;

/**
 * Abstract activity entity.
 */
@MappedSuperclass
@IdClass(ActivityId.class)
public abstract class AbstractActivityEntity {

  /**
   * The post ID (activity ID).
   */
  @Id
  @Column(name = "post_id")
  protected Long    postId;

  /**
   * The poster ID.
   */
  @Id
  @Column(name = "poster_id")
  protected Long    posterId;

  /** The updated date. */
  @Column(name = "updated_date")
  @Id
  protected Long    updated;

  /** The posted date. */
  @Column(name = "posted_date")
  protected Long    posted;

  /**
   * The provider ID.
   */
  @Column(name = "post_provider_id")
  protected String  providerId;

  /**
   * The post type.
   */
  @Column(name = "post_type")
  protected String  type;

  /**
   * The owner ID.
   */
  @Column(name = "owner_id")
  protected String  ownerId;

  /** The hidden. */
  @Column(name = "hidden")
  protected Boolean hidden;

  /** The parent id. */
  @Column(name = "parent_id")
  protected String  parentId;

  /**
   * Gets the post id.
   *
   * @return the post id
   */
  public Long getId() {
    return postId;
  }

  /**
   * Gets the provider id.
   *
   * @return the provider id
   */
  public String getProviderId() {
    return providerId;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Gets the poster id.
   *
   * @return the poster id
   */
  public Long getPosterId() {
    return posterId;
  }

  /**
   * Gets the owner id.
   *
   * @return the owner id
   */
  public String getOwnerId() {
    return ownerId;
  }

  /**
   * Gets the posted time in milliseconds.
   *
   * @return the posted
   */
  public Long getPosted() {
    return posted;
  }

  /**
   * Gets the posted date.
   *
   * @return the posted date
   */
  public Date getPostedDate() {
    return posted != null && posted > 0 ? new Date(posted) : null;
  }

  /**
   * Gets the updated date.
   *
   * @return the updated date
   */
  public Date getUpdatedDate() {
    return updated != null && updated > 0 ? new Date(updated) : null;
  }

  /**
   * Gets the updated time in milliseconds.
   *
   * @return the updated
   */
  public Long getUpdated() {
    return updated;
  }

  /**
   * Gets the hidden.
   *
   * @return the hidden
   */
  public Boolean getHidden() {
    return hidden;
  }

  /**
   * Gets the parent id.
   *
   * @return the parent id
   */
  public String getParentId() {
    return parentId;
  }

}
