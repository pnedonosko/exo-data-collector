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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PredictionDataset.java 00000 Dec 14, 2018 pnedonosko $
 */
@Entity(name = "PredictionModel")
@Table(name = "ST_PREDICTION_MODELS")
@ExoEntity
@IdClass(ModelId.class)
@NamedQueries({
    /* Get last named version */
    @NamedQuery(name = "PredictionModel.findLastModelVersion", query = "SELECT MAX(m.version) FROM PredictionModel m"
        + " WHERE m.name = :name GROUP BY m.name"),
    @NamedQuery(name = "PredictionModel.findStatusByNameAndVersion", query = "SELECT m.status FROM PredictionModel m"
        + " WHERE m.name = :name AND m.version = :version"),
    @NamedQuery(name = "PredictionModel.findByName", query = "SELECT m FROM PredictionModel m"
        + " WHERE m.name = :name")})
public class ModelEntity implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 8773314714414238636L;

  public enum Status {
    NEW, PROCESSING, READY
  }

  @Id
  @Column(name = "NAME", nullable = false)
  protected String name;

  @Id
  @Column(name = "VERSION", nullable = false)
  @GenericGenerator(name = "SEQ_ST_MODEL_VERSION", strategy = "org.exoplatform.prediction.user.domain.VersionGenerator")
  @GeneratedValue(generator = "SEQ_ST_MODEL_VERSION")
  protected Long   version;

  @Column(name = "STATUS", nullable = false)
  @Enumerated(EnumType.STRING)
  protected Status status;

  @Column(name = "MODEL_FILE")
  protected String modelFile;

  @Column(name = "DATASET_FILE")
  protected String datasetFile;

  @Column(name = "CREATED_DATE", nullable = false)
  protected Date   created;

  @Column(name = "ACTIVATED_DATE")
  protected Date   activated;

  @Column(name = "ARCHIVED_DATE")
  protected Date   archived;

  public ModelEntity() {

  }

  public ModelEntity(String name, String datasetFile) {
    super();
    this.name = name;
    this.datasetFile = datasetFile;
    this.status = Status.NEW;
  }

  public String getName() {
    return name;
  }

  public Long getVersion() {
    return version;
  }

  public Status getStatus() {
    return status;
  }

  public String getModelFile() {
    return modelFile;
  }

  public String getDatasetFile() {
    return datasetFile;
  }

  public Date getCreated() {
    return created;
  }

  public Date getActivated() {
    return activated;
  }

  public Date getArchived() {
    return archived;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setModelFile(String modelFile) {
    this.modelFile = modelFile;
  }

  public void setDatasetFile(String datasetFile) {
    this.datasetFile = datasetFile;
  }

  public void setActivated(Date activated) {
    this.activated = activated;
  }

  public void setArchived(Date archived) {
    this.archived = archived;
  }

}
