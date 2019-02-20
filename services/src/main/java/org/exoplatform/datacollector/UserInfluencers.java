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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.exoplatform.datacollector.domain.ActivityCommentedEntity;
import org.exoplatform.datacollector.domain.ActivityLikedEntity;
import org.exoplatform.datacollector.domain.ActivityMentionedEntity;
import org.exoplatform.datacollector.domain.ActivityPostedEntity;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * User influencers snapshot. Data calculated here should not be modified since
 * once collected (by
 * {@link SocialDataCollectorService#collectUserActivities(org.exoplatform.datacollector.identity.UserIdentity, java.io.PrintWriter)}).<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserInfluencers.java 00000 Nov 19, 2018 pnedonosko $
 */
public class UserInfluencers {

  /** The Logger. */
  private static final Log      LOG                        = ExoLogger.getExoLogger(UserInfluencers.class);

  public static final long      DAY_LENGTH_MILLIS          = 86400000;

  /** Default or uncertain weight constant. */
  public static final double    DEFAULT_WEIGHT             = 0.1;

  /**
   * The Constant WIDELY_LIKES_MAX - 30, number taken from observations at eXo
   * Tribe.
   */
  public static final int       WIDELY_LIKES_MAX           = 30;

  public static final int       WEIGHT_PRECISION           = 100000;

  public static final int       REACTIVITY_DAYS_RANGE      = 30;

  public static final int       INFLUENCE_DAYS_RANGE       = 90;

  /** The Constant FEED_DAYS_RANGE = 2 years long. */
  public static final int       FEED_DAYS_RANGE            = 731;

  public static final long      FEED_MILLIS_RANGE          = FEED_DAYS_RANGE * DAY_LENGTH_MILLIS;

  public static final double    REACTIVITY_DAY_WEIGHT_GROW = 0.7;

  public static final double    INFLUENCE_DAY_WEIGHT_GROW  = 0.2;

  public static final int       ACTIVITY_PARTICIPANTS_TOP  = 5;

  /** The Constant ACTIVITY_EXPIRATION_L0 (30 min). */
  public static final long      ACTIVITY_EXPIRATION_L0     = 1000 * 60 * 30;

  /** The Constant ACTIVITY_EXPIRATION_L1 (2 hours). */
  public static final long      ACTIVITY_EXPIRATION_L1     = 1000 * 60 * 60 * 2;

  private static final double[] REACTIVITY_WEIGHTS         = new double[REACTIVITY_DAYS_RANGE];

  private static final double[] INFLUENCE_WEIGHTS          = new double[INFLUENCE_DAYS_RANGE];

  static {
    // Index 0 is for first day and so on
    // Each day has weight smaller of the previous on a square of part of the
    // previous one
    // Influence table:
    INFLUENCE_WEIGHTS[0] = 1;
    for (int i = 1; i < INFLUENCE_WEIGHTS.length; i++) {
      double prev = INFLUENCE_WEIGHTS[i - 1];
      INFLUENCE_WEIGHTS[i] = prev - Math.pow(prev * INFLUENCE_DAY_WEIGHT_GROW, 2);
    }
    // Reactivity table (same formula as for influence for beginning):
    REACTIVITY_WEIGHTS[0] = 1;
    for (int i = 1; i < REACTIVITY_WEIGHTS.length; i++) {
      double prev = REACTIVITY_WEIGHTS[i - 1];
      REACTIVITY_WEIGHTS[i] = prev - Math.pow(prev * REACTIVITY_DAY_WEIGHT_GROW, 2);
    }
  }

  class ActivityInfo {
    final String id;

    final Long   created;

    Boolean      posted = Boolean.FALSE;

    Long         lastLiked;

    Long         lastCommented;

    ActivityInfo(String id, Long created) {
      this.id = id;
      this.created = created;
    }

    /**
     * Reactivity encoded as difference in days between a day of posted and a
     * day when user commented/liked falling logarithmically: 0..1, where 1 is
     * same day, 0 is 30+ days old.
     *
     * @return the double
     */
    double reactivity() {
      Long lastActive = posted ? created : (lastCommented != null ? lastCommented : (lastLiked != null ? lastLiked : null));
      if (lastActive != null) {
        return getDateReactivityWeight(created, lastActive);
      }
      return 0d;
    }

    void posted() {
      posted = true;
    }

    void liked(Long likedTime) {
      if (lastLiked == null || lastLiked < likedTime) {
        lastLiked = likedTime;
      }
    }

    void commented(Long commentTime) {
      if (lastCommented == null || lastCommented < commentTime) {
        lastCommented = commentTime;
      }
    }
  }

  private Map<String, List<Double>>        streams      = new HashMap<>();

  private Map<String, List<Double>>        participants = new HashMap<>();

  private Map<String, ActivityInfo>        activities   = new HashMap<>();

  @Deprecated // TODO not used
  private final Identity                   identity;

  private transient final Set<String>      connections;

  private transient final Map<String, SpaceSnapshot> spaces;

  /**
   * Instantiates a new user influencers.
   *
   * @param identity the user identity
   * @param connections the user connections
   * @param spaces the user spaces
   */
  public UserInfluencers(Identity identity, Set<String> connections, Map<String, SpaceSnapshot> spaces) {
    this.identity = identity;
    this.connections = connections;
    this.spaces = spaces;
  }

  public boolean isWidelyLiked(int likes) {
    return likes >= WIDELY_LIKES_MAX;
  }

  private static double dayWeight(double[] table, long from, long to, boolean olderIsZero) {
    long diff = to - from;
    int day = Long.valueOf(Math.round(Math.floor(diff / DAY_LENGTH_MILLIS))).intValue();
    if (day == 0) {
      return 1d;
    } else if (day >= table.length) {
      return olderIsZero ? 0d : table[table.length - 1];
    } else if (day < 0) {
      LOG.warn("Negative day found: " + day + ". Return zero weight for it.");
      return 0d;
    } else {
      return table[day];
    }
  }

  public static double getDateInfluenceWeight(long date) {
    return dayWeight(INFLUENCE_WEIGHTS, date, System.currentTimeMillis(), false);
  }

  public static double getDateReactivityWeight(long from, long to) {
    return dayWeight(REACTIVITY_WEIGHTS, from, to, true);
  }

  /**
   * Decrease given weight relative to given date weight (make lower for older
   * date).
   *
   * @param weight the weight
   * @param date the date in ms
   * @return the double - the adjusted weight
   */
  public static double adjustWeightByDate(double weight, long date) {
    double dweight = getDateInfluenceWeight(date);
    return weight * dweight;
  }

  public static double round(double value, double precision) {
    return Math.round(value * precision) / precision;
  }

  public static double sigmoid(double value) {
    // 1/(1+EXP(-LN(value)*2)) TODO *1.5 may produce a bit smaller values
    if (value != 0) {
      return round(1 / (1 + Math.exp(-1 * Math.log(value))), WEIGHT_PRECISION);
    }
    return 0d;
  }

  protected void addStream(String id, double weight) {
    streams.computeIfAbsent(id, k -> new ArrayList<Double>()).add(weight);
  }

  private Map<String, Double> aggregateWeight(Map<String, List<Double>> mapOfWeights) {
    return mapOfWeights.entrySet()
                       .stream()
                       .collect(Collectors.toMap(e -> e.getKey(),
                                                 e -> sigmoid(e.getValue()
                                                               .stream()
                                                               .collect(Collectors.summingDouble(w -> w.doubleValue())))));
  }

  private Map<String, Double> orderWeights(Map<String, Double> weights) {
    return weights.entrySet()
                  .stream()
                  .sorted((e1, e2) -> (int) Math.round((e2.getValue() - e1.getValue()) * WEIGHT_PRECISION))
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  public Map<String, Double> getFavoriteStreamsWeight() {
    // TODO consider for caching of the calculated below until a new stream,
    // will be added

    // First get calculated stream weight
    Map<String, Double> weights = aggregateWeight(streams);
    return weights;
  }

  public double getStreamWeight(String streamId) {
    return getFavoriteStreamsWeight().getOrDefault(streamId, DEFAULT_WEIGHT);
  }

  /**
   * Gets the favorite streams, first most favorite and lesser later.
   *
   * @return the favorite streams ordered in descending order of user favor
   */
  public Set<String> getFavoriteStreams() {
    // TODO filter all the streams to only ones that have weight higher of some
    // trendy value (e.g. 0.75)

    // TODO may be collect directly to a collection of keys, w/o transitioning
    // through a map instance - if we don't this map in other places.

    // First get calculated streams weight, then order by weight
    Map<String, Double> weights = getFavoriteStreamsWeight();
    Map<String, Double> ordered = orderWeights(weights);

    // TODO test it to ensure order
    return Collections.unmodifiableSet(ordered.keySet());
  }

  public Set<String> getFavoriteStreamsTop(int top) {
    // TODO test it to ensure order
    return getFavoriteStreams().stream().limit(top).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public boolean isFavoriteStreams(String streamId) {
    return streams.containsKey(streamId);
  }

  public boolean isParticipant(String partId) {
    return participants.containsKey(partId);
  }

  public Set<String> getAllParticipants() {
    return Collections.unmodifiableSet(participants.keySet());
  }

  protected void addParticipant(String id, double weight) {
    // TODO we may want to avoid adding same activity part several times via
    // different queries, like as commenter and favorite stream commenter, same
    // for commenters and likers.
    participants.computeIfAbsent(id, k -> new ArrayList<Double>()).add(weight);
  }

  public Map<String, Double> getParticipantsWeight() {
    // TODO consider for caching of the calculated below until a new part, will
    // be added

    // First get calculated part weight
    Map<String, Double> weights = aggregateWeight(participants);
    return weights;
  }

  public Set<String> getParticipants() {
    // TODO filter all the parts to only ones that have weight higher of some
    // trendy value (e.g. 0.75)

    // TODO may be collect directly to a collection of keys, w/o transitioning
    // through a map instance - if we don't this map in other places.

    // First get calculated parts weight, then order by weight
    Map<String, Double> weights = getParticipantsWeight();
    Map<String, Double> ordered = orderWeights(weights);

    // TODO test it to ensure order
    return Collections.unmodifiableSet(ordered.keySet());
  }

  public Set<String> getParticipantsTop(int top) {
    // TODO test it to ensure order
    return getParticipants().stream().limit(top).collect(Collectors.toCollection(LinkedHashSet::new));
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

    boolean isConn = connections.contains(id);
    if (isConn) {
      weights.add(0.3);
    }
    SpaceSnapshot spaceInfo = null;
    if (streamId != null) {
      spaceInfo = spaces.get(streamId);
      if (spaceInfo != null) {
        if (spaceInfo.managers.contains(id)) {
          weights.add(0.5);
        } else if (spaceInfo.members.contains(id)) {
          weights.add(0.2);
        } else {
          weights.add(DEFAULT_WEIGHT);
        }
      }
    }
    if (!isConn && spaceInfo == null) {
      weights.add(DEFAULT_WEIGHT);
    }

    //
    double weightSum = weights.stream().mapToDouble(w -> w.doubleValue()).sum();
    return sigmoid(weightSum);
  }

  public double getPostReactivity(String postId) {
    ActivityInfo a = activities.get(postId);
    return a != null ? a.reactivity() : 0d;
  }

  protected ActivityInfo addPost(ActivityPostedEntity p) {
    // Note: AbstractActivityEntity has only a post ID, even if it tells about a
    // comment or like on the post or its comments.
    return activities.computeIfAbsent(p.getId(), id -> new ActivityInfo(p.getId(), p.getPosted()));
  }

  protected ActivityInfo addComment(ActivityCommentedEntity c) {
    // Note: AbstractActivityEntity has only a post ID, even if it tells about a
    // comment or like on the post on its comments
    return activities.compute(c.getId(), (id, p) -> {
      if (p == null) {
        p = new ActivityInfo(c.getId(), c.getPosted());
      }
      p.commented(c.getCommentPosted());
      return p;
    });
  }

  protected ActivityInfo addMention(ActivityMentionedEntity m) {
    // here it has no difference with posted
    return activities.computeIfAbsent(m.getId(), id -> new ActivityInfo(m.getId(), m.getPosted()));
  }

  protected ActivityInfo addLike(ActivityLikedEntity l) {
    // Note: AbstractActivityEntity has only a post ID, even if it tells about a
    // comment or like on the post on its comments
    return activities.compute(l.getId(), (id, p) -> {
      if (p == null) {
        p = new ActivityInfo(l.getId(), l.getPosted());
      }
      p.liked(l.getLiked());
      return p;
    });
  }

  // *********************************

  public void addUserPosts(List<ActivityPostedEntity> posted) {
    for (ActivityPostedEntity p : posted) {
      double w = 1.0;
      // We don't add participants here, if any found they'll be added via
      // addCommented/liked/mentioned etc.
      // addParticipant(p.getPosterId(), w);
      addStream(p.getOwnerId(), w);
      addPost(p).posted(); // mark as posted by the user
    }
  }

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
      addComment(c);
    }
  }

  public void addCommentedCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      // double w = adjustWeightByDate(0.9, c.getCommentUpdatedDate());
      double w = 0.9;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
      addComment(c);
    }
  }

  public void addCommentedConvoPoster(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      double w = 0.8;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
      addComment(c);
    }
  }

  public void addPostCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      double w = 1;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
      addComment(c);
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

  public void addMentioner(List<ActivityMentionedEntity> mentioner) {
    for (ActivityMentionedEntity m : mentioner) {
      // addParticipant(m.getPosterId(), adjustWeightByDate(0.9,
      // m.getUpdatedDate()));
      double w = 0.8;
      // TODO similar situation as in addMentioned(), but opposite.
      // Current user mentioned in post comment of other users, will appear
      // twice:
      // 1) first time within the post activity where comment mentions it
      // 2) second time within the comments itself, if several comments do
      // mention, then this will happen for each of them.
      // To figure out was the user mentioned in the post message or its
      // comments we need see in the texts as DB table soc_mentions duplicates
      // mentions for post and its comment activities.
      // FYI part it's who posted with the mention: can be post author or
      // commenter
      addParticipant(m.getPosterId(), w);
      addStream(m.getOwnerId(), w);
      addMention(m);
    }
  }

  public void addMentioned(List<ActivityMentionedEntity> mentioned) {
    for (ActivityMentionedEntity m : mentioned) {
      // addParticipant(m.getMentionedId(), adjustWeightByDate(0.8,
      // m.getUpdatedDate()));
      double w = 0.8;
      // TODO May be we need improve logic for following (get rid of #1):
      // In a post of current user, a participant once mentioned in a comment to
      // the post, will appear twice for that post:
      // 1) first time within the post activity where comment mentions it
      // 2) second time within the comments itself, if several comments do
      // mention, then this will happen for each of them.
      addParticipant(m.getMentionedId(), w);
      addStream(m.getOwnerId(), w);
      addMention(m);
    }
  }

  public void addLikedPoster(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.8,
      // l.getLikedDate()));
      double w = 0.8;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addLikedCommenter(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.7,
      // l.getLikedDate()));
      double w = 0.7;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addLikedConvoPoster(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.5,
      // l.getLikedDate()));
      double w = 0.5;
      addParticipant(l.getPosterId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addPostLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.4,
      // l.getLikedDate()));
      double w = 0.4;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addCommentLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.4,
      // l.getLikedDate()));
      double w = 0.4;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addConvoLiker(List<ActivityLikedEntity> liked) {
    // TODO here we may find more complex relations of liking the post because
    // of comments/likes by others, and build more robust weight for the liker:
    // e.g. follow replies to the liker comments or mentions of him
    // TODO to avoid duplicates of post likers via direct comments and comments
    // on comments, maintain a set of "postID - likerID" - if item exists,
    // then don't count it again.
    // Similar problem in addSameConvoLiker()
    // XXX attempted to fix this by SQL SELECT DISTINCT
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.3,
      // l.getLikedDate()));
      double w = 0.3;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addSamePostLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // addParticipant(l.getPosterId(), adjustWeightByDate(0.7,
      // l.getLikedDate()));
      double w = 0.7;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addSameCommentLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      double w = 0.7;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addSameConvoLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      // TODO to avoid duplicates of posters via direct comments and
      // comments on comments, maintain a set of "postID - posterID" - if item
      // exists, then don't count it again.
      // Similar problem in addConvoLiker()
      // XXX attempted to fix this by SQL SELECT DISTINCT
      double w = 0.3;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addStreamPoster(List<ActivityPostedEntity> posted) {
    for (ActivityPostedEntity p : posted) {
      double w = 0.6;
      addParticipant(p.getPosterId(), w);
      addStream(p.getOwnerId(), w);
      addPost(p);
    }
  }

  public void addStreamCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      double w = 0.5;
      addParticipant(c.getPosterId(), w);
      addStream(c.getOwnerId(), w);
      addComment(c);
    }
  }

  public void addStreamPostLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      double w = 0.4;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

  public void addStreamCommentLiker(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      double w = 0.3;
      addParticipant(l.getLikerId(), w);
      addStream(l.getOwnerId(), w);
      addLike(l);
    }
  }

}
