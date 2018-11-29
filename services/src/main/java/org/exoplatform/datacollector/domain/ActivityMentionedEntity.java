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
        + " FROM soc_activities a, soc_mentions m"
        + " WHERE a.activity_id = m.activity_id AND m.mentioner_id != a.poster_id AND a.poster_id = :posterId"
        + " ORDER BY owner_id DESC, parent_id, updated_date", resultClass = ActivityMentionedEntity.class),
    /* Others often mention the user (find mentioners) */
    @NamedNativeQuery(name = "ActivityMentioned.findPartIsMentioner", query = "SELECT a.activity_id AS post_id,"
        + "  a.provider_id AS post_provider_id, a.type AS post_type, a.poster_id, a.owner_id, a.parent_id,"
        + "  a.hidden, a.posted AS posted_date, a.updated_date, m.mentioner_id AS mentioned_id"
        + " FROM soc_activities a, soc_mentions m" //
        + " WHERE a.activity_id = m.activity_id AND m.mentioner_id != a.poster_id AND m.mentioner_id = :mentionerId"
        + " ORDER BY owner_id DESC, parent_id, updated_date", resultClass = ActivityMentionedEntity.class) })

public class ActivityMentionedEntity extends AbstractActivityEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 2885880561077614334L;

  /**
   * The mentioned ID.
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

  public void setMentionedId(String mentionedId) {
    this.mentionedId = mentionedId;
  }

  /**
   * Converts the ActivityMentionedEntity to the String
   */
  @Override
  public String toString() {
    return "ActivityMentionedEntity [id=" + postId + ", posterId=" + posterId + "]";
  }

}
