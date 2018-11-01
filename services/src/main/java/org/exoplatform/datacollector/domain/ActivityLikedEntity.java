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
    /* User liked other users' post */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsLikedPoster", query = "SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id,"
        + "  a.type AS post_type, a.poster_id AS poster_id, a.owner_id AS owner_id, a.title, a.title_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.created_date AS liked_date"
        + "FROM soc_activities a, soc_activity_likers l"
        + "WHERE a.activity_id = l.activity_id AND a.poster_id != l.liker_id AND a.owner_id IS NOT NULL AND l.liker_id = :likerId", resultClass = ActivityLikedEntity.class),
    /* User liked other users' comments */
    @NamedNativeQuery(name = "ActivityLiked.findPartIsLikedCommenter", query = "SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id,"
        + "  a.type AS post_type, a.poster_id AS poster_id, a.owner_id AS owner_id, a.title, a.title_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, l.created_date AS liked_date"
        + "FROM soc_activities a, soc_activity_likers l"
        + "WHERE a.activity_id = l.activity_id AND a.poster_id != l.liker_id AND a.owner_id IS NOT NULL AND l.liker_id = :likerId", resultClass = ActivityLikedEntity.class) })

public class ActivityLikedEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 2885880561077614334L;
  
  /**
   * The liked date.
   */
  @Column(name = "liked_date")
  protected Date likedDate;

  /**
   * Gets the liked date.
   *
   * @return the liked date
   */
  public Date getLikedDate() {
    return likedDate;
  }

  /**
   * Converts the ActivityLikeEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityLike [id=" + postId + ", posterId=" + posterId + "]";
  }

}
