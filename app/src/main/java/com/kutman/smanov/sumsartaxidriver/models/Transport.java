package com.kutman.smanov.sumsartaxidriver.models;

public class Transport {

    private String _id;
    private User user;
    private Point location;
    private float bearing;
    private float speed;
    private String stateNumber;
    private String model;
    private String type;
    private String session;
    private String image;

    public void set_id(String _id) {
        this._id = _id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setStateNumber(String stateNumber) {
        this.stateNumber = stateNumber;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String get_id() {
        return _id;
    }

    public User getUser() {
        return user;
    }

    public Point getLocation() {
        return location;
    }

    public float getBearing() {
        return bearing;
    }

    public float getSpeed() {
        return speed;
    }

    public String getStateNumber() {
        return stateNumber;
    }

    public String getModel() {
        return model;
    }

    public String getType() {
        return type;
    }

    public String getSession() {
        return session;
    }

    public String getImage() {
        return image;
    }
}
