package com.github.marschall.hibernate.batchsequencegenerators.entities;

import static com.github.marschall.hibernate.batchsequencegenerators.BatchSequenceGenerator.FETCH_SIZE_PARAM;
import static com.github.marschall.hibernate.batchsequencegenerators.BatchSequenceGenerator.SEQUENCE_PARAM;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity(name = "PARENT_ENTITY")
public class ParentEntity {

  @Id
  @GenericGenerator(name = "parent_id_generator", strategy = "com.github.marschall.hibernate.batchsequencegenerators.BatchSequenceGenerator",
          parameters = {
              @Parameter(name = SEQUENCE_PARAM, value = "SEQ_PARENT_ID"),
              @Parameter(name = FETCH_SIZE_PARAM, value = "50")
          })
  @GeneratedValue(generator = "parent_id_generator")
  @Column(name = "PARENT_ID")
  private Long parentId;

  @OneToMany
  @JoinColumn(name = "PARENT_ID")
//  @Cascade({CascadeType.PERSIST})
  private Set<ChildEntity> children = new HashSet<>();

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Set<ChildEntity> getChildren() {
    return children;
  }

  public void setChildren(Set<ChildEntity> children) {
    this.children = children;
  }

  public void addChild(ChildEntity child) {
    this.children.add(child);
  }

}
