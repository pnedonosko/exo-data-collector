package org.exoplatform.datacollector.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "ActivityPost")
@ExoEntity
@NamedNativeQueries({
    /* User commented someone's post */
    @NamedNativeQuery(name = "ActivityPost.findPartIsCommentedPoster", query = "SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id,"
        + "  a.type AS post_type, a.poster_id AS poster_id, a.owner_id AS owner_id, "
        + "  c.posted AS c_posted_date, c.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM soc_activities a, soc_activities c"
        + " WHERE a.activity_id = c.parent_id AND a.poster_id != c.poster_id AND a.owner_id IS NOT NULL"
        + " AND c.poster_id = :posterId", resultClass = ActivityPostEntity.class),
    /* User commented someone's comment in a post */
    @NamedNativeQuery(name = "ActivityPost.findPartIsCommentedCommenter", query = "SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id,"
        + "  a.type AS post_type, oc.poster_id AS poster_id, a.owner_id AS owner_id, "
        + "  c.posted AS c_posted_date, c.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM soc_activities a, soc_activities oc, soc_activities c"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = c.parent_id AND oc.poster_id != c.poster_id AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL"
        + " AND c.poster_id = :commenterId", resultClass = ActivityPostEntity.class),
    /* User commented other users in someone's post (conversation poster) */
    @NamedNativeQuery(name = "ActivityPost.findPartIsCommentedConvoPoster", query = "SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id,"
        + "  a.type AS post_type, a.poster_id AS poster_id, a.owner_id AS owner_id, "
        + "  c.posted AS c_posted_date, c.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM soc_activities a, soc_activities oc, soc_activities c"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = c.parent_id AND oc.poster_id != c.poster_id AND a.poster_id != c.poster_id AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL"
        + " AND c.poster_id = :posterId", resultClass = ActivityPostEntity.class) })
public class ActivityPostEntity  implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 7990009059287150156L;

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

  /**
   * The posted date
   */
  @Column(name = "posted_date")
  protected Date    postedDate;

  /**
   * The updated date
   */
  @Column(name = "posted_date")
  protected Date    updatedDate;

  /**
   * The hidden
   */
  @Column(name = "hidden")
  protected Boolean hidden;

  /**
   * The parent id
   */
  @Column(name = "parent_id")
  protected String  parentId;

  /**
   * The comment posted date
   */
  @Column(name = "c_posted_date")
  protected Date    commentPostedDate;

  /**
   * The comment posted date
   */
  @Column(name = "c_updated_date")
  protected Date    commentUpdatedDate;

  /**
   * Gets the post ID
   * 
   * @return postId
   */
  public String getPostId() {
    return postId;
  }

  /**
   * Sets the post ID
   * 
   * @param postId
   */
  public void setPostId(String postId) {
    this.postId = postId;
  }

  /**
   * Gets the provider ID
   * 
   * @return provider ID
   */
  public String getProviderId() {
    return providerId;
  }

  /**
   * Sets the provider ID
   * 
   * @param providerId
   */
  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  /**
   * Gets the type
   * 
   * @return type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type
   * 
   * @param type
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Gets the poster ID
   * 
   * @return poster ID
   */
  public String getPosterId() {
    return posterId;
  }

  /**
   * Sets the poster ID
   * 
   * @param posterId
   */
  public void setPosterId(String posterId) {
    this.posterId = posterId;
  }

  /**
   * Gets the owner ID
   * 
   * @return
   */
  public String getOwnerId() {
    return ownerId;
  }

  /**
   * Sets the owner ID
   * 
   * @param ownerId
   */
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  /**
   * Gets the posted date
   * 
   * @return posted date
   */
  public Date getPostedDate() {
    return postedDate;
  }

  /**
   * Sets the posted date
   * 
   * @param postedDate
   */
  public void setPostedDate(Date postedDate) {
    this.postedDate = postedDate;
  }

  /**
   * Gets the updated date
   * 
   * @return updated date
   */
  public Date getUpdatedDate() {
    return updatedDate;
  }

  /**
   * Sets the updated date
   * 
   * @param updatedDate
   */
  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  /**
   * Gets the hidden
   * 
   * @return hidden
   */
  public Boolean getHidden() {
    return hidden;
  }

  /**
   * Sets the hidden
   * 
   * @param hidden
   */
  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  /**
   * Gets the parent ID
   * 
   * @return parent ID
   */
  public String getParentId() {
    return parentId;
  }

  /**
   * Sets the parent ID
   * 
   * @param parentId
   */
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  /**
   * Gets the comment posted date
   * 
   * @return comment posted date
   */
  public Date getCommentPostedDate() {
    return commentPostedDate;
  }

  /**
   * Sets the comment posted date
   * 
   * @param commentPostedDate
   */
  public void setCommentPostedDate(Date commentPostedDate) {
    this.commentPostedDate = commentPostedDate;
  }

  /**
   * Gets the comment updated date
   * 
   * @return comment updated date
   */
  public Date getCommentUpdatedDate() {
    return commentUpdatedDate;
  }

  /**
   * Sets the comment updated date
   * 
   * @param commentUpdatedDate
   */
  public void setCommentUpdatedDate(Date commentUpdatedDate) {
    this.commentUpdatedDate = commentUpdatedDate;
  }

  /**
   * Converts the RelevanceEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityPost [id=" + postId + ", posterId=" + posterId + "]";
  }

}
