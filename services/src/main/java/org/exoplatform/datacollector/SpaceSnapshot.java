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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.social.core.space.model.Space;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SpaceSnapshot.java 00000 Jan 30, 2019 pnedonosko $
 */
public class SpaceSnapshot {

  protected final Set<String> members;

  protected final Set<String> managers;

  /**
   * Instantiates a new space snapshot.
   *
   * @param space the space
   */
  protected SpaceSnapshot(Space space) {
    this(Arrays.asList(space.getMembers()), Arrays.asList(space.getManagers()));
  }

  /**
   * Instantiates a new space snapshot.
   *
   * @param members the members
   * @param managers the managers
   */
  protected SpaceSnapshot(Collection<String> members, Collection<String> managers) {
    this.members = Collections.unmodifiableSet(new HashSet<String>(members));
    this.managers = Collections.unmodifiableSet(new HashSet<String>(managers));
  }

  /**
   * Gets the members.
   *
   * @return the members
   */
  public Set<String> getMembers() {
    return members;
  }

  /**
   * Gets the managers.
   *
   * @return the managers
   */
  public Set<String> getManagers() {
    return managers;
  }

}
