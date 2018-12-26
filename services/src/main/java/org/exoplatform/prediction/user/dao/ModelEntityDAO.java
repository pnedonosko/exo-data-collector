package org.exoplatform.prediction.user.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.prediction.user.domain.ModelEntity;
import org.exoplatform.prediction.user.domain.ModelEntity.Status;
import org.exoplatform.prediction.user.domain.ModelId;

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
}
