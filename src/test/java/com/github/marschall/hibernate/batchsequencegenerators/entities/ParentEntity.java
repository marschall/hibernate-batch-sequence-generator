package com.github.marschall.hibernate.batchsequencegenerators.entities;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import com.github.marschall.hibernate.batchsequencegenerators.WithRecursiveGenerator;

@Entity(name = "PARENT_ENTITY")
public class ParentEntity {

  // https://stackoverflow.com/questions/31158509/how-to-generate-custom-id-using-hibernate-while-it-must-be-primary-key-of-table
  @Id
  @GenericGenerator(name = "with-recursive", strategy = "com.github.marschall.hibernate.batchsequencegenerators.WithRecursiveGenerator",
          parameters = {
              @Parameter(name = WithRecursiveGenerator.SEQUENCE, value = "seq_parent_id"),
              @Parameter(name = WithRecursiveGenerator.FETCH_SIZE_PARAM, value = "50")
          })
  @GeneratedValue(generator = "with-recursive")
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
