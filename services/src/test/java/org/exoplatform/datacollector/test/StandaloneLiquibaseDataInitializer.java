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
package org.exoplatform.datacollector.test;

import org.exoplatform.commons.persistence.impl.LiquibaseDataInitializer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.naming.InitialContextInitializer;

/**
 * Goal of this {@link LiquibaseDataInitializer} wrapper to start
 * {@link InitialContextInitializer} prior the initializer work.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: StandaloneLiquibaseDataInitializer.java 00000 Dec 4, 2018
 *          pnedonosko $
 */
public class StandaloneLiquibaseDataInitializer extends LiquibaseDataInitializer {

  /**
   * Instantiates a new standalone Liquibase data initializer.
   *
   * @param initParams the init params
   */
  public StandaloneLiquibaseDataInitializer(InitParams initParams, InitialContextInitializer jndiInitializer) {
    super(initParams);
  }

}
