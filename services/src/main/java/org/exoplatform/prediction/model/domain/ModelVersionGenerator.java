package org.exoplatform.prediction.model.domain;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import org.exoplatform.commons.persistence.impl.EntityManagerHolder;

public class ModelVersionGenerator implements IdentifierGenerator {

  @Override
  public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
    String name = ((ModelEntity) object).name;
    EntityManager entityManager = EntityManagerHolder.get();

    try {
      TypedQuery<ModelEntity> query = entityManager.createNamedQuery("PredictionModel.findLastModel", ModelEntity.class)
                                            .setParameter("name", name)
                                            .setMaxResults(1);
      return query.getSingleResult().getVersion() + 1;
    } catch (NoResultException e) {
      return 1L;
    }
  }
}
