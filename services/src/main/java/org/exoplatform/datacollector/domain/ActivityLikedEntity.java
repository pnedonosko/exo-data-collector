package org.exoplatform.datacollector.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "ActivityLiked")
@ExoEntity
@NamedNativeQueries({
    /* ===== User liked others ===== */
    /* User liked others' post (find posters) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsLikedPoster", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM soc_activities a, soc_activity_likers l"
        + " WHERE a.activity_id = l.activity_id AND a.poster_id != l.liker_id AND a.owner_id IS NOT NULL"
        + " AND l.liker_id = :likerId", resultClass = ActivityLikedEntity.class),
    /* User liked others' comments (find commenters) */
    /* TODO take in account comments on comments */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsLikedCommenter", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM soc_activities a, soc_activities oc, soc_activity_likers l"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = l.activity_id AND oc.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL AND l.liker_id = :likerId", resultClass = ActivityLikedEntity.class),
    /* User liked others' comments in someone's post (find posters) */
    /* TODO take in account comments on comments */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsLikedConvoPoster", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM soc_activities a, soc_activities oc, soc_activity_likers l"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = l.activity_id"
        + " AND oc.poster_id != l.liker_id AND a.poster_id != oc.poster_id"
        + " AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL AND l.liker_id = :likerId", resultClass = ActivityLikedEntity.class),
    /* ===== Others liked the user ===== */
    /* Others like user post (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsPostLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM soc_activities a, soc_activity_likers l" //
        + " WHERE a.activity_id = l.activity_id AND a.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND a.poster_id = :posterId", resultClass = ActivityLikedEntity.class),
    /* Others like user comments (find likers) */
    /* TODO take in account comments on comments */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsCommentLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, c.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id AS liker_id, l.created_date AS liked_date"
        + " FROM soc_activities a, soc_activities c, soc_activity_likers l"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = l.activity_id AND c.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND c.poster_id = :commenterId", resultClass = ActivityLikedEntity.class),
    /* Others like posts where user comments (find likers) */
    /* TODO take in account comments on comments */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsConvoLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.liker_id, l.created_date AS liked_date"
        + " FROM soc_activities a, soc_activities c, soc_activity_likers l"
        + " WHERE a.activity_id = c.parent_id AND a.activity_id = l.activity_id"
        + " AND c.poster_id != l.liker_id AND a.poster_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND c.poster_id = :commenterId", resultClass = ActivityLikedEntity.class),
    /* ===== Others liked what the user likes ===== */
    /* Others like same posts the user liked (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsSamePostLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM soc_activities a, soc_activity_likers ol, soc_activity_likers l"
        + " WHERE a.activity_id = ol.activity_id AND a.activity_id = l.activity_id"
        + " AND a.poster_id != l.liker_id AND a.poster_id != ol.liker_id AND ol.liker_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND l.liker_id = :likerId", resultClass = ActivityLikedEntity.class),
    /* Others like same comments the user liked (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsSameCommentLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, c.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM soc_activities a, soc_activities c, soc_activity_likers ol, soc_activity_likers l"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = ol.activity_id AND c.activity_id = l.activity_id"
        + " AND c.poster_id != ol.liker_id AND c.poster_id != l.liker_id AND ol.liker_id != l.liker_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND l.liker_id = :likerId", resultClass = ActivityLikedEntity.class),
    /* Others like in posts where the user liked other comments (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsSameConvoLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM soc_activities a, soc_activities oc, soc_activities c, soc_activity_likers ol, soc_activity_likers l"
        + " WHERE a.activity_id = c.parent_id AND a.activity_id = oc.parent_id AND oc.activity_id = ol.activity_id"
        + " AND c.activity_id = l.activity_id AND oc.poster_id != ol.liker_id AND oc.poster_id != l.liker_id"
        + " AND c.poster_id != l.liker_id AND ol.liker_id != l.liker_id AND ol.activity_id != l.activity_id"
        + " AND oc.owner_id IS NULL AND c.owner_id IS NULL AND l.liker_id = :likerId", resultClass = ActivityLikedEntity.class),
    /* ===== Others do in the user favorite streams ===== */
    /* Others often like posts in the user favorite streams (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsFavoriteStreamPostLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM soc_activities a, soc_activity_likers ol, soc_identities si"
        + " WHERE si.identity_id = ol.liker_id AND si.provider_id = 'organization' AND a.activity_id = ol.activity_id"
        + " AND a.owner_id IS NOT NULL AND a.owner_id IN (:favoriteStreams) AND ol.liker_id != :likerId"
        + " ORDER BY a.owner_id, liked_date", resultClass = ActivityCommentedEntity.class),
    /* Others often like comments in the user favorite streams (find likers) */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsFavoriteStreamCommentLiker", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, oc.poster_id, a.owner_id, oc.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM soc_activities a, soc_activities oc, soc_activity_likers ol"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = ol.activity_id AND a.owner_id IS NOT NULL"
        + " AND a.owner_id IN (:favoriteStreams) AND ol.liker_id != :likerId" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  oc.poster_id, a.owner_id, oc.parent_id," //
        + "  a.hidden, a.posted AS posted_date, a.updated_date, ol.liker_id, ol.created_date AS liked_date"
        + " FROM soc_activities a, soc_activities cp, soc_activities oc, soc_activity_likers ol"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = oc.parent_id"
        + " AND oc.activity_id = ol.activity_id AND a.owner_id IS NOT NULL"
        + " AND a.owner_id IN (:favoriteStreams) AND ol.liker_id != :likerId"
        + " ORDER BY a.owner_id, liked_date", resultClass = ActivityCommentedEntity.class)

})

public class ActivityLikedEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 2885880561077614334L;

  /**
   * The liker ID.
   */
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

  /**
   * Converts the ActivityLikeEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityLike [id=" + postId + ", posterId=" + posterId + "]";
  }

}
