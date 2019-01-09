package org.exoplatform.datacollector.domain;

import java.util.Date;

/**
 * Abstract activity entity.
 */

public abstract class AbstractActivityEntity {

  /**
   * Gets the post id.
   *
   * @return the post id
   */
  public abstract String getId();

  /**
   * Gets the provider id.
   *
   * @return the provider id
   */
  public abstract String getProviderId();

  /**
   * Gets the type.
   *
   * @return the type
   */
  public abstract String getType();

  /**
   * Gets the poster id.
   *
   * @return the poster id
   */
  public abstract String getPosterId();

  /**
   * Gets the owner id.
   *
   * @return the owner id
   */
  public abstract String getOwnerId();

  /**
   * Gets the posted time in milliseconds.
   *
   * @return the posted
   */
  public abstract Long getPosted();

  /**
   * Gets the posted date.
   *
   * @return the posted date
   */
  public abstract Date getPostedDate();

  /**
   * Gets the updated date.
   *
   * @return the updated date
   */
  public abstract Date getUpdatedDate();

  /**
   * Gets the updated time in milliseconds.
   *
   * @return the updated
   */
  public abstract Long getUpdated();

  /**
   * Gets the hidden.
   *
   * @return the hidden
   */
  public abstract Boolean getHidden();

  /**
   * Gets the parent id.
   *
   * @return the parent id
   */
  public abstract String getParentId();

}
