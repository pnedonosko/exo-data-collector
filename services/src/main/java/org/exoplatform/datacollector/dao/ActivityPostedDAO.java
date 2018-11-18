package org.exoplatform.datacollector.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityCommentedEntity;
import org.exoplatform.datacollector.domain.ActivityPostedEntity;

public class ActivityPostedDAO extends GenericDAOJPAImpl<ActivityPostedEntity, String> {

  public List<ActivityCommentedEntity> findUserPosts(String posterId) {
    try {
      TypedQuery<ActivityCommentedEntity> query = getEntityManager()
                                                                    .createNamedQuery("ActivityPosted.findUserPosts",
                                                                                      ActivityCommentedEntity.class)
                                                                    .setParameter("posterId", posterId);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsFavoriteStreamPoster(String posterId, String... favoriteSpaces) {
    TypedQuery<ActivityCommentedEntity> query =
                                              getEntityManager().createNamedQuery("ActivityPosted.findPartIsFavoriteStreamPoster",
                                                                                  ActivityCommentedEntity.class)
                                                                .setParameter("posterId", posterId)
                                                                .setParameter("favoriteSpaces", String.join(",", favoriteSpaces));
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }
}
