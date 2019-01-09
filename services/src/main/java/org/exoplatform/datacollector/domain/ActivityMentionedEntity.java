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
import org.exoplatform.datacollector.domain.id.ActivityMentionedId;

@Entity(name = "ActivityMentioned")
@ExoEntity
@NamedNativeQueries({
    /* User often mentions others (find mentioned) */
    @NamedNativeQuery(name = "ActivityMentioned.findPartIsMentioned", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM SOC_ACTIVITIES a, SOC_MENTIONS m" //
        + " WHERE a.activity_id = m.activity_id AND m.mentioner_id != a.poster_id AND a.owner_id IS NOT NULL"
        + " AND a.poster_id = :posterId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id, a.owner_id, c.parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c, SOC_MENTIONS m"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = m.activity_id AND m.mentioner_id != c.poster_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND c.poster_id = :posterId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id, a.owner_id, c.parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES c, SOC_MENTIONS m"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = c.parent_id AND c.activity_id = m.activity_id"
        + " AND m.mentioner_id != c.poster_id AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL AND c.owner_id IS NULL"
        + " AND c.poster_id = :posterId AND a.posted >= :sinceTime" //
        + " ORDER BY post_id, parent_id, mentioned_id", resultClass = ActivityMentionedEntity.class),
    /* Others often mention the user (find mentioners) */
    @NamedNativeQuery(name = "ActivityMentioned.findPartIsMentioner", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM SOC_ACTIVITIES a, SOC_MENTIONS m" //
        + " WHERE a.activity_id = m.activity_id AND m.mentioner_id != a.poster_id AND a.owner_id IS NOT NULL"
        + " AND m.mentioner_id = :mentionerId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id, a.owner_id, c.parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES c, SOC_MENTIONS m"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = m.activity_id AND m.mentioner_id != c.poster_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND m.mentioner_id = :mentionerId AND a.posted >= :sinceTime" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id, a.owner_id, c.parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM SOC_ACTIVITIES a, SOC_ACTIVITIES cp, SOC_ACTIVITIES c, SOC_MENTIONS m"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = c.parent_id AND c.activity_id = m.activity_id"
        + " AND m.mentioner_id != c.poster_id AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL AND c.owner_id IS NULL"
        + " AND m.mentioner_id = :mentionerId AND a.posted >= :sinceTime" //
        + " ORDER BY post_id, parent_id, poster_id", resultClass = ActivityMentionedEntity.class) })
@IdClass(ActivityMentionedId.class)
public class ActivityMentionedEntity extends AbstractActivityEntity implements Serializable {

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
   * The mentioned identity ID.
   */
  @Id
  @Column(name = "mentioned_id")
  protected String          mentionedId;
  
  /**
   * Gets the mentioned ID.
   *
   * @return the mentioned id
   */
  public String getMentionedId() {
    return mentionedId;
  }

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
   * Converts the ActivityMentionedEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityMentionedEntity [id=" + postId + ", posterId=" + posterId + "]";
  }
}
