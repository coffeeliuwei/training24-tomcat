package com.training.dao.memory;

import java.util.List;
import com.training.dao.CourseDao;
import com.training.db.Db;
import com.training.db.Db.Course;
import com.training.db.Db.TimeSlot;

public class InMemoryCourseDao implements CourseDao {
    @Override public Course addCourse(String name, int credit, int capacity, List<TimeSlot> times){
        return Db.addCourse(name, credit, capacity, times);
    }
    @Override public boolean updateCourse(String id, String name, Integer credit, Integer capacity, List<TimeSlot> times){
        return Db.updateCourse(id, name, credit, capacity, times);
    }
    @Override public boolean deleteCourse(String id){
        return Db.deleteCourse(id);
    }
    @Override public List<Course> listCourses(){
        return Db.listCourses();
    }
    @Override public List<Course> filterCourses(Integer minCredit, Integer maxCredit, String day){
        return Db.filterCourses(minCredit, maxCredit, day);
    }
    @Override public List<Course> recommend(String userId){
        return Db.recommend(userId);
    }
}