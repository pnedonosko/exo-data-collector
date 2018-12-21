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
package org.exoplatform.prediction.user.domain;

import java.io.Serializable;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ModelId.java 00000 Dec 16, 2018 pnedonosko $
 */
public class ModelId implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -6466053843467892933L;

  /** The model unique version. */
  protected Long            version;

  /** The model name. */
  protected String          name;

  /**
   * Instantiates a new model id.
   */
  public ModelId() {
  }

  /**
   * Gets the version.
   *
   * @return the version
   */
  public Long getVersion() {
    return version;
  }

  /**
   * Sets the version.
   *
   * @param version the new version
   */
  public void setVersion(Long version) {
    this.version = version;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

}
