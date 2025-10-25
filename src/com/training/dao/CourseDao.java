package com.training.dao;

import java.util.List;
import com.training.db.Db.Course;
import com.training.db.Db.TimeSlot;

public interface CourseDao {
    Course addCourse(String name, int credit, int capacity, List<TimeSlot> times);
    boolean updateCourse(String id, String name, Integer credit, Integer capacity, List<TimeSlot> times);
    boolean deleteCourse(String id);
    List<Course> listCourses();
    List<Course> filterCourses(Integer minCredit, Integer maxCredit, String day);
    List<Course> recommend(String userId);
}