/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.datacollector.social;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.picocontainer.Startable;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.jpa.storage.dao.ConnectionDAO;
import org.exoplatform.social.core.jpa.storage.entity.ConnectionEntity;
import org.exoplatform.social.core.relationship.model.Relationship;

/**
 * Custom access to Social storage using its RDMBS storage DAOs.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SocialStorage.java 00000 Feb 15, 2019 pnedonosko $
 */
public class SocialStorage implements Startable {

  protected final ConnectionDAO connsStorage;

  /**
   * Instantiates a new social identity storage.
   *
   * @param connsStorage the conns storage
   */
  public SocialStorage(ConnectionDAO connsStorage) {
    this.connsStorage = connsStorage;
  }

  /**
   * Gets the connections count.
   *
   * @param id the id
   * @return the connections count
   */
  public int getConnectionsCount(Identity id) {
    return this.connsStorage.getConnectionsCount(id, Relationship.Type.CONFIRMED);
  }

  /**
   * Gets the connections.
   *
   * @param id the id
   * @return the connections set
   */
  public Set<String> getConnections(Identity id) {
    // Usage grabbed from RDBMSRelationshipStorageImpl.getConnections(id)
    List<ConnectionEntity> conns = this.connsStorage.getConnections(id, Relationship.Type.CONFIRMED, 0, -1);
    Set<String> connIds = conns.stream().map(ce -> {
      String cid;
      if (id.getId().equals(cid = ce.getSender().getStringId())) {
        return ce.getReceiver().getStringId();
      } else {
        return cid;
      }
    }).collect(Collectors.toSet());
    return connIds;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // TODO something?
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // TODO something?
  }

}
