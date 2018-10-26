package org.exoplatform.datacollector.dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityPostEntity;

public class ActivityPostDAO extends GenericDAOJPAImpl<ActivityPostEntity, String> {

  public List<ActivityPostEntity> findPartIsCommentedPoster(String posterId) {
    try {
      TypedQuery<ActivityPostEntity> query = getEntityManager()
          .createNamedQuery("ActivityPost.findPartIsCommentedPoster",
                            ActivityPostEntity.class);
      try {
        query.setParameter("posterId", posterId);
      } catch (Throwable e) {
        return null;
      }
      try {
        return query.getResultList();
      } catch (NoResultException e) {
        return null;
      }
    } catch (Throwable e) {
      return null;
    }
  }

  public ActivityPostEntity findPartIsCommentedCommenter(String posterId) {
    Query query = getEntityManager().createNativeQuery("ActivityPost.findPartIsCommentedCommenter", ActivityPostEntity.class);
    query.setParameter("posterId", posterId);
    try {
      return (ActivityPostEntity) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ActivityPostEntity findPartIsCommentedConvoPoster(String posterId) {
    TypedQuery<ActivityPostEntity> query = getEntityManager()
                                                             .createNamedQuery("ActivityPost.findPartIsCommentedConvoPoster",
                                                                               ActivityPostEntity.class)
                                                             .setParameter("posterId", posterId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
