package org.exoplatform.datacollector.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityLikedEntity;

public class ActivityLikedDAO extends GenericDAOJPAImpl<ActivityLikedEntity, String> {

  public List<ActivityLikedEntity> findPartIsLikedPoster(String likerId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsLikedPoster",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("likerId", likerId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsLikedCommenter(String likerId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsLikedCommenter",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("likerId", likerId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsLikedConvoPoster(String likerId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsLikedConvoPoster",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("likerId", likerId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsPostLiker(String posterId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsPostLiker",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("posterId", posterId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsCommentLiker(String commenterId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsCommentLiker",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("commenterId", commenterId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsConvoLiker(String commenterId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsConvoLiker",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("commenterId", commenterId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsSamePostLiker(String likerId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsSamePostLiker",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("likerId", likerId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsSameCommentLiker(String likerId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsSameCommentLiker",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("likerId", likerId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsSameConvoLiker(String likerId, long sinceTime) {
    try {
      TypedQuery<ActivityLikedEntity> query = getEntityManager()
                                                                .createNamedQuery("ActivityLiked.findPartIsSameConvoLiker",
                                                                                  ActivityLikedEntity.class)
                                                                .setParameter("likerId", likerId)
                                                                .setParameter("sinceTime", sinceTime);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsFavoriteStreamPostLiker(String likerId,
                                                                     long sinceTime,
                                                                     Collection<String> favoriteSpaces) {
    try {
      TypedQuery<ActivityLikedEntity> query =
                                            getEntityManager().createNamedQuery("ActivityLiked.findPartIsFavoriteStreamPostLiker",
                                                                                ActivityLikedEntity.class)
                                                              .setParameter("likerId", likerId)
                                                              .setParameter("sinceTime", sinceTime)
                                                              .setParameter("favoriteStreams", favoriteSpaces);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityLikedEntity> findPartIsFavoriteStreamCommentLiker(String likerId,
                                                                        long sinceTime,
                                                                        Collection<String> favoriteStreams) {
    try {
      TypedQuery<ActivityLikedEntity> query =
                                            getEntityManager().createNamedQuery("ActivityLiked.findPartIsFavoriteStreamCommentLiker",
                                                                                ActivityLikedEntity.class)
                                                              .setParameter("likerId", likerId)
                                                              .setParameter("sinceTime", sinceTime)
                                                              .setParameter("favoriteStreams", favoriteStreams);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

}
