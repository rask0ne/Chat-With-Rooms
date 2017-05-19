package com.messages;

import java.io.Serializable;

/**
 * Created by rask on 06.05.2017.
 */
public class User implements Serializable {

    String name;
    String email;
    String picture;
    Status status;
    Rooms room;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setRoom(Rooms room){
        this.room = room;
    }

    public Rooms getRoom(){
        return this.room;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
