package com.kutman.smanov.sumsartaxidriver.models;

public class Request {
    private User user;
    private Point location;

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public Point getLocation() {
        return location;
    }
}
