package org.exoplatform.prediction.model.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.prediction.model.domain.ModelEntity;
import org.exoplatform.prediction.model.domain.ModelEntity.Status;
import org.exoplatform.prediction.model.domain.ModelId;

public class ModelEntityDAO extends GenericDAOJPAImpl<ModelEntity, ModelId> {

  public List<ModelEntity> findByName(String name) {
    TypedQuery<ModelEntity> query = getEntityManager().createNamedQuery("PredictionModel.findByName", ModelEntity.class)
                                                      .setParameter("name", name);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public Status findStatusByNameAndVersion(String name, Long version) {
    TypedQuery<Status> query = getEntityManager().createNamedQuery("PredictionModel.findStatusByNameAndVersion", Status.class)
                                                 .setParameter("name", name)
                                                 .setParameter("version", version);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ModelEntity findLastModel(String name) {
    TypedQuery<ModelEntity> query = getEntityManager().createNamedQuery("PredictionModel.findLastModel", ModelEntity.class)
                                                      .setParameter("name", name)
                                                      .setMaxResults(1);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ModelEntity findFirstModel(String name) {
    TypedQuery<ModelEntity> query = getEntityManager().createNamedQuery("PredictionModel.findFirstModel", ModelEntity.class)
                                                      .setParameter("name", name)
                                                      .setMaxResults(1);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ModelEntity findLastModelWithStatus(String name, Status status) {
    TypedQuery<ModelEntity> query = getEntityManager()
                                                      .createNamedQuery("PredictionModel.findLastModelWithStatus",
                                                                        ModelEntity.class)
                                                      .setParameter("name", name)
                                                      .setParameter("status", status)
                                                      .setMaxResults(1);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ModelEntity findFirstModelWithStatus(String name, Status status) {
    TypedQuery<ModelEntity> query = getEntityManager()
                                                      .createNamedQuery("PredictionModel.findFirstModelWithStatus",
                                                                        ModelEntity.class)
                                                      .setParameter("name", name)
                                                      .setParameter("status", status)
                                                      .setMaxResults(1);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
