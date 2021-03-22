package com.mycompany.searchservice.dto;

import java.io.Serializable;

public class User implements Serializable {
    public enum Type {
        REGISTER,
        LOGIN,
        REMIND
    }
    private static final long serialVersionUID = 1L;
    private String userName;
    private String password;
    private Type type;

    public User(String userName, String password, Type type) {
        this.userName = userName;
        this.password = password;
        this.type = type;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
