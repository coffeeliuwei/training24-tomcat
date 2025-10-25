package com.training.dao;

import java.util.List;
import java.util.Map;

public interface AdminDao {
    Map<String,Object> stats();
    void log(String text);
    List<String> getLogs();
    void seed();
}