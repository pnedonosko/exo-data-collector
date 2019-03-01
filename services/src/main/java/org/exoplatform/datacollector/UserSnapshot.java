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
import java.util.stream.Collectors;

import org.exoplatform.social.core.identity.model.Identity;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserSnapshot.java 00000 Jan 30, 2019 pnedonosko $
 */
public class UserSnapshot {

  protected final Identity                   identity;

  protected final Set<String>                connections;

  protected final Map<String, SpaceSnapshot> spaces;

  protected UserInfluencers                  influencers;

  private Set<String>                        favoriteStreams;

  /**
   * Instantiates a new user's snapshot.
   *
   * @param identity the identity
   * @param connections the connections
   * @param spaces the spaces
   */
  protected UserSnapshot(Identity identity, Set<String> connections, Map<String, SpaceSnapshot> spaces) {
    super();
    this.identity = identity;
    this.connections = connections;
    this.spaces = spaces;
  }

  void setFavoriteStreams(Set<String> favoriteStreams) {
    this.favoriteStreams = favoriteStreams;
  }

  UserInfluencers initInfluencers() {
    this.influencers = new UserInfluencers(identity, connections, spaces);
    return this.influencers;
  }

  UserInfluencers initInfluencers(UserInfluencers entity /*
                                                          * TODO
                                                          * UserInfluencersEntity
                                                          * here
                                                          */) {
    // TODO load UserInfluencers from the entity, then set conns and spaces into
    // it
    this.influencers = new UserInfluencers(identity, connections, spaces);

    // influencers' streams and participants collections can be saved in
    // aggregated state - a single weight per item
    // activities - we need save all its fields: id, created (date), posted
    // (bool), lastLiked (date) and lastCommented (date)
    // Load them respectively here (create required setters if required)
    //
    // TODO A problem with persisting and reading weights (streams and
    // participants) from DB - they never will be decreasing, but if calculate
    // from the scratch this problem will not appear. But at this stage we don't
    // handle this problem.
    // As a simplest solution, we would recollect from the
    // scratch periodically (once a month for instance).
    // Another solution (!): introduce weights aging in time and multiply them
    // on decimal coefficient on a next incremental training. Coefficient is
    // dynamic and calculated from a time difference between trainings.

    return this.influencers;
  }

  /**
   * Dump the snapshot to text representation (for development/debug purpose).
   *
   * @return the string with text
   */
  protected String dump() {
    StringBuilder text = new StringBuilder();
    text.append("identity: ").append(identity.getRemoteId()).append(" (").append(identity.getId()).append(")\n");
    text.append("influencers.participants: ")
        .append(influencers.getParticipantsWeight()
                           .entrySet()
                           .stream()
                           .sorted((p1, p2) -> p2.getValue().compareTo(p1.getValue()))
                           .map(pe -> pe.getKey() + "=" + pe.getValue())
                           .collect(Collectors.joining(" ")))
        .append("\n");
    text.append("influencers.favoriteStreams: ")
        .append(influencers.getFavoriteStreams().stream().sorted().collect(Collectors.joining(" ")))
        .append("\n");
    return text.toString();
  }

  /**
   * Gets the connections.
   *
   * @return the connections
   */
  public Set<String> getConnections() {
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
