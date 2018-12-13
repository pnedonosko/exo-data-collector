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

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ActivityDataException.java 00000 Dec 13, 2018 pnedonosko $
 */
public class ActivityDataException extends Exception {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -2120871397066227799L;

  /**
   * Instantiates a new activity data exception.
   *
   * @param message the message
   */
  public ActivityDataException(String message) {
    super(message);
  }

  /**
   * Instantiates a new activity data exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public ActivityDataException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new activity data exception.
   *
   * @param message the message
   * @param cause the cause
   * @param enableSuppression the enable suppression
   * @param writableStackTrace the writable stack trace
   */
  public ActivityDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
