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

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FocusGroupPlugin.java 00000 Apr 19, 2019 pnedonosko $
 */
public class FocusGroupPlugin extends BaseComponentPlugin {

  public static final String FOCUS_GROUP_PARAM = "focus-group";

  private final String       groupId;

  /**
   * Instantiates a new focus group plugin.
   *
   * @param initParams the init params
   * @throws ConfigurationException the configuration exception
   */
  public FocusGroupPlugin(InitParams initParams) throws ConfigurationException {
    String groupId;
    ValueParam groupParam = initParams.getValueParam(FOCUS_GROUP_PARAM);
    if (groupParam != null) {
      groupId = groupParam.getValue();
    } else {
      throw new ConfigurationException(FOCUS_GROUP_PARAM + " parameter is mandatory");
    }
    this.groupId = groupId;
  }

  /**
   * Gets the group id.
   *
   * @return the groupId
   */
  public String getGroupId() {
    return groupId;
  }

}
