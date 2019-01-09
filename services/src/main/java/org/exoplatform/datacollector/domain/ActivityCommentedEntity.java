package org.exoplatform.datacollector.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.datacollector.domain.id.ActivityCommentedId;

@Entity(name = "ActivityCommented")
@ExoEntity
@NamedNativeQueries({
    /* ===== User commented others ===== */
    /* User commented someone's post (find posters) */
    @NamedNativeQuery(name = "ActivityCommented.findPartIsCommentedPoster", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  c.posted AS c_posted_date, c.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c"
        + " WHERE a.activity_id = c.parent_id AND a.poster_id != c.poster_id AND a.owner_id IS NOT NULL"
        + " AND c.poster_id = :commenterId AND a.posted >= :sinceTime", resultClass = ActivityCommentedEntity.class),
    /* User commented someone's comment in a post (find commenters) */
    @NamedNativeQuery(name = "ActivityCommented.findPartIsCommentedCommenter", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  c.posted AS c_posted_date, c.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES oc, SOC_ACTIVITIES c"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = c.parent_id AND oc.poster_id != c.poster_id"
        + " AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL"
        + " AND c.poster_id = :commenterId AND a.posted >= :sinceTime", resultClass = ActivityCommentedEntity.class),
    /* User commented others in someone's post (find poster) */
    @NamedNativeQuery(name = "ActivityCommented.findPartIsCommentedConvoPoster", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  c.posted AS c_posted_date, c.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES oc, SOC_ACTIVITIES c"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = c.parent_id"
        + " AND oc.poster_id != c.poster_id AND a.poster_id != c.poster_id"
        + " AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL"
        + " AND c.poster_id = :commenterId AND a.posted >= :sinceTime", resultClass = ActivityCommentedEntity.class),
    /* ===== Others commented the user ===== */
    /* Others often comment the user posts (find commenters) */
    @NamedNativeQuery(name = "ActivityCommented.findPartIsPostCommenter", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  oc.posted AS c_posted_date, oc.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES oc" //
        + " WHERE a.activity_id = oc.parent_id AND a.poster_id != oc.poster_id AND a.owner_id IS NOT NULL"
        + " AND a.poster_id = :posterId AND a.posted >= :sinceTime", resultClass = ActivityCommentedEntity.class),
    /* Others often comment the user comments (find commenters) */
    @NamedNativeQuery(name = "ActivityCommented.findPartIsCommentCommenter", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  oc.posted AS c_posted_date, oc.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c, SOC_ACTIVITIES oc"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = oc.parent_id AND oc.poster_id != c.poster_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL"
        + " AND c.poster_id = :commenterId AND a.posted >= :sinceTime", resultClass = ActivityCommentedEntity.class),
    /* Others often comment others in the user posts (find commenters) */
    @NamedNativeQuery(name = "ActivityCommented.findPartIsConvoCommenter", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  oc.posted AS c_posted_date, oc.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c, SOC_ACTIVITIES oc"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = oc.parent_id"
        + " AND a.poster_id != c.poster_id AND a.poster_id != oc.poster_id"
        + " AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL"
        + " AND a.poster_id = :posterId AND a.posted >= :sinceTime", resultClass = ActivityCommentedEntity.class),
    /* ===== Others do in the user favorite streams ===== */
    /* Others often comment in the user favorite streams (find commenters) */
    @NamedNativeQuery(name = "ActivityCommented.findPartIsFavoriteStreamCommenter", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  oc.posted AS c_posted_date, oc.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES oc, SOC_IDENTITIES si"
        + " WHERE si.identity_id = a.poster_id AND si.provider_id = 'organization'"
        + " AND a.activity_id = oc.parent_id AND a.owner_id IS NOT NULL"
        + " AND a.owner_id IN (:favoriteStreams) AND oc.poster_id != :posterId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  oc.poster_id, a.owner_id, oc.parent_id, oc.posted AS c_posted_date, oc.updated_date AS c_updated_date,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date" //
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES oc, SOC_IDENTITIES si"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = oc.parent_id AND si.identity_id = a.poster_id"
        + " AND si.provider_id = 'organization' AND a.owner_id IS NOT NULL" //
        + " AND a.owner_id IN (:favoriteStreams) AND oc.poster_id != :posterId AND a.posted >= :sinceTime" //
        + " ORDER BY post_id, parent_id, poster_id", resultClass = ActivityCommentedEntity.class) })
@IdClass(ActivityCommentedId.class)
public class ActivityCommentedEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -1401311373865460325L;

  /**
   * The post ID (activity ID).
   */
  @Id
  @Column(name = "post_id")
  protected String          postId;

  /**
   * The poster ID.
   */
  @Id
  @Column(name = "poster_id")
  protected String          posterId;

  /** The updated date. */
  @Id
  @Column(name = "updated_date")
  protected Long            updated;

  /** The posted date. */
  @Column(name = "posted_date")
  protected Long            posted;

  /**
   * The provider ID.
   */
  @Column(name = "post_provider_id")
  protected String          providerId;

  /**
   * The post type.
   */
  @Column(name = "post_type")
  protected String          type;

  /**
   * The owner ID.
   */
  @Column(name = "owner_id")
  protected String          ownerId;

  /** The hidden. */
  @Column(name = "hidden")
  protected Boolean         hidden;

  /** The parent id. */
  @Column(name = "parent_id")
  protected String          parentId;
  
  /**
   * The comment posted date.
   */
  @Column(name = "c_posted_date")
  protected Long            commentPosted;

  /**
   * The comment updated date.
   */
  @Id
  @Column(name = "c_updated_date")
  protected Long            commentUpdated;

  @Override
  public String getPosterId() {
    return posterId;
  }

  @Override
  public String getId() {
    return postId;
  }

  @Override
  public String getProviderId() {
    return providerId;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public Long getPosted() {
    return posted;
  }

  @Override
  public Long getUpdated() {
    return updated;
  }

  @Override
  public Date getPostedDate() {
    return posted != null && posted > 0 ? new Date(posted) : null;
  }

  @Override
  public Date getUpdatedDate() {
    return updated != null && updated > 0 ? new Date(updated) : null;
  }

  @Override
  public Boolean getHidden() {
    return hidden;
  }

  @Override
  public String getParentId() {
    return parentId;
  }
  
  
  /**
   * Gets the comment posted time in milliseconds.
   *
   * @return the comment posted
   */
  public Long getCommentPosted() {
    return commentPosted;
  }

  /**
   * Gets the comment updated time in milliseconds.
   *
   * @return the comment updated
   */
  public Long getCommentUpdated() {
    return commentUpdated;
  }

  /**
   * Gets the comment posted date.
   * 
   * @return comment posted date
   */
  public Date getCommentPostedDate() {
    return commentPosted != null && commentPosted > 0 ? new Date(commentPosted) : null;
  }

  /**
   * Gets the comment updated date.
   * 
   * @return comment updated date
   */
  public Date getCommentUpdatedDate() {
    return commentUpdated != null && commentUpdated > 0 ? new Date(commentUpdated) : null;
  }
  

  /**
   * Converts the ActivityCommentedEntity to the String.
   */
  @Override
  public String toString() {
    return "ActivityComment [id=" + postId + ", posterId=" + posterId + "]";
  }

}
