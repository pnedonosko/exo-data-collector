package org.exoplatform.datacollector.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "ActivityMentioned")
@ExoEntity
@NamedNativeQueries({
    /* User often mentions others (find mentioned) */
    @NamedNativeQuery(name = "ActivityMentioned.findPartIsMentioned", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM soc_activities a, soc_mentions m" //
        + " WHERE a.activity_id = m.activity_id AND m.mentioner_id != a.poster_id AND a.owner_id IS NOT NULL"
        + " AND a.poster_id = :posterId" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id AS poster_id, a.owner_id AS owner_id, c.parent_id AS parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM soc_activities a, soc_activities c, soc_mentions m"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = m.activity_id AND m.mentioner_id != c.poster_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND c.poster_id = :posterId" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id AS poster_id, a.owner_id AS owner_id, c.parent_id AS parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM soc_activities a, soc_activities cp, soc_activities c, soc_mentions m"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = c.parent_id AND c.activity_id = m.activity_id"
        + " AND m.mentioner_id != c.poster_id AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL AND c.owner_id IS NULL"
        + " AND c.poster_id = :posterId" //
        + " ORDER BY post_id, parent_id", resultClass = ActivityMentionedEntity.class),
    /* Others often mention the user (find mentioners) */
    @NamedNativeQuery(name = "ActivityMentioned.findPartIsMentioner", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM soc_activities a, soc_mentions m" //
        + " WHERE a.activity_id = m.activity_id AND m.mentioner_id != a.poster_id AND a.owner_id IS NOT NULL"
        + " AND m.mentioner_id = :mentionerId" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id AS poster_id, a.owner_id AS owner_id, c.parent_id AS parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM soc_activities a, soc_activities c, soc_mentions m"
        + " WHERE a.activity_id = c.parent_id AND c.activity_id = m.activity_id AND m.mentioner_id != c.poster_id"
        + " AND a.owner_id IS NOT NULL AND c.owner_id IS NULL AND m.mentioner_id = :mentionerId" //
        + " UNION ALL" //
        + " SELECT a.activity_id AS post_id, a.provider_id AS post_provider_id, a.type AS post_type,"
        + "  c.poster_id AS poster_id, a.owner_id AS owner_id, c.parent_id AS parent_id, a.hidden,"
        + "  a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM soc_activities a, soc_activities cp, soc_activities c, soc_mentions m"
        + " WHERE a.activity_id = cp.parent_id AND cp.activity_id = c.parent_id AND c.activity_id = m.activity_id"
        + " AND m.mentioner_id != c.poster_id AND a.owner_id IS NOT NULL AND cp.owner_id IS NULL AND c.owner_id IS NULL"
        + " AND m.mentioner_id = :mentionerId" //
        + " ORDER BY post_id, parent_id", resultClass = ActivityMentionedEntity.class) })

public class ActivityMentionedEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 2885880561077614334L;

  /**
   * The mentioned identity ID.
   */
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
    return super.getPosterId();
  }

  /**
   * Converts the ActivityMentionedEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityMentionedEntity [id=" + postId + ", posterId=" + posterId + "]";
  }

}
