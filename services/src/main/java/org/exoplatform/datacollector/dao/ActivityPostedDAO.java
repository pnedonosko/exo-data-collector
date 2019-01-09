package org.exoplatform.datacollector.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityPostedEntity;

public class ActivityPostedDAO extends GenericDAOJPAImpl<ActivityPostedEntity, String> {

  public List<ActivityPostedEntity> findUserPosts(String posterId, long sinceTime) {
    try {
      TypedQuery<ActivityPostedEntity> query = getEntityManager()
                                                                 .createNamedQuery("ActivityPosted.findUserPosts",
                                                                                   ActivityPostedEntity.class)
                                                                 .setParameter("posterId", posterId)
                                                                 .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityPostedEntity> findPartIsFavoriteStreamPoster(String posterId,
                                                                   long sinceTime,
                                                                   Collection<String> favoriteStreams) {
    TypedQuery<ActivityPostedEntity> query = getEntityManager()
                                                               .createNamedQuery("ActivityPosted.findPartIsFavoriteStreamPoster",
                                                                                 ActivityPostedEntity.class)
                                                               .setParameter("posterId", posterId)
                                                               .setParameter("sinceTime", sinceTime)
                                                               .setParameter("favoriteStreams", favoriteStreams);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }
}
