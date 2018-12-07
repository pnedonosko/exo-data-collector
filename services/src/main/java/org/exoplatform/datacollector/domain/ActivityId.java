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
package org.exoplatform.datacollector.domain;

import java.io.Serializable;

/**
 * The Class ActivityId.
 */
public class ActivityId implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 3235629193550319959L;

  /**  The activity id. */
  protected String            postId;
  
  /**  The poster id. */
  protected String            posterId;

  /** The updated date (timestamp). */
  protected Long            updated;

  /**
   * Instantiates a new ActivityId.
   */
  public ActivityId() {

  }

  /**
   * Instantiates a new ActivityId.
   *
   * @param posterId the poster id
   * @param postId the post id
   * @param updated the updated
   */
  public ActivityId(String posterId, String postId, Long updated) {
    super();
    this.posterId = posterId;
    this.postId = postId;
    this.updated = updated;
  }

  /**
   * Gets the poster id.
   *
   * @return the poster id
   */
  public String getPosterId() {
    return posterId;
  }

  /**
   * Gets the post id.
   *
   * @return the post id
   */
  public String getPostId() {
    return postId;
  }

  /**
   * Gets the updated.
   *
   * @return the updated
   */
  public Long getUpdated() {
    return updated;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int hc = 7 + posterId.hashCode() * 31;
    hc = hc * 31 + posterId.hashCode();
    hc = hc * 31 + updated.hashCode();
    return hc;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (o != null) {
      if (ActivityId.class.isAssignableFrom(o.getClass())) {
        ActivityId other = ActivityId.class.cast(o);
        return posterId.equals(other.getPosterId()) && postId.equals(other.getPostId()) && updated.equals(other.getUpdated());
      }
    }
    return false;
  }

}
