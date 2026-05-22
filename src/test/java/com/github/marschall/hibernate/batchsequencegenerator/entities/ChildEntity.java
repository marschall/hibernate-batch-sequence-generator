package com.github.marschall.hibernate.batchsequencegenerator.entities;

import static com.github.marschall.hibernate.batchsequencegenerator.BatchSequenceGenerator.FETCH_SIZE_PARAM;
import static com.github.marschall.hibernate.batchsequencegenerator.BatchSequenceGenerator.SEQUENCE_PARAM;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import com.github.marschall.hibernate.batchsequencegenerator.BatchSequenceGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class ChildEntity {

  @Id
  @GenericGenerator(
          name = "child_id_generator",
          type = BatchSequenceGenerator.class,
          parameters = {
              @Parameter(name = SEQUENCE_PARAM, value = "SEQ_CHILD_ID"),
              @Parameter(name = FETCH_SIZE_PARAM, value = "50")
          })
  @GeneratedValue(generator = "child_id_generator")
  private Long childId;

  private Long parentId;

  public Long getChildId() {
    return this.childId;
  }

  public void setChildId(Long childId) {
    this.childId = childId;
  }

  public Long getParentId() {
    return this.parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

}
