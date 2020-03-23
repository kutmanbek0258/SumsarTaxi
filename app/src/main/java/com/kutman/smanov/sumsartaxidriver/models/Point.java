package com.kutman.smanov.sumsartaxidriver.models;

public class Point {
    private String type;
    private Double[] coordinates;

    public void setType(String type) {
        this.type = type;
    }

    public void setCoordinates(Double[] coordinates) {
        this.coordinates = coordinates;
    }

    public String getType() {
        return type;
    }

    public Double[] getCoordinates() {
        return coordinates;
    }
}
