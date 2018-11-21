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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.datacollector.domain.AbstractActivityEntity;
import org.exoplatform.datacollector.domain.ActivityCommentedEntity;
import org.exoplatform.datacollector.domain.ActivityLikedEntity;
import org.exoplatform.datacollector.domain.ActivityMentionedEntity;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserInfluencers.java 00000 Nov 19, 2018 pnedonosko $
 */
public class UserInfluencers {

  private static int      MAX_DAYS_RANGE    = 30;

  private static double   DAY_WEIGHT_GROW   = 0.25;

  private static double[] DAY_WEIGHTS       = new double[MAX_DAYS_RANGE];

  private static long     DAY_LENGTH_MILLIS = 86400000;

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

  private Map<String, List<Double>>           spaces       = new HashMap<>();

  private Map<String, List<Double>>           participants = new HashMap<>();

  private Map<String, AbstractActivityEntity> activities   = new HashMap<>();

  public static double getDateWeight(Date date) {
    long diff = System.currentTimeMillis() - date.getTime();
    Long d = Math.round(Math.floor(diff / DAY_LENGTH_MILLIS));
    int days = d.intValue();
    if (days > MAX_DAYS_RANGE) {
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

  public void addStream(String id, double weight) {
    spaces.computeIfAbsent(id, k -> new ArrayList<Double>()).add(weight);
  }

  public void addParticipant(String id, double weight) {
    participants.computeIfAbsent(id, k -> new ArrayList<Double>()).add(weight);
  }

  public void addCommentedPoster(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      // We can adjust relatively create and update (liked/answered) dates - the
      // update one looks as more relevant
      // TODO also consider for adjusting the weight if commented/posted too far
      // from updated (is it less relevant?)
      addParticipant(c.getPosterId(), adjustWeightByDate(1, c.getCommentUpdatedDate()));
    }
  }

  public void addCommentedCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      addParticipant(c.getPosterId(), adjustWeightByDate(0.9, c.getCommentUpdatedDate()));
    }
  }

  public void addCommentedConvoPoster(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      addParticipant(c.getPosterId(), adjustWeightByDate(0.8, c.getCommentUpdatedDate()));
    }
  }

  public void addPostCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      addParticipant(c.getPosterId(), adjustWeightByDate(1, c.getCommentUpdatedDate()));
    }
  }

  public void addCommentCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      addParticipant(c.getPosterId(), adjustWeightByDate(0.9, c.getCommentUpdatedDate()));
    }
  }

  public void addConvoCommenter(List<ActivityCommentedEntity> commented) {
    for (ActivityCommentedEntity c : commented) {
      addParticipant(c.getPosterId(), adjustWeightByDate(0.8, c.getCommentUpdatedDate()));
    }
  }

  public void addMentioner(List<ActivityMentionedEntity> mentioned) {
    for (ActivityMentionedEntity m : mentioned) {
      addParticipant(m.getPosterId(), adjustWeightByDate(0.9, m.getUpdatedDate()));
    }
  }

  public void addMentioned(List<ActivityMentionedEntity> mentioned) {
    for (ActivityMentionedEntity m : mentioned) {
      addParticipant(m.getMentionedId(), adjustWeightByDate(0.8, m.getUpdatedDate()));
    }
  }

  public void addLikedPoster(List<ActivityLikedEntity> liked) {
    for (ActivityLikedEntity l : liked) {
      addParticipant(l.getPosterId(), adjustWeightByDate(0.8, l.getUpdatedDate()));
    }
  }

  public void addLikedCommenter(String id) {
    addParticipant(id, 0.7);
  }

  public void addLikedConvoPoster(String id) {
    addParticipant(id, 0.5);
  }

  public void addPostLiker(String id) {
    addParticipant(id, 0.4);
  }

  public void addCommentLiker(String id) {
    addParticipant(id, 0.4);
  }

  public void addConvoLiker(String id) {
    addParticipant(id, 0.3);
  }

  public void addSamePostLiker(String id) {
    addParticipant(id, 0.7);
  }

  public void addSameCommentLiker(String id) {
    addParticipant(id, 0.7);
  }

  public void addSameConvoLiker(String id) {
    addParticipant(id, 0.2);
  }

  public void addStreamPoster(String streamId, String id) {
    // TODO Take in account the stream influence
    addParticipant(id, 0.6);
  }

  public void addStreamCommenter(String streamId, String id) {
    // TODO Take in account the stream influence
    addParticipant(id, 0.5);
  }

  public void addStreamPostLiker(String streamId, String id) {
    // TODO Take in account the stream influence
    addParticipant(id, 0.2);
  }

  public void addStreamCommentLiker(String streamId, String id) {
    // TODO Take in account the stream influence
    addParticipant(id, 0.1);
  }

}
