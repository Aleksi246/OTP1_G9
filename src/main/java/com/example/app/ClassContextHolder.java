package com.example.app;

public final class ClassContextHolder {
  private ClassContextHolder() {}

  private static Integer classId;

  public static void setClassId(Integer id) {
    classId = id;
  }

  public static Integer getClassId() {
    return classId;
  }
}
