package org.exoplatform.datacollector.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.datacollector.domain.IdentityProfileEntity;
import org.exoplatform.datacollector.domain.id.IdentityProfileId;

public class IdentityProfileDAO extends GenericDAOJPAImpl<IdentityProfileEntity, IdentityProfileId> {

  public IdentityProfileEntity findById(String id) {
    try {
      TypedQuery<IdentityProfileEntity> query = getEntityManager()
                                                                  .createNamedQuery("IdentityProfile.findById",
                                                                                    IdentityProfileEntity.class)
                                                                  .setParameter("id", id);
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public IdentityProfileEntity findByName(String name) {
    try {
      TypedQuery<IdentityProfileEntity> query = getEntityManager()
                                                                  .createNamedQuery("IdentityProfile.findByName",
                                                                                    IdentityProfileEntity.class)
                                                                  .setParameter("name", name);
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
