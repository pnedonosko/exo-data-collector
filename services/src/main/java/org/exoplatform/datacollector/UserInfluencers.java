/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.exoplatform.datacollector.UserInfluencers.ActivityInfo;
import org.exoplatform.datacollector.domain.ActivityCommentedEntity;
import org.exoplatform.datacollector.domain.ActivityLikedEntity;
import org.exoplatform.datacollector.domain.ActivityMentionedEntity;
import org.exoplatform.datacollector.domain.ActivityPostedEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.space.model.Space;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserInfluencers.java 00000 Nov 19, 2018 pnedonosko $
 */
public class UserInfluencers {

  /** Default or uncertain weight constant. */
  public static final double    DEFAULT_WEIGHT    = 0.1;

  private static final int      WEIGHT_PRECISION  = 100000;

  private static final int      MAX_DAYS_RANGE    = 90;

  private static final double   DAY_WEIGHT_GROW   = 0.2;

  private static final double[] DAY_WEIGHTS       = new double[MAX_DAYS_RANGE];

  private static final int      DAY_LENGTH_MILLIS = 86400000;

  static {
    // Index 0 is for first day and so on
    // Each day has weight smaller of the previous on a square of part of the
    // previous one
    DAY_WEIGHTS[0] = 1;
    for (int i = 1; i < DAY_WEIGHTS.length; i++) {
      double prev = DAY_WEIGHTS[i - 1];
      DAY_WEIGHTS[i] = prev - Math.pow(2, prev * DAY_WEIGHT_GROW);
    }
  }

  class SpaceInfo {
    final Space       space;

    final Set<String> members;

    final Set<String> managers;

    SpaceInfo(Space space) {
      super();
      this.space = space;
      this.members = new HashSet<String>(Arrays.asList(space.getMembers()));
      this.managers = new HashSet<String>(Arrays.asList(space.getManagers()));
    }
  }

  class ActivityInfo {
    final String id;

    // final String ownerId;
    // final String posterId;

    final Long         created;

    //Long         updated;

    Long         lastLiked;

    Long         lastCommented;

    ActivityInfo(String id, Long created) {
      this.id = id;
      this.created = created;
    }

    void liked(Long likedTime) {
      if (lastLiked == null || lastLiked < likedTime) {
        lastLiked = likedTime;
      } else {
        lastLiked = likedTime;
      }
    }

    void commented(Long commentTime) {
      if (lastCommented == null || lastCommented < commentTime) {
        lastCommented = commentTime;
      } else {
        lastCommented = commentTime;
      }
    }
  }

  private Map<String, List<Double>> streams      = new HashMap<>();

  private Map<String, List<Double>> participants = new HashMap<>();

  private Map<String, ActivityInfo> activities   = new HashMap<>();

//  private final OrganizationService           organization;
//
//  private final IdentityManager               identityManager;
//
//  private final RelationshipManager           relationshipManager;

//  private final SpaceService                  spaceService;

  private final Identity            userIdentity;

  private Map<String, Identity>     userConnections;

  private Map<String, SpaceInfo>    userSpaces;

  public UserInfluencers(Identity userIdentity, List<Identity> userConnections, List<Space> userSpaces
  /*
   * OrganizationService organization, IdentityManager identityManager,
   * RelationshipManager relationshipManager, SpaceService spaceService
   */) {
    this.userIdentity = userIdentity;
//    this.organization = organization;
//    this.identityManager = identityManager;
//    this.relationshipManager = relationshipManager;
//    this.spaceService = spaceService;

    // Preload some common info
    this.userConnections = userConnections.stream().collect(Collectors.toMap(c -> c.getId(), c -> c));
    this.userSpaces = userSpaces.stream().collect(Collectors.toMap(s -> s.getId(), s -> new SpaceInfo(s)));
  }

  public static double getDateWeight(Date date) {
    long diff = System.currentTimeMillis() - date.getTime();
    Long d = Math.round(Math.floor(diff / DAY_LENGTH_MILLIS));
    int days = d.intValue();
    if (days == 0) {
      return 1;
    } else if (days >= MAX_DAYS_RANGE) {
      return DAY_WEIGHTS[MAX_DAYS_RANGE - 1];
    } else {
      return DAY_WEIGHTS[days];
    }
  }

