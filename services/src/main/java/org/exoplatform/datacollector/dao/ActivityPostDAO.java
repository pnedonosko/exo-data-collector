package org.exoplatform.datacollector.dao;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityPostEntity;

public class ActivityPostDAO extends GenericDAOJPAImpl<ActivityPostEntity, String> {
 
  public ActivityPostEntity findPartIsCommentedPoster(String posterId) {
    TypedQuery<ActivityPostEntity> query = getEntityManager().createNamedQuery("ActivityPost.findPartIsCommentedPoster", ActivityPostEntity.class)
        .setParameter("posterId", posterId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
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
    TypedQuery<ActivityPostEntity> query = getEntityManager().createNamedQuery("ActivityPost.findPartIsCommentedConvoPoster", ActivityPostEntity.class)
        .setParameter("posterId", posterId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
