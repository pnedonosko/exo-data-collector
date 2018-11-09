package org.exoplatform.datacollector.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.ActivityCommentedEntity;
import org.exoplatform.datacollector.domain.ActivityPostEntity;

public class ActivityCommentedDAO extends GenericDAOJPAImpl<ActivityPostEntity, String> {

  public List<ActivityCommentedEntity> findPartIsCommentedPoster(String commenterId) {
    try {
      TypedQuery<ActivityCommentedEntity> query =
                                                getEntityManager().createNamedQuery("ActivityCommented.findPartIsCommentedPoster",
                                                                                    ActivityCommentedEntity.class)
                                                                  .setParameter("commenterId", commenterId);
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsCommentedCommenter(String commenterId) {
    TypedQuery<ActivityCommentedEntity> query =
                                              getEntityManager().createNamedQuery("ActivityCommented.findPartIsCommentedCommenter",
                                                                                  ActivityCommentedEntity.class)
                                                                .setParameter("commenterId", commenterId);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public List<ActivityCommentedEntity> findPartIsCommentedConvoPoster(String commenterId) {
    TypedQuery<ActivityCommentedEntity> query =
                                              getEntityManager().createNamedQuery("ActivityCommented.findPartIsCommentedConvoPoster",
                                                                                  ActivityCommentedEntity.class)
                                                                .setParameter("commenterId", commenterId);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }
}
