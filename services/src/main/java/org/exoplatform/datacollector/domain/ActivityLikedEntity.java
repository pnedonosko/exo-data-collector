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
import org.exoplatform.datacollector.domain.id.ActivityLikedId;

@Entity(name = "ActivityLiked")
@ExoEntity
@NamedNativeQueries({
    /* ===== User liked others ===== */
    /* User liked others' post (find posters) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsLikedPoster", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = l.activity_id AND a.poster_id != l.liker_id AND a.owner_id IS NOT NULL"
        + " AND l.liker_id = :likerId AND a.posted >= :sinceTime", resultClass = ActivityLikedEntity.class),
    /* User liked others' comments (find commenters) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsLikedCommenter", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES oc, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = l.activity_id AND oc.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL AND l.liker_id = :likerId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  oc.poster_id, a.owner_id, oc.parent_id, a.hidden, a.posted AS posted_date, a.updated_date,"
        + "  l.liker_id, l.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES pc, SOC_ACTIVITIES oc, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = pc.parent_id AND pc.activity_id = oc.parent_id AND oc.activity_id = l.activity_id"
        + " AND oc.poster_id != l.liker_id AND a.owner_id IS NOT NULL AND pc.owner_id IS NULL"
        + " AND oc.owner_id IS NULL AND l.liker_id = :likerId AND a.posted >= :sinceTime"
        + " ORDER BY post_id, parent_id, poster_id", resultClass = ActivityLikedEntity.class),
    /* User liked others' comments in someone's post (find posters) */
    // XXX attempt to solve duplicated by SELECT a.* FROM with aggregation
    // MAX(a.liked_date)
    @NamedNativeQuery(name = "ActivityLiked.findPartIsLikedConvoPoster", query = "SELECT a.post_id,"
        + " a.post_provider_id, a.post_type, a.poster_id, a.owner_id, a.parent_id, a.hidden, a.posted_date, a.updated_date,"
        + " a.liker_id, MAX(a.liked_date) as liked_date" //
        + " FROM (" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  a.poster_id, a.owner_id, a.parent_id, a.hidden, a.posted AS posted_date, a.updated_date,"
        + "  l.liker_id, l.created_date AS liked_date" //
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES oc, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = l.activity_id "
        + " AND oc.poster_id != l.liker_id AND a.poster_id != oc.poster_id AND a.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL AND l.liker_id = :likerId AND a.posted >= :sinceTime"
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  a.poster_id, a.owner_id, a.parent_id, a.hidden, a.posted AS posted_date, a.updated_date,"
        + "  l.liker_id, l.created_date AS liked_date" //
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES oc, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = oc.parent_id AND oc.activity_id = l.activity_id"
        + " AND a.poster_id != oc.poster_id AND oc.poster_id != l.liker_id AND a.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL AND oc.owner_id IS NULL"
        + " AND l.liker_id = :likerId AND a.posted >= :sinceTime" //
        + ") AS a" //
        + " GROUP BY post_id, parent_id, poster_id, owner_id, liker_id, post_provider_id, post_type, hidden, posted_date, updated_date"
        + " ORDER BY post_id", resultClass = ActivityLikedEntity.class),
    /* ===== Others liked the user ===== */
    /* Others like user post (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsPostLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITY_LIKERS l" //
        + " WHERE a.activity_id = l.activity_id AND a.poster_id != l.liker_id AND a.owner_id IS NOT NULL "
        + " AND a.poster_id = :posterId AND a.posted >= :sinceTime", resultClass = ActivityLikedEntity.class),
    /* Others like user comments (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsCommentLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, c.poster_id, a.owner_id, c.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = l.activity_id AND c.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND c.poster_id = :commenterId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id, a.owner_id, c.parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES c, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = c.parent_id AND c.activity_id = l.activity_id"
        + " AND c.poster_id != l.liker_id AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL"
        + " AND c.owner_id IS NULL AND c.poster_id = :commenterId AND a.posted >= :sinceTime" //
        + " ORDER BY post_id, parent_id, liker_id", resultClass = ActivityLikedEntity.class),
    /* Others like posts where user comments (find likers) */
    // TODO here will be duplicates from comments on the post and comments on
    // comments - solve it in SocialInfluencers.addConvoLiker().
    // XXX attempt to solve duplicated by SELECT DISTINCT a.* FROM
    @NamedNativeQuery(name = "ActivityLiked.findPartIsConvoLiker", query = "SELECT DISTINCT a.* FROM ("
        + "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = c.parent_id AND a.activity_id = l.activity_id"
        + " AND c.poster_id != l.liker_id AND a.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND c.poster_id = :commenterId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT DISTINCT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  a.poster_id, a.owner_id, a.parent_id, a.hidden, a.posted AS posted_date, a.updated_date,"
        + "  l.liker_id, l.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES c, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = c.parent_id AND a.activity_id = l.activity_id"
        + " AND c.poster_id != l.liker_id AND a.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL AND c.owner_id IS NULL"
        + " AND c.poster_id = :commenterId AND a.posted >= :sinceTime" //
        + ") AS a" //
        + " ORDER BY post_id, parent_id, liker_id", resultClass = ActivityLikedEntity.class),
    /* ===== Others liked what the user likes ===== */
    /* Others like same posts the user liked (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsSamePostLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITY_LIKERS ol, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = ol.activity_id AND a.activity_id = l.activity_id"
        + " AND a.poster_id != l.liker_id AND a.poster_id != ol.liker_id AND ol.liker_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND l.liker_id = :likerId AND a.posted >= :sinceTime", resultClass = ActivityLikedEntity.class),
    /* Others like same comments the user liked (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsSameCommentLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, c.poster_id, a.owner_id, c.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c, SOC_ACTIVITY_LIKERS ol, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = ol.activity_id AND c.activity_id = l.activity_id"
        + " AND c.poster_id != ol.liker_id AND c.poster_id != l.liker_id AND ol.liker_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND l.liker_id = :likerId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id, a.owner_id, c.parent_id, a.hidden, a.posted AS posted_date, a.updated_date,"
        + "  ol.liker_id, ol.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES c, SOC_ACTIVITY_LIKERS ol, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = c.parent_id"
        + " AND c.activity_id = ol.activity_id AND c.activity_id = l.activity_id"
        + " AND c.poster_id != ol.liker_id AND c.poster_id != l.liker_id AND ol.liker_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL AND c.owner_id IS NULL"
        + " AND l.liker_id = :likerId AND a.posted >= :sinceTime"
        + " ORDER BY post_id, parent_id, liker_id", resultClass = ActivityLikedEntity.class),
    /* Others like posts where the user likes only others' comments (likers) */
    // TODO here will be duplicates from comments on the post and comments on
    // comments - solve it in SocialInfluencers.addLikedConvoPoster().
    // XXX attempt to solve duplicated by SELECT DISTINCT a.* FROM
    @NamedNativeQuery(name = "ActivityLiked.findPartIsSameConvoLiker", query = "SELECT DISTINCT a.* FROM ("
        + "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c, SOC_ACTIVITY_LIKERS ol, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id NOT IN (SELECT activity_id FROM SOC_ACTIVITY_LIKERS WHERE liker_id = l.liker_id)"
        + " AND a.activity_id = c.parent_id AND a.activity_id = ol.activity_id AND c.activity_id = l.activity_id"
        + " AND a.poster_id != ol.liker_id AND a.poster_id != l.liker_id"
        + " AND c.poster_id != l.liker_id AND ol.liker_id != l.liker_id AND ol.activity_id != l.activity_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND l.liker_id = :likerId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  a.poster_id, a.owner_id, a.parent_id, a.hidden, a.posted AS posted_date, a.updated_date,"
        + "  ol.liker_id, ol.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES c, SOC_ACTIVITY_LIKERS ol, SOC_ACTIVITY_LIKERS l"
        + " WHERE a.activity_id NOT IN (SELECT activity_id FROM SOC_ACTIVITY_LIKERS WHERE liker_id = l.liker_id)"
        + " AND a.activity_id = cp.parent_id AND cp.activity_id = c.parent_id"
        + " AND a.activity_id = ol.activity_id AND c.activity_id = l.activity_id"
        + " AND a.poster_id != ol.liker_id AND a.poster_id != l.liker_id"
        + " AND c.poster_id != l.liker_id AND ol.liker_id != l.liker_id AND ol.activity_id != l.activity_id"
        + " AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL AND c.owner_id IS NULL"
        + " AND l.liker_id = :likerId AND a.posted >= :sinceTime" //
        + ") AS a" //
        + " ORDER BY post_id, parent_id, liker_id", resultClass = ActivityLikedEntity.class),
    /* ===== Others do in the user favorite streams ===== */
    /* Others often like posts in the user favorite streams (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsFavoriteStreamPostLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITY_LIKERS ol, SOC_IDENTITIES si"
        + " WHERE si.identity_id = ol.liker_id AND si.provider_id = 'organization' AND a.activity_id = ol.activity_id"
        + " AND a.owner_id IS NOT NULL AND a.owner_id IN (:favoriteStreams) AND ol.liker_id != :likerId AND a.posted >= :sinceTime"
        + " ORDER BY post_id, parent_id, liker_id", resultClass = ActivityLikedEntity.class),
    /* Others often like comments in the user favorite streams (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsFavoriteStreamCommentLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES oc, SOC_ACTIVITY_LIKERS ol"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = ol.activity_id AND a.owner_id IS NOT NULL"
        + " AND a.owner_id IN (:favoriteStreams) AND ol.liker_id != :likerId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  oc.poster_id, a.owner_id, oc.parent_id," //
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES oc, SOC_ACTIVITY_LIKERS ol"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = oc.parent_id"
        + " AND oc.activity_id = ol.activity_id AND a.owner_id IS NOT NULL"
        + " AND a.owner_id IN (:favoriteStreams) AND ol.liker_id != :likerId AND a.posted >= :sinceTime"
        + " ORDER BY post_id, parent_id, liker_id", resultClass = ActivityLikedEntity.class) })

@IdClass(ActivityLikedId.class)
public class ActivityLikedEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 2885880561077614334L;

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
   * The liker ID.
   */
  @Id
  @Column(name = "liker_id")
  protected String          likerId;

  /**
   * The liked date.
   */
  @Column(name = "liked_date")
  protected Date            likedDate;

  /**
   * Gets the liked date.
   *
   * @return the liked date
   */
  public Date getLikedDate() {
    return likedDate;
  }

  /**
   * Gets the liked time in milliseconds.
   *
   * @return the liked
   */
  public Long getLiked() {
    return likedDate != null ? likedDate.getTime() : null;
  }

  /**
   * Gets the liker id.
   *
   * @return the liker id
   */
  public String getLikerId() {
    return likerId;
  }

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
   * Converts the ActivityLikeEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityLike [id=" + postId + ", posterId=" + posterId + "]";
  }

}
