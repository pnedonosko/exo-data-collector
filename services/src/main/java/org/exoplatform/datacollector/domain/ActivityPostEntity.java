package org.exoplatform.datacollector.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "ActivityPost")
@ExoEntity
@NamedNativeQuery(
    /* User commented someone's post */
    name = "ActivityPost.findPartIsCommentedPoster", query = "SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  a.poster_id AS poster_id, a.owner_id AS owner_id," + "  c.posted AS c_posted_date, c.updated_date AS c_updated_date,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date" + " FROM soc_activities a, soc_activities c"
        + " WHERE a.activity_id = c.parent_id AND a.poster_id != c.poster_id AND a.owner_id IS NOT NULL"
        + " AND c.poster_id = :posterId", resultClass = ActivityPostEntity.class)
public class ActivityPostEntity {

  /**
   * The post ID (activity ID).
   */
  @Id
  @Column(name = "post_id")
  protected String postId;

  /**
   * The provider ID.
   */
  @Column(name = "post_provider_id")
  protected String providerId;

  /**
   * The post type.
   */
  @Column(name = "post_type")
  protected String type;

  /**
   * The poster ID.
   */
  @Column(name = "poster_id")
  protected String posterId;

  /**
   * The owner ID.
   */
  @Column(name = "owner_id")
  protected String ownerId;

  /**
   * Converts the RelevanceEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityPost [id=" + postId + ", posterId=" + posterId + "]";
  }

}
