package org.exoplatform.prediction.model.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.prediction.model.domain.ModelEntity;
import org.exoplatform.prediction.model.domain.ModelId;
import org.exoplatform.prediction.model.domain.ModelEntity.Status;

public class ModelEntityDAO extends GenericDAOJPAImpl<ModelEntity, ModelId> {

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

  public Long findLastModelVersion(String name) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("PredictionModel.findLastModelVersion", Long.class)
                                               .setParameter("name", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
