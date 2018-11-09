package org.exoplatform.datacollector.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * Activity post entity.
 */
@Entity(name = "ActivityPost")
@ExoEntity
@NamedNativeQueries({
    /* Get user posts */
    @NamedNativeQuery(name = "ActivityPost.findUserPosts", query = "SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id,"
        + "  a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date " //
        + " FROM soc_activities a"
        + " WHERE a.owner_id IS NOT NULL AND a.poster_id = :posterId", resultClass = ActivityPostEntity.class) })

public class ActivityPostEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 7990009059287150156L;

  /**
   * Represents the ActivityPostEntity as a String.
   */
  @Override
  public String toString() {
    return "ActivityPost [id=" + postId + ", posterId=" + posterId + "]";
  }

}
