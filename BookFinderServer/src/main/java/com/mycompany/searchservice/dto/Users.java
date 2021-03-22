package com.mycompany.searchservice.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement(name = "users")
public class Users {

    private Set<User> users;

    public Set<User> getUsers() {
        return users;
    }

    @XmlElement(name = "user")
    public void setUsers(Set<User> users) {
        this.users = users;
    }
}
