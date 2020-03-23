package com.kutman.smanov.sumsartaxidriver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;
import com.kutman.smanov.sumsartaxidriver.activities.StartActivity;
import com.kutman.smanov.sumsartaxidriver.models.Point;
import com.kutman.smanov.sumsartaxidriver.models.Transport;
import com.kutman.smanov.sumsartaxidriver.utils.Constants;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import io.socket.client.Socket;

public class LocationService extends Service {

    private TransportApplication application;
    private Socket socket;

    private final LocationServiceBinder binder = new LocationServiceBinder();
    private static final String TAG = "LocationService";
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private final int LOCATION_INTERVAL = 300;
    private final int LOCATION_DISTANCE = 1;

    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    private TimerCounter tc;
    private int counter = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private class LocationListener implements android.location.LocationListener {
        private final String TAG = "LocationListener";

        public LocationListener(String provider) {
            //application.setLastLocation(new Location(provider));
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "LocationChanged: " + location);
            if(application.isBackgroundMode()){
                if(application.isTracking() & application.getLastLocation() != null){
                    application.setTrackingDistance(application.getTrackingDistance() + (application.getLastLocation().distanceTo(location))/1000);
                    application.setLastLocation(location);
                    Log.i(TAG, "LocationChanged: " + location);
                    emitMove(location);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + status);

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        tc.startTimer(counter);
        return START_NOT_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        tc = new TimerCounter();
        startForeground(12345678, getNotificationO());
        application = (TransportApplication)getApplication();
        socket = application.getSocket();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listeners, ignore", ex);
            }
        }
        Intent broadcastIntent = new Intent(this, SensorRestartBroadcastReceiver.class);
        sendBroadcast(broadcastIntent);
        tc.stopTimerTask();
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking() {
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListener);

        } catch (SecurityException ex) {
            // Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            // Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    public void stopTracking() {
        this.onDestroy();
    }

    private Notification getNotificationO() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My Channel", NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID).setAutoCancel(true);
            return builder.build();
        }

        Intent intent = new Intent(this, StartActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);

        NotificationCompat.Builder foregroundNotification = new NotificationCompat.Builder(this, CHANNEL_ID);
        foregroundNotification.setOngoing(true);

        foregroundNotification.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_running))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent);

        return foregroundNotification.build();
    }

    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    private void emitMove(Location myLocation){
        Transport transport = application.getTransport();
        Point point = new Point();
        point.setType("Point");
        Double[] coordinates = new Double[2];
        coordinates[0] = myLocation.getLongitude();
        coordinates[1] = myLocation.getLatitude();
        point.setCoordinates(coordinates);
        transport.setLocation(point);
        transport.setBearing(myLocation.getBearing());
        transport.setSpeed(myLocation.getSpeed());
        String sTransport = new Gson().toJson(transport);
        application.setTransport(transport);
        socket.emit(Constants.SERVER_TRANSPORT_MOVE,sTransport);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "serviceonTaskRemoved()");

        Intent intent = new Intent(getApplicationContext(), LocationService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 5000, pendingIntent);

        super.onTaskRemoved(rootIntent);

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "onLowMemory()");
    }
}
