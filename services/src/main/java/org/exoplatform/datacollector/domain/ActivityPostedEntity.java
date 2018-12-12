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
import org.exoplatform.datacollector.domain.id.ActivityPostedId;

/**
 * Activity post entity.
 */
@Entity(name = "ActivityPosted")
@ExoEntity
@NamedNativeQueries({
    /* Get user posts */
    @NamedNativeQuery(name = "ActivityPosted.findUserPosts", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date " //
        + " FROM soc_activities a" //
        + " WHERE a.owner_id IS NOT NULL AND a.poster_id = :posterId" //
        + " ORDER BY owner_id, updated_date", resultClass = ActivityPostedEntity.class),
    /* ===== Others do in the user favorite streams ===== */
    /* Others often post in the user favorite streams (find posters) */
    @NamedNativeQuery(name = "ActivityPosted.findPartIsFavoriteStreamPoster", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id AS poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date" //
        + " FROM soc_activities a, soc_identities si"
        + " WHERE a.owner_id IS NOT NULL AND si.identity_id = a.poster_id AND si.provider_id = 'organization'"
        + " AND a.owner_id IN (:favoriteStreams) AND a.poster_id != :posterId"
        + " ORDER BY owner_id, post_id, poster_id", resultClass = ActivityPostedEntity.class)

})
@IdClass(ActivityPostedId.class)
public class ActivityPostedEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 7990009059287150156L;

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
   * Participant ID who posted an activity with the mention: if mentioned in the
   * post it's post author, if in comment then it's commenter. But activity ID
   * will reflect the post in any case (for comment it's post ID where comment
   * was replied).
   */
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
   * Represents the ActivityPostedEntity as a String.
   */
  @Override
  public String toString() {
    return "ActivityPost [id=" + postId + ", posterId=" + posterId + "]";
  }

}
