package org.exoplatform.datacollector.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityCommentedEntity;

public class ActivityCommentedDAO extends GenericDAOJPAImpl<ActivityCommentedEntity, String> {

  public List<ActivityCommentedEntity> findPartIsCommentedPoster(String commenterId, long sinceTime) {
    try {
      TypedQuery<ActivityCommentedEntity> query =
                                                getEntityManager().createNamedQuery("ActivityCommented.findPartIsCommentedPoster",
                                                                                    ActivityCommentedEntity.class)
                                                                  .setParameter("commenterId", commenterId)
                                                                  .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsCommentedCommenter(String commenterId, long sinceTime) {
    TypedQuery<ActivityCommentedEntity> query =
                                              getEntityManager().createNamedQuery("ActivityCommented.findPartIsCommentedCommenter",
                                                                                  ActivityCommentedEntity.class)
                                                                .setParameter("commenterId", commenterId)
                                                                .setParameter("sinceTime", sinceTime);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsCommentedConvoPoster(String commenterId, long sinceTime) {
    TypedQuery<ActivityCommentedEntity> query =
                                              getEntityManager().createNamedQuery("ActivityCommented.findPartIsCommentedConvoPoster",
                                                                                  ActivityCommentedEntity.class)
                                                                .setParameter("commenterId", commenterId)
                                                                .setParameter("sinceTime", sinceTime);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsPostCommenter(String posterId, long sinceTime) {
    TypedQuery<ActivityCommentedEntity> query = getEntityManager()
                                                                  .createNamedQuery("ActivityCommented.findPartIsPostCommenter",
                                                                                    ActivityCommentedEntity.class)
                                                                  .setParameter("posterId", posterId)
                                                                  .setParameter("sinceTime", sinceTime);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsCommentCommenter(String commenterId, long sinceTime) {
    TypedQuery<ActivityCommentedEntity> query =
                                              getEntityManager().createNamedQuery("ActivityCommented.findPartIsCommentCommenter",
                                                                                  ActivityCommentedEntity.class)
                                                                .setParameter("commenterId", commenterId)
                                                                .setParameter("sinceTime", sinceTime);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsConvoCommenter(String posterId, long sinceTime) {
    TypedQuery<ActivityCommentedEntity> query = getEntityManager()
                                                                  .createNamedQuery("ActivityCommented.findPartIsConvoCommenter",
                                                                                    ActivityCommentedEntity.class)
                                                                  .setParameter("posterId", posterId)
                                                                  .setParameter("sinceTime", sinceTime);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsFavoriteStreamCommenter(String posterId,
                                                                         long sinceTime,
                                                                         Collection<String> favoriteStreams) {

    TypedQuery<ActivityCommentedEntity> query =
                                              getEntityManager().createNamedQuery("ActivityCommented.findPartIsFavoriteStreamCommenter",
                                                                                  ActivityCommentedEntity.class)
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
