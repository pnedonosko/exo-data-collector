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
package org.exoplatform.datacollector;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.exoplatform.social.core.identity.model.Identity;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserSnapshot.java 00000 Jan 30, 2019 pnedonosko $
 */
public class UserSnapshot {
  protected final UserInfluencers            influencers;

  protected final Identity                   identity;

  protected final Map<String, Identity>      connections;

  protected final Map<String, SpaceSnapshot> spaces;

  private Set<String>                        favoriteStreams;

  /**
   * Instantiates a new user snapshot.
   *
   * @param influencers the influencers
   * @param identity the identity
   * @param connections the connections
   * @param spaces the spaces
   */
  protected UserSnapshot(UserInfluencers influencers,
                         Identity identity,
                         Map<String, Identity> connections,
                         Map<String, SpaceSnapshot> spaces) {
    super();
    this.influencers = influencers;
    this.identity = identity;
    this.connections = connections;
    this.spaces = spaces;
  }

  void setFavoriteStreams(Set<String> favoriteStreams) {
    this.favoriteStreams = favoriteStreams;
  }

  /**
   * Gets the connections.
   *
   * @return the connections
   */
  public Map<String, Identity> getConnections() {
    return connections;
  }

  /**
   * Gets the spaces.
   *
   * @return the spaces
   */
  public Map<String, SpaceSnapshot> getSpaces() {
    return spaces;
  }

  /**
   * Gets the identity.
   *
   * @return the identity
   */
  public Identity getIdentity() {
    return identity;
  }

  /**
   * Gets the influencers.
   *
   * @return the influencers
   */
  public UserInfluencers getInfluencers() {
    return influencers;
  }

  /**
   * Gets the favorite streams.
   *
   * @return the favoriteStreams
   */
  public Set<String> getFavoriteStreams() {
    return favoriteStreams != null ? favoriteStreams : Collections.emptySet();
  }

}
