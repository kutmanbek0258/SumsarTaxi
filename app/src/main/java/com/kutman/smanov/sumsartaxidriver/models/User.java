package com.kutman.smanov.sumsartaxidriver.models;


public class User {

    private String _id;
    private String name;
    private String phone;
    private String password;
    private String newPassword;
    private String token;
    private boolean transport;

    public void set_id(String _id) {
        this._id = _id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setTransport(boolean transport) {
        this.transport = transport;
    }

    public String get_id() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isTransport() {
        return transport;
    }
}
