package org.exoplatform.datacollector.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

import org.exoplatform.commons.api.persistence.ExoEntity;

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
        + " ORDER BY a.owner_id, updated_date", resultClass = ActivityPostedEntity.class),
    /* ===== Others do in the user favorite streams ===== */
    /* Others often post in the user favorite streams (find posters) */
    @NamedNativeQuery(name = "ActivityPosted.findPartIsFavoriteStreamPoster", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id AS poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date" //
        + " FROM soc_activities a, soc_identities si"
        + " WHERE a.owner_id IS NOT NULL AND si.identity_id = a.poster_id AND si.provider_id = 'organization'"
        + " AND a.owner_id IN (:favoriteStreams) AND a.poster_id != :posterId"
        + " ORDER BY a.owner_id, updated_date", resultClass = ActivityPostedEntity.class)

})

public class ActivityPostedEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 7990009059287150156L;

  /**
   * Represents the ActivityPostedEntity as a String.
   */
  @Override
  public String toString() {
    return "ActivityPost [id=" + postId + ", posterId=" + posterId + "]";
  }

}
