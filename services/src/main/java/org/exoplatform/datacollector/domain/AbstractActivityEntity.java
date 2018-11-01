package org.exoplatform.datacollector.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Abstract activity entity.
 */
@MappedSuperclass
public abstract class AbstractActivityEntity {

  /**
   * The post ID (activity ID).
   */
  @Id
  @Column(name = "post_id")
  protected String  postId;

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
   * The poster ID.
   */
  @Column(name = "poster_id")
  protected String  posterId;

  /**
   * The owner ID.
   */
  @Column(name = "owner_id")
  protected String  ownerId;

  /** The posted date. */
  @Column(name = "posted_date")
  protected Long    postedDate;

  /** The updated date. */
  @Column(name = "updated_date")
  protected Long    updatedDate;

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
  public String getPostId() {
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
  public String getPosterId() {
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
   * Gets the posted date.
   *
   * @return the posted date
   */
  public Date getPostedDate() {
    return postedDate != null && postedDate > 0 ? new Date(postedDate) : null;
  }

  /**
   * Gets the updated date.
   *
   * @return the updated date
   */
  public Date getUpdatedDate() {
    return updatedDate != null && updatedDate > 0 ? new Date(updatedDate) : null;
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
