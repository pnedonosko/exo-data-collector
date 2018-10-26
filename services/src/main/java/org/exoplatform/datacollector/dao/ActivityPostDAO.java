package org.exoplatform.datacollector.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityPostEntity;

public class ActivityPostDAO extends GenericDAOJPAImpl<ActivityPostEntity, String> {

  public List<ActivityPostEntity> findPartIsCommentedPoster(String posterId) {
    try {
      TypedQuery<ActivityPostEntity> query = getEntityManager()
                                                               .createNamedQuery("ActivityPost.findPartIsCommentedPoster",
                                                                                 ActivityPostEntity.class)
                                                               .setParameter("posterId", posterId);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityPostEntity> findPartIsCommentedCommenter(String commenterId) {
    TypedQuery<ActivityPostEntity> query = getEntityManager()
                                                             .createNamedQuery("ActivityPost.findPartIsCommentedCommenter",
                                                                               ActivityPostEntity.class)
                                                             .setParameter("commenterId", commenterId);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityPostEntity> findPartIsCommentedConvoPoster(String posterId) {
    TypedQuery<ActivityPostEntity> query = getEntityManager()
                                                             .createNamedQuery("ActivityPost.findPartIsCommentedConvoPoster",
                                                                               ActivityPostEntity.class)
                                                             .setParameter("posterId", posterId);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }
}
