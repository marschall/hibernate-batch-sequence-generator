package com.github.marschall.hibernate.batchsequencegenerator.entities;

import static com.github.marschall.hibernate.batchsequencegenerator.BatchSequenceGenerator.FETCH_SIZE_PARAM;
import static com.github.marschall.hibernate.batchsequencegenerator.BatchSequenceGenerator.SEQUENCE_PARAM;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity(name = "CHILD_ENTITY")
public class ChildEntity {

  @Id
  @GenericGenerator(
          name = "child_id_generator",
          strategy = "com.github.marschall.hibernate.batchsequencegenerator.BatchSequenceGenerator",
          parameters = {
              @Parameter(name = SEQUENCE_PARAM, value = "SEQ_CHILD_ID"),
              @Parameter(name = FETCH_SIZE_PARAM, value = "50")
          })
  @GeneratedValue(generator = "child_id_generator")
  @Column(name = "CHILD_ID")
  private Long childId;

  @Column(name = "PARENT_ID")
  private Long parentId;

  public Long getChildId() {
    return childId;
  }

  public void setChildId(Long childId) {
    this.childId = childId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

}