  /**
   * Decrease given weight relative to given date weight (make lower for older
   * date).
   *
   * @param weight the weight
   * @param date the date
   * @return the double - the adjusted weight
   */
  public static double adjustWeightByDate(double weight, Date date) {
    double dweight = getDateWeight(date);
    return weight * dweight;
  }

  public static double sigmoid(double value) {
    // 1/(1+EXP(-LN(value)*2))
    return 1 / (1 + Math.exp(-2 * Math.log(value)));
  }

  protected void addStream(String id, double weight) {
    streams.computeIfAbsent(id, k -> new ArrayList<Double>()).add(weight);
  }

  public Map<String, Double> getFavoriteStreamsWeight() {
    // TODO consider for caching of the calculated below until a new stream,
    // will be added

    // First get calculated stream weight
    Map<String, Double> weights =
                                streams.entrySet()
                                       .stream()
                                       .collect(Collectors.toMap(e -> e.getKey(),
                                                                 e -> sigmoid(e.getValue()
                                                                               .stream()
                                                                               .collect(Collectors.summingDouble(w -> w.doubleValue())))));
    return weights;
  }

  public double getStreamWeight(String streamId) {
    return getFavoriteStreamsWeight().getOrDefault(streamId, DEFAULT_WEIGHT);
  }

  public Collection<String> getFavoriteStreams() {
    // TODO filter all the streams to only ones that have weight higher of some
    // trendy value (e.g. 0.75)

    // TODO may be collect directly to a collection of keys, w/o transitioning
    // through a map instance - if we don't this map in other places.

    // First get calculated streams weight, then order by weight
    Map<String, Double> weights = getFavoriteStreamsWeight();
    Map<String, Double> ordered = weights.entrySet()
                                         .stream()
                                         .sorted((e1, e2) -> (int) Math.round((e1.getValue() - e2.getValue()) * WEIGHT_PRECISION))
                                         .collect(Collectors.toMap(Map.Entry::getKey,
                                                                   Map.Entry::getValue,
                                                                   (e1, e2) -> e1,
                                                                   LinkedHashMap::new));

    return Collections.unmodifiableCollection(ordered.keySet());
  }

  public Collection<String> getFavoriteStreamsTop(int top) {
    return getFavoriteStreams().stream().limit(top).collect(Collectors.toList());
  }

  protected void addParticipant(String id, double weight) {
    participants.computeIfAbsent(id, k -> new ArrayList<Double>()).add(weight);
  }

  public double getParticipantWeight(String id, String streamId) {
    // TODO consider for caching of the calculated below until a new part item
    // will be added

    List<Double> weights = participants.get(id);
    if (weights != null) {
      weights = new ArrayList<Double>(weights);
    } else {
      weights = new ArrayList<Double>();
    }

    // 1) Add formal weight to the participant found weights: if it's user
    // connection (=0.3), if it's user's space member (=0.3) or manager (=0.5),
    // otherwise (=0.1 or 0.05)
    // TODO
    // 2) Add weights for most "recent" user spaces: where more activities
    // happened last days and user connections participated
    // 3) find most "recent" user connections: those who more active last days
    // and possible have common interest with the user.

    Identity conn = userConnections.get(id);
    if (conn != null) {
      weights.add(0.3);
    }
    SpaceInfo spaceInfo = userSpaces.get(streamId);
    if (spaceInfo != null) {
      if (spaceInfo.managers.contains(id)) {
        weights.add(0.5);
      } else if (spaceInfo.members.contains(id)) {
        weights.add(0.2);
      } else {
        weights.add(DEFAULT_WEIGHT);
      }
    }
    if (conn == null && spaceInfo == null) {
      weights.add(DEFAULT_WEIGHT);
    }

    //
    double weightSum = weights.stream().mapToDouble(w -> w.doubleValue()).sum();
    return sigmoid(weightSum);
  }

