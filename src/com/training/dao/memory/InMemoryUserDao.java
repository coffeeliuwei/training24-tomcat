package com.training.dao.memory;

import com.training.dao.UserDao;
import com.training.db.Db;
import com.training.db.Db.User;

public class InMemoryUserDao implements UserDao {
    @Override public User addUser(String username, String password, String role, String email){
        return Db.addUser(username, password, role, email);
    }
    @Override public User findByName(String username){
        return Db.findUserByName(username);
    }
    @Override public boolean resetPassword(String username, String newPwd){
        return Db.resetPassword(username, newPwd);
    }
    @Override public User auth(String username, String password){
        return Db.auth(username, password);
    }
}