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
package org.exoplatform.datacollector.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.datacollector.domain.id.IdentityProfileId;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: IdentityProfileEntity.java 00000 Jan 14, 2019 pnedonosko $
 */
@Entity(name = "IdentityProfile")
@Table(name = "ST_IDENTITY_PROFILE")
@IdClass(IdentityProfileId.class)
@ExoEntity
@NamedQueries({ @NamedQuery(name = "IdentityProfile.findById", query = "SELECT p FROM IdentityProfile p WHERE p.id = :id"),
    @NamedQuery(name = "IdentityProfile.findByName", query = "SELECT p FROM IdentityProfile p WHERE p.name = :name") })
public class IdentityProfileEntity {

  /** The ID of Social identity (numerical value). */
  @Id
  @Column(name = "ID")
  protected String id;

  /** The name of Social identity (text with user name or group pretty name). */
  @Id
  @Column(name = "NAME")
  protected String name;

  /** The social identity provider ID. */
  @Column(name = "PROVIDER_ID")
  protected String providerId;

  /** The focus of the profile. */
  @Column(name = "FOCUS")
  protected String focus;

  /** The context of the profile (mapped values, e.g. gender:female;role:administrator). */
  @Column(name = "CONTEXT")
  protected String context;

  /**
   * 
   */
  public IdentityProfileEntity() {
  }

  public IdentityProfileEntity(String id, String name, String providerId, String focus, String context) {
    super();
    this.id = id;
    this.name = name;
    this.providerId = providerId;
    this.focus = focus != null ? focus.intern() : null;
    this.context = context;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getFocus() {
    return focus;
  }

  public void setFocus(String focus) {
    this.focus = focus != null ? focus.intern() : null;
  }

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

}
