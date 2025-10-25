package com.training.dao.memory;

import java.util.List;
import java.util.Map;
import com.training.dao.AdminDao;
import com.training.db.Db;

public class InMemoryAdminDao implements AdminDao {
    @Override public Map<String,Object> stats(){
        return Db.stats();
    }
    @Override public void log(String text){
        Db.log(text);
    }
    @Override public List<String> getLogs(){
        return Db.getLogs();
    }
    @Override public void seed(){
        Db.seed();
    }
}