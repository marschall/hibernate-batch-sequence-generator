package com.github.marschall.hibernate.batchsequencegenerators.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import com.github.marschall.hibernate.batchsequencegenerators.WithRecursiveGenerator;

@Entity(name = "CHILD_ENTITY")
public class ChildEntity {

  @Id
  @GenericGenerator(name = "with-recursive", strategy = "com.github.marschall.hibernate.batchsequencegenerators.WithRecursiveGenerator",
          parameters = {
              @Parameter(name = WithRecursiveGenerator.SEQUENCE, value = "seq_child_id"),
              @Parameter(name = WithRecursiveGenerator.FETCH_SIZE_PARAM, value = "50")
          })
  @GeneratedValue(generator = "with-recursive")
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
