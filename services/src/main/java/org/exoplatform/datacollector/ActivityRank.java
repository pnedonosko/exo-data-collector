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

import static org.exoplatform.datacollector.SocialInfluencers.sigmoid;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ActivityRank.java 00000 Jan 4, 2019 pnedonosko $
 */
public class ActivityRank {

  // participated_by_me
  protected boolean      isParticipatedByMe          = false;

  // liked_by_me
  protected boolean      isLikedByMe                 = false;

  // participated_by_influencer
  protected List<Double> participatedInfluencers     = new ArrayList<>();

  // liked_by_influencer
  protected List<Double> likedInfluencers            = new ArrayList<>();

  // participated_by_connection
  protected boolean      isParticipatedByConnections = false;

  // liked_by_connections
  protected boolean      isLikedByConnections        = false;

  // posted_in_favorite_stream
  protected boolean      isPostedInFavoriteStream    = false;

  // posted_in_favorite_app
  protected boolean      isPostedInFavoriteApp       = false;

  // widely_liked
  protected boolean      isWidelyLiked               = false;

  // reactivity
  protected double       reactivity                  = 1d;

  /**
   * Instantiates a new activity rank.
   */
  public ActivityRank() {
  }

  /**
   * Participated by me.
   *
   * @param isParticipatedByMe the isParticipatedByMe to set
   * @return the activity target
   */
  public ActivityRank participatedByMe(boolean isParticipatedByMe) {
    this.isParticipatedByMe = isParticipatedByMe;
    return this;
  }

  /**
   * Liked by me.
   *
   * @param isLikedByMe the isLikedByMe to set
   * @return the activity target
   */
  public ActivityRank likedByMe(boolean isLikedByMe) {
    this.isLikedByMe = isLikedByMe;
    return this;
  }

  /**
   * Adds the participated influencer weight, if call several times - will add
   * several weights.
   *
   * @param weight the weight to add
   * @return the activity target
   */
  public ActivityRank participatedByInfluencer(double weight) {
    this.participatedInfluencers.add(weight);
    return this;
  }

  /**
   * Adds the liked influencer weight, if call several times - will add several
   * weights.
   *
   * @param weight the weight to add
   * @return the activity target
   */
  public ActivityRank likedByInfluencer(double weight) {
    this.likedInfluencers.add(weight);
    return this;
  }

  /**
   * Participated by connections.
   *
   * @param isParticipatedByConnections the isParticipatedByConnections to set
   * @return the activity target
   */
  public ActivityRank participatedByConnections(boolean isParticipatedByConnections) {
    this.isParticipatedByConnections = isParticipatedByConnections;
    return this;
  }

  /**
   * Liked by connections.
   *
   * @param isLikedByConnections the isLikedByConnections to set
   * @return the activity target
   */
  public ActivityRank likedByConnections(boolean isLikedByConnections) {
    this.isLikedByConnections = isLikedByConnections;
    return this;
  }

  /**
   * Posted in favorite stream.
   *
   * @param isPostedInFavoriteStream the isPostedInFavoriteStream to set
   * @return the activity target
   */
  public ActivityRank postedInFavoriteStream(boolean isPostedInFavoriteStream) {
    this.isPostedInFavoriteStream = isPostedInFavoriteStream;
    return this;
  }

  /**
   * Posted in favorite app.
   *
   * @param isPostedInFavoriteApp the isPostedInFavoriteApp to set
   * @return the activity target
   */
  public ActivityRank postedInFavoriteApp(boolean isPostedInFavoriteApp) {
    this.isPostedInFavoriteApp = isPostedInFavoriteApp;
    return this;
  }

  /**
   * Widely liked.
   *
   * @param isWidelyLiked the isWidelyLiked to set
   * @return the activity target
   */
  public ActivityRank widelyLiked(boolean isWidelyLiked) {
    this.isWidelyLiked = isWidelyLiked;
    return this;
  }

  /**
   * Activity reactivity weight by current user.
   *
   * @param reactivity the reactivity 0..1
   * @return the activity target
   */
  public ActivityRank reactivity(double reactivity) {
    this.reactivity = reactivity;
    return this;
  }

  /**
   * Builds activity target weight.
   *
   * @return the double weight
   */
  public double build() {
    // TODO decrease weights to leave more space before reaching the one.
    double res = 0d;
    if (isPostedInFavoriteApp) {
      res += 0.2;
    }
    if (isLikedByConnections || isWidelyLiked) {
      res += 0.5;
    }
    if (isParticipatedByConnections || isPostedInFavoriteStream) {
      res += 0.6;
    }
    for (Double w : likedInfluencers) {
      res += w * 0.7;
    }
    for (Double w : participatedInfluencers) {
      res += w * 0.9;
    }
    if (isLikedByMe) {
      double myRes = reactivity * 0.7;
      res += myRes;
    }
    if (isParticipatedByMe) {
      double myRes = reactivity * 1;
      res += myRes;
    }
    return sigmoid(res);
  }

}
