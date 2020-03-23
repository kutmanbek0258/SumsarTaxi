package com.kutman.smanov.sumsartaxidriver;

import android.app.Application;
import android.location.Location;

import com.kutman.smanov.sumsartaxidriver.models.Transport;
import com.kutman.smanov.sumsartaxidriver.models.User;

import io.socket.client.Socket;

public class TransportApplication extends Application {

    private Socket mSocket;
    private User user;
    private String session;
    private Transport transport;
    private boolean transportSelected = false;
    private boolean tracking;
    private boolean backgroundMode = false;
    private float trackingDistance = 0;
    private Location lastLocation;

    public void setSocket(Socket socket){
        mSocket = socket;
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
    }

    public void setBackgroundMode(boolean backgroundMode) {
        this.backgroundMode = backgroundMode;
    }

    public void setTransportSelected(boolean transportSelected) {
        this.transportSelected = transportSelected;
    }

    public void setTrackingDistance(float trackingDistance) {
        this.trackingDistance = trackingDistance;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public String getSession() {
        return session;
    }

    public Transport getTransport() {
        return transport;
    }

    public boolean isTracking() {
        return tracking;
    }

    public boolean isBackgroundMode() {
        return backgroundMode;
    }

    public boolean isTransportSelected() {
        return transportSelected;
    }

    public float getTrackingDistance() {
        return trackingDistance;
    }

    public Location getLastLocation() {
        return lastLocation;
    }
}
