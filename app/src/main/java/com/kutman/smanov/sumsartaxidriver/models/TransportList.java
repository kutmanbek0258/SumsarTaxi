package com.kutman.smanov.sumsartaxidriver.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class TransportList {

    @SerializedName("transports")
    @Expose
    private ArrayList<Transport> transports;

    public void setTransports(ArrayList<Transport> transports) {
        this.transports = transports;
    }

    public ArrayList<Transport> getTransports() {
        return transports;
    }
}
