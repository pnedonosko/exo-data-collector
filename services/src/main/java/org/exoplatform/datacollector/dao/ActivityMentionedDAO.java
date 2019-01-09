package org.exoplatform.datacollector.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityMentionedEntity;

public class ActivityMentionedDAO extends GenericDAOJPAImpl<ActivityMentionedEntity, String> {

  public List<ActivityMentionedEntity> findPartIsMentioned(String posterId, long sinceTime) {
    try {
      TypedQuery<ActivityMentionedEntity> query = getEntityManager()
                                                                    .createNamedQuery("ActivityMentioned.findPartIsMentioned",
                                                                                      ActivityMentionedEntity.class)
                                                                    .setParameter("posterId", posterId)
                                                                    .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityMentionedEntity> findPartIsMentioner(String mentionerId, long sinceTime) {
    try {
      TypedQuery<ActivityMentionedEntity> query = getEntityManager()
                                                                    .createNamedQuery("ActivityMentioned.findPartIsMentioner",
                                                                                      ActivityMentionedEntity.class)
                                                                    .setParameter("mentionerId", mentionerId)
                                                                    .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }
}
