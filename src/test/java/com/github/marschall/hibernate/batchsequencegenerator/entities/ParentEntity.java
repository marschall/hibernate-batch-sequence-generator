package com.github.marschall.hibernate.batchsequencegenerator.entities;

import java.util.HashSet;
import java.util.Set;

import com.github.marschall.hibernate.batchsequencegenerator.BatchSequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

@Entity(name = "PARENT_ENTITY")
public class ParentEntity {

  @Id
  @BatchSequence(name = "SEQ_PARENT_ID", fetchSize = 50)
  @Column(name = "PARENT_ID")
  private Long parentId;

  @OneToMany
  @JoinColumn(name = "PARENT_ID")
  private Set<ChildEntity> children = new HashSet<>();

  public Long getParentId() {
    return this.parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Set<ChildEntity> getChildren() {
    return this.children;
  }

  public void setChildren(Set<ChildEntity> children) {
    this.children = children;
  }

  public void addChild(ChildEntity child) {
    this.children.add(child);
  }

}
