// java

package com.example.otp.model;

import java.sql.Timestamp;

public class Course {
  private Integer classId;
  private String className;
  private Integer creatorId;
  private String topic;
  private Timestamp createdAt;

  public Course() {}

  public Course(Integer classId,
                String className,
                Integer creatorId,
                String topic,
                Timestamp createdAt
  ) {
    this.classId = classId;
    this.className = className;
    this.creatorId = creatorId;
    this.topic = topic;
    this.createdAt = createdAt;
  }

  public Integer getClassId() {
    return classId;
  }

  public void setClassId(Integer classId) {
    this.classId = classId;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public Integer getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(Integer creatorId) {
    this.creatorId = creatorId;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }
}
