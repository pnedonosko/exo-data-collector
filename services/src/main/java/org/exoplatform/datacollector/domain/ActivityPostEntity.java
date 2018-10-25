package org.exoplatform.datacollector.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
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
        + " AND c.poster_id = :posterId", resultClass = ActivityPostEntity.class),
    /* User commented other users in someone's post (conversation poster) */
    @NamedNativeQuery(name = "ActivityPost.findPartIsCommentedConvoPoster", query = "SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id,"
        + "  a.type AS post_type, a.poster_id AS poster_id, a.owner_id AS owner_id, "
        + "  c.posted AS c_posted_date, c.updated_date AS c_updated_date, a.hidden, a.posted AS posted_date, a.updated_date "
        + " FROM soc_activities a, soc_activities oc, soc_activities c"
        + " WHERE a.activity_id = oc.parent_id AND oc.activity_id = c.parent_id AND oc.poster_id != c.poster_id AND a.poster_id != c.poster_id AND a.owner_id IS NOT NULL AND oc.owner_id IS NULL"
        + " AND c.poster_id = :posterId", resultClass = ActivityPostEntity.class) })
public class ActivityPostEntity {

  /**
   * The post ID (activity ID).
   */
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
   * Converts the RelevanceEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityPost [id=" + postId + ", posterId=" + posterId + "]";
  }

}