  protected void addPost(ActivityPostedEntity p) {
    // Note: AbstractActivityEntity has only a post ID, even if it tells about a
    // comment or like on the post on its comments
    activities.computeIfAbsent(p.getId(), id -> new ActivityInfo(p.getId(), p.getPosted()));
  }
  
  protected void addComment(ActivityCommentedEntity c) {
    // Note: AbstractActivityEntity has only a post ID, even if it tells about a
    // comment or like on the post on its comments
    activities.compute(c.getId(), (id, p) -> {
      if (p == null) {
        p = new ActivityInfo(c.getId(), c.getPosted());
      }
      p.commented(c.getCommentPosted());
      return p;
    });
  }
  
  protected void addMention(ActivityMentionedEntity m) {
    // here it has no difference with posted
    activities.computeIfAbsent(m.getId(), id -> new ActivityInfo(m.getId(), m.getPosted()));
  }
  
  protected void addLike(ActivityLikedEntity l) {
    // Note: AbstractActivityEntity has only a post ID, even if it tells about a
    // comment or like on the post on its comments
    activities.compute(l.getId(), (id, p) -> {
      if (p == null) {
        p = new ActivityInfo(l.getId(), l.getPosted());
      }
      p.liked(l.getLiked());
      return p;
    });
  }

  // *********************************

  public void addCommentedPoster(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      // TODO We can adjust relatively create and update (liked/answered) dates
      // - the update one looks as more relevant
      // TODO also consider for adjusting the weight if commented/posted too far
      // from updated (is it less relevant?)
      // TODO may be we need different adjustment for users and streams?
      // double w = adjustWeightByDate(1, c.getCommentUpdatedDate());
      double w = 1;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
    }
  }

  public void addCommentedCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      // double w = adjustWeightByDate(0.9, c.getCommentUpdatedDate());
      double w = 0.9;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
    }
  }

  public void addCommentedConvoPoster(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      double w = 0.8;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
    }
  }

  public void addPostCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      double w = 1;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
    }
  }

  public void addCommentCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      double w = 0.9;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
      addComment(c);
    }
  }

  public void addConvoCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      // addParticipant(c.getPosterId(), adjustWeightByDate(0.8,
      // c.getCommentUpdatedDate()));
      double w = 0.8;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
      addComment(c);
    }
  }

  public void addMentioner(List<ActivityMentionedEntity> mentioned) {
    for (ActivityMentionedEntity m : mentioned) {
      // addParticipant(m.getPosterId(), adjustWeightByDate(0.9,
      // m.getUpdatedDate()));
      double w = 0.8;
      addParticipant(m.getPosterId(), w);
      addStream(m.getOwnerId(), w);
    }
  }

  public void addMentioned(List<ActivityMentionedEntity> mentioned) {
    for (ActivityMentionedEntity m : mentioned) {
      // addParticipant(m.getMentionedId(), adjustWeightByDate(0.8,
      // m.getUpdatedDate()));
      double w = 0.8;
      addParticipant(m.getMentionedId(), w);
      addStream(m.getOwnerId(), w);
    }
  }

  public void addLikedPoster(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.8,
      // l.getLikedDate()));
      double w = 0.8;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addLikedCommenter(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.7,
      // l.getLikedDate()));
      double w = 0.7;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addLikedConvoPoster(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.5,
      // l.getLikedDate()));
      double w = 0.5;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addPostLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.4,
      // l.getLikedDate()));
      double w = 0.4;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addCommentLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.4,
      // l.getLikedDate()));
      double w = 0.4;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addConvoLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.3,
      // l.getLikedDate()));
      double w = 0.3;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addSamePostLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.7,
      // l.getLikedDate()));
      double w = 0.7;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addSameCommentLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      double w = 0.7;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addSameConvoLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      double w = 0.3;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addStreamPoster(List<ActivityPostedEntity> posted) {
    for (ActivityPostedEntity p : posted) {
      double w = 0.6;
      addParticipant(p.getPosterId(), w);
      addStream(p.getOwnerId(), w);
    }
  }

  public void addStreamCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      double w = 0.5;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
    }
  }

  public void addStreamPostLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      double w = 0.4;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

  public void addStreamCommentLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      double w = 0.3;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
    }
  }

}
