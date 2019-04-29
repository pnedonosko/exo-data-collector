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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserSnapshot.java 00000 Jan 30, 2019 pnedonosko $
 */
public class UserSnapshot {

  private static final Log LOG = ExoLogger.getExoLogger(UserSnapshot.class);

  /**
   * Content exception tells that this class data cannot be read from serialized
   * content.
   */
  class ContentException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = -2627878168262293704L;

    /**
     * Instantiates a new content exception.
     *
     * @param message the message
     */
    ContentException(String message) {
      super(message);
    }
  }

  class InfluencersSnapshot extends SocialInfluencers implements JSONAware {

    InfluencersSnapshot(Set<String> connections, Map<String, SpaceSnapshot> spaces) {
      super(connections, spaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toJSONString() {
      return toJson().toJSONString();
    }

    @SuppressWarnings("unchecked")
    JSONObject toJson() {
      JSONObject root = new JSONObject();
      // streams
      root.put("streams", streams);
      // participants
      root.put("participants", participants);
      // activities
      JSONObject activitiesMap = new JSONObject();
      for (Map.Entry<String, ActivityInfo> ae : activities.entrySet()) {
        JSONObject activityMap = new JSONObject();
        // activityMap.put("id", ae.getValue().id);
        activityMap.put("created", ae.getValue().created);
        activityMap.put("lastCommented", ae.getValue().lastCommented);
        activityMap.put("lastLiked", ae.getValue().lastLiked);
        activityMap.put("posted", ae.getValue().posted);
        activitiesMap.put(ae.getKey(), activityMap);
      }
      root.put("activities", activitiesMap);
      return root;
    }

    void fromJson(JSONObject root) throws ContentException {
      // streams
      Map<String, List<Double>> streams = readMap(root.get("streams"), (streamId, weightsArray) -> {
        return readArray(weightsArray, () -> new ArrayList<Double>());
      }, () -> new HashMap<String, List<Double>>());
      // participants
      Map<String, List<Double>> participants = readMap(root.get("participants"), (partId, weightsArray) -> {
        return readArray(weightsArray, () -> new ArrayList<Double>());
      }, () -> new HashMap<String, List<Double>>());
      // activities
      Map<String, ActivityInfo> activities = readMap(root.get("activities"), (activityId, activityObj) -> {
        if (activityObj != null && JSONObject.class.isAssignableFrom(activityObj.getClass())) {
          JSONObject activityMap = JSONObject.class.cast(activityObj);
          // String id = castAs(activityMap.get("id"), String.class);
          Long created = castAs(activityMap.get("created"), Long.class);
          Long lastCommented = castAs(activityMap.get("lastCommented"), Long.class);
          Long lastLiked = castAs(activityMap.get("lastLiked"), Long.class);
          Boolean posted = castAs(activityMap.get("posted"), Boolean.class);
          if (created != null && posted != null) {
            return new ActivityInfo(activityId, created, lastCommented, lastLiked, posted);
          } else {
            LOG.warn("Bad content of saved activity {}, created: {}, posted: {}", activityId, created, posted);
          }
        } else {
          LOG.warn("Unexpected saved object format of activityId {}", activityId, activityObj);
        }
        return null;
      }, () -> new HashMap<String, ActivityInfo>());

      // Apply atomically - all or nothing (code will not reach here if
      // something missing or failed)
      this.streams = streams;
      this.participants = participants;
      this.activities = activities;
    }
  }

  private final Identity             identity;

  private Set<String>                connections;

  private Map<String, SpaceSnapshot> spaces;

  private InfluencersSnapshot        influencers;

  private Set<String>                favoriteStreams;

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

  UserSnapshot(Identity identity) {
    super();
    this.identity = identity;
  }

  void setFavoriteStreams(Set<String> favoriteStreams) {
    this.favoriteStreams = favoriteStreams;
  }

  SocialInfluencers initInfluencers() {
    this.influencers = new InfluencersSnapshot(connections, spaces);
    return this.influencers;
  }

  SocialInfluencers loadInfluencers(SocialInfluencers entity /*
                                                              * TODO
                                                              * UserInfluencersEntity
                                                              * here
                                                              */) {
    // TODO load SocialInfluencers from the entity, then set conns and spaces
    // into it
    this.influencers = new InfluencersSnapshot(connections, spaces);

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
  public SocialInfluencers getInfluencers() {
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

  @SuppressWarnings("unchecked")
  public String toJson() {
    JSONObject root = new JSONObject();
    // connections
    root.put("connections", new ArrayList<>(connections));
    // spaces
    JSONObject spacesMap = new JSONObject();
    for (Map.Entry<String, SpaceSnapshot> se : spaces.entrySet()) {
      JSONObject spaceMap = new JSONObject();
      spaceMap.put("managers", new ArrayList<>(se.getValue().getManagers()));
      spaceMap.put("members", new ArrayList<>(se.getValue().getMembers()));
      spacesMap.put(se.getKey(), spaceMap);
    }
    root.put("spaces", spacesMap);
    root.put("influencers", influencers);
    return root.toJSONString();
  }

  public void fromJson(String json) throws SnapshotException {
    JSONParser parser = new JSONParser();
    try {
      Object obj = parser.parse(json);
      if (obj != null && JSONObject.class.isAssignableFrom(obj.getClass())) {
        JSONObject root = JSONObject.class.cast(obj);
        try {
          // connections
          Set<String> connections = Collections.unmodifiableSet(readArray(root.get("connections"), () -> new HashSet<String>()));
          // spaces
          Map<String, SpaceSnapshot> spaces = Collections.unmodifiableMap(readMap(root.get("spaces"), (spaceId, spaceObj) -> {
            if (spaceObj != null && JSONObject.class.isAssignableFrom(spaceObj.getClass())) {
              JSONObject spaceMap = JSONObject.class.cast(spaceObj);
              List<String> managers = readArray(spaceMap.get("managers"), () -> new ArrayList<String>());
              List<String> members = readArray(spaceMap.get("members"), () -> new ArrayList<String>());
              if (managers != null && !managers.isEmpty() && members != null) {
                return new SpaceSnapshot(members, managers);
              } else {
                LOG.warn("Bad content of saved space {}, managers: {}, members: {}",
                         spaceId,
                         managers.stream().collect(Collectors.joining(" ")),
                         members.stream().collect(Collectors.joining(" ")));
              }
            } else {
              LOG.warn("Unexpected saved object format of spaceId {}", spaceId, spaceObj);
            }
            return null;
          }, () -> new HashMap<String, SpaceSnapshot>()));
          // influencers
          obj = root.get("influencers");
          if (obj != null && JSONObject.class.isAssignableFrom(obj.getClass())) {
            JSONObject influencersObj = JSONObject.class.cast(obj);
            InfluencersSnapshot influencers = new InfluencersSnapshot(connections, spaces);
            influencers.fromJson(influencersObj);
            // Atomically apply
            this.connections = connections;
            this.spaces = spaces;
            this.influencers = influencers;
          } else {
            throw new SnapshotException("Unexpected influencers object format: " + obj);
          }
        } catch (ContentException e) {
          throw new SnapshotException("Bad content of saved snapshot object: " + root, e);
        }
      } else {
        throw new SnapshotException("Unexpected snapshot object format: " + obj);
      }
    } catch (ParseException e) {
      throw new SnapshotException("Error reading snapshot JSON", e);
    }
  }

  // ******* internals *********

  @SuppressWarnings("unchecked")
  private <E extends Object, C extends Collection<E>> C readArray(Object obj, Supplier<C> out) throws ContentException {
    if (obj != null && JSONArray.class.isAssignableFrom(obj.getClass())) {
      C dest = out.get();
      JSONArray array = JSONArray.class.cast(obj);
      Iterator<E> iterator = array.iterator();
      while (iterator.hasNext()) {
        dest.add(iterator.next());
      }
      return dest;
    }
    throw new ContentException("Cannot read object as array: " + obj);
  }

  @SuppressWarnings("unchecked")
  private <E extends Object, S extends Object, M extends Map<String, E>> M readMap(Object obj,
                                                                                   BiFunction<String, S, E> reader,
                                                                                   Supplier<M> out) throws ContentException {
    if (obj != null && JSONObject.class.isAssignableFrom(obj.getClass())) {
      M dest = out.get();
      JSONObject object = JSONObject.class.cast(obj);
      object.forEach((k, v) -> {
        if (String.class.isAssignableFrom(k.getClass()) && v != null) {
          String key = String.class.cast(k);
          try {
            E element = reader.apply(key, (S) v);
            if (dest != null && element != null) {
              dest.put(key, element);
            }
          } catch (ClassCastException e) {
            LOG.warn("Cannot read saved key {} value {}", key, v, e);
          }
        } else {
          LOG.warn("Cannot read saved key {} or its value is null", k);
        }
      });
      return dest;
    }
    throw new ContentException("Cannot read object: " + obj);
  }

  private <C extends Object> C castAs(Object obj, Class<C> clazz) {
    return obj != null && clazz.isAssignableFrom(obj.getClass()) ? clazz.cast(obj) : null;
  }

}
