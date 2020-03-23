package com.kutman.smanov.sumsartaxidriver.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.kutman.smanov.sumsartaxidriver.R;
import com.kutman.smanov.sumsartaxidriver.TransportApplication;
import com.kutman.smanov.sumsartaxidriver.data.UserData;
import com.kutman.smanov.sumsartaxidriver.models.Point;
import com.kutman.smanov.sumsartaxidriver.models.Request;
import com.kutman.smanov.sumsartaxidriver.models.Session;
import com.kutman.smanov.sumsartaxidriver.models.Transport;
import com.kutman.smanov.sumsartaxidriver.models.TransportList;
import com.kutman.smanov.sumsartaxidriver.models.User;
import com.kutman.smanov.sumsartaxidriver.network.NetworkUtil;
import com.kutman.smanov.sumsartaxidriver.session.TransportSession;
import com.kutman.smanov.sumsartaxidriver.session.UserSession;
import com.kutman.smanov.sumsartaxidriver.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MapActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, GoogleMap.OnCameraChangeListener{

    private Context context;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private LatLng cameraTarget;

    private CompositeSubscription mSubscriptions;

    private SupportMapFragment mFragment;
    private ImageButton zoomIn,zoomOut,myLocation,mapMode,autoRotate;
    private Button btGo;
    private TextView error_tv;
    private TextView tracking_dist_tv;
    private TextView trash_count_tv;

    private UserSession session;
    private TransportSession transportSession;
    private UserData userData;
    private HashMap<String, String> user;

    private Socket socket;

    private Map<String, Marker> transportMarkers = new HashMap<>();

    private Marker mCurrLocationMarker;

    private boolean authorized = false;
    private boolean map_mode_satellite = false;
    private boolean map_focused = false;
    private boolean isMarkerRotating = false;
    private boolean auto_rotate = false;
    private boolean location_changed = false;
    private boolean active_transport_list = false;
    private boolean isLastState = false;

    private float mapNormalZoom = 16;
    private final int mapPaddingLeft = 10;
    private final int mapPaddingTop = 200;
    private final int mapPaddingRight = 10;
    private final int mapPaddingBottom = 160;
    private final int animateCameraDuration = 1500;

    private TransportApplication application;

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int TRANSPORT_ADD_REQUEST = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        transportSession = new TransportSession(getApplicationContext());

        if(transportSession.isLoggedIn()){
            application = (TransportApplication)getApplication();

            context = getApplicationContext();

            mSubscriptions = new CompositeSubscription();

            session = new UserSession(getApplicationContext());
            userData = new UserData(getApplicationContext());
            user = userData.getUserDetails();

            initViews();

            if(session.isLoggedIn()){
                IO.Options opts = new IO.Options();
                opts.forceNew = true;
                opts.query = "token=" + user.get("token");
                try {
                    socket = IO.socket(Constants.BASE_URL,opts);
                    socket.connect();
                    application.setSocket(socket);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            socket.on(Socket.EVENT_CONNECT,onConnect);
            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
            socket.on(Constants.CLIENT_TRANSPORT_GO_RESPONSE,onGoResponse);
            socket.on(Constants.CLIENT_TRANSPORT_STOP_RESPONSE, onStopResponse);
            socket.on(Constants.CLIENTS_TRANSPORT_GO,onGo);
            socket.on(Constants.CLIENTS_TRANSPORT_MOVE,onMove);
            socket.on(Constants.CLIENTS_TRANSPORT_STOP,onStop);
            socket.on(Constants.CLIENTS_TRANSPORT_DISCONNECT,onDisconnect);
            socket.on(Constants.CLIENT_TRANSPORT_ACTIVE_LIST,onTransportList);

            application.setBackgroundMode(false);
        }else {
            Intent intent = new Intent(MapActivity.this, TransportAddActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == TRANSPORT_ADD_REQUEST){
            if(resultCode == Activity.RESULT_OK){
                Bundle extras = intent.getExtras();
                if(extras != null){
                    Gson gPoint = new Gson();
                    application.setTransport(gPoint.fromJson(extras.getString("transport"), Transport.class));
                    application.setTransportSelected(true);
                    go();
                    btGo.setText(R.string.close_session);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, R.string.permission_denied,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void initViews(){
        zoomIn = (ImageButton)findViewById(R.id.zoomIn);
        zoomOut = (ImageButton)findViewById(R.id.zoomOut);
        myLocation = (ImageButton)findViewById(R.id.myLocation);
        mapMode = (ImageButton)findViewById(R.id.mapMode);
        autoRotate = (ImageButton)findViewById(R.id.autoRotate);
        btGo = (Button)findViewById(R.id.go);
        error_tv = (TextView)findViewById(R.id.conn_error_tv);
        tracking_dist_tv = (TextView)findViewById(R.id.tracking_distance);
        trash_count_tv = (TextView)findViewById(R.id.trash_count);

        int trDist = Math.round(application.getTrackingDistance());
        tracking_dist_tv.setText(String.valueOf(trDist) + getResources().getString(R.string.meter));

        btGo.setVisibility(View.INVISIBLE);

        mFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mFragment != null) {
            mFragment.getMapAsync(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void initLastSate(Session lastState){
        if(lastState.isOpen()){
            application.setTracking(lastState.isOpen());
            application.setTransportSelected(true);
            application.setTransport(lastState.getTransport());
            application.setSession(lastState.get_id());
            btGo.setText(R.string.close_session);

            if(mCurrLocationMarker == null){
                Bitmap transport = drawableToBitmap(context,R.drawable.taxi_top);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(application.getLastLocation().getLatitude(), application.getLastLocation().getLongitude()));
                markerOptions.title(getResources().getString(R.string.you));
                markerOptions.flat(true);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(transport));
                mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
                mCurrLocationMarker.setRotation(lastState.getTransport().getBearing());
                float x = (float) 0.5;
                float y = (float) 0.5;
                mCurrLocationMarker.setAnchor(x,y);
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mGoogleMap.setMyLocationEnabled(false);
            autoRotate.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        if(!authorized){
            getProfile(user.get("token"),user.get("email"));
        }

        if(application.isTracking() & application.getLastLocation() != null & location.getSpeed()>=0.5){
            application.setTrackingDistance(application.getTrackingDistance() + (application.getLastLocation().distanceTo(location))/1000);
            tracking_dist_tv.setText(String.format("%.2f",application.getTrackingDistance()) + getResources().getString(R.string.meter));
        }
        application.setLastLocation(location);
        location_changed = true;

        if(!map_focused){
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, mapNormalZoom), animateCameraDuration, null);
            map_focused = true;
        }

        if(!application.isBackgroundMode() & location.getSpeed()>=0.5){
            if(application.isTracking()){
                if (mCurrLocationMarker != null) {
                    if(auto_rotate){
                        updateCamera(mGoogleMap,location);
                    }else {
                        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(application.getLastLocation().getLatitude(), application.getLastLocation().getLongitude())));
                    }
                    rotateMarker(mCurrLocationMarker, location.getBearing());
                    animateMarker(mCurrLocationMarker,new LatLng(application.getLastLocation().getLatitude(), application.getLastLocation().getLongitude()),false);
                }
                emitMove(location);
            }
        }

        if(!isLastState){
            getLastState(user.get("token"),user.get("email"),application.getUser());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(300);
        mLocationRequest.setFastestInterval(200);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(1);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        cameraTarget = cameraPosition.target;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        mGoogleMap.setPadding(mapPaddingLeft, mapPaddingTop, mapPaddingRight, mapPaddingBottom);

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        mGoogleMap.getUiSettings().setZoomGesturesEnabled(true);
        mGoogleMap.getUiSettings().setCompassEnabled(true);

        zoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomIn());
                mapNormalZoom++;
            }
        });

        zoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomOut());
                mapNormalZoom--;
            }
        });

        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateToMyLocation(mGoogleMap);
            }
        });

        autoRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!auto_rotate){
                    auto_rotate = true;
                    Drawable replacer = getResources().getDrawable(R.drawable.autorotate_icon_unlock);
                    autoRotate.setImageDrawable(replacer);
                }else {
                    auto_rotate = false;
                    Drawable replacer = getResources().getDrawable(R.drawable.autorotate_icon_lock);
                    autoRotate.setImageDrawable(replacer);
                }
            }
        });

        btGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go();
            }
        });

        mapMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMapMode();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                if(!application.isTracking()){
                    mGoogleMap.setMyLocationEnabled(true);
                }
            }
        } else {
            buildGoogleApiClient();
            if(!application.isTracking()){
                mGoogleMap.setMyLocationEnabled(true);
            }
        }
        if(!map_focused){
            animateToMyLocation(mGoogleMap);
        }

        mGoogleMap.setOnCameraChangeListener(this);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    private void animateToMyLocation(GoogleMap googleMap) {

        if (ActivityCompat.checkSelfPermission(MapActivity.this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapActivity.this,new String[]{ACCESS_FINE_LOCATION}, 1);
        }else{

            if(application.getLastLocation()!=null){
                LatLng userLocation = new LatLng(application.getLastLocation().getLatitude(), application.getLastLocation().getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, mapNormalZoom), animateCameraDuration, null);
                map_focused = true;
            }
        }
    }

    private void setMapMode(){
        if(!map_mode_satellite){
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            map_mode_satellite = true;
            Drawable replacer = getResources().getDrawable(R.drawable.map_mode_satellite);
            mapMode.setImageDrawable(replacer);
        }else {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            map_mode_satellite = false;
            Drawable replacer = getResources().getDrawable(R.drawable.map_mode_normal);
            mapMode.setImageDrawable(replacer);
        }
    }

    private Bitmap drawableToBitmap(Context gContext, int gResId){
        int heightE = 220;
        Resources resources = gContext.getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resources, gResId);
        int scale = bitmap.getHeight()/heightE;
        int widthE = bitmap.getWidth()/scale;
        return Bitmap.createScaledBitmap(bitmap, widthE, heightE, false);
    }

    private void updateCamera(GoogleMap googleMap, Location newLocation) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()))
                .zoom(mapNormalZoom)
                .bearing(newLocation.getBearing())
                .tilt(0)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void animateMarker(final Marker marker, final LatLng toPosition, final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mGoogleMap.getProjection();
        android.graphics.Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    private void rotateMarker(final Marker marker, final float toRotation) {
        if(!isMarkerRotating) {
            isMarkerRotating = true;
            float x = (float) 0.5;
            float y = (float) 0.5;
            marker.setAnchor(x,y);
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final float startRotation = marker.getRotation();
            final long duration = 1555;

            final Interpolator interpolator = new LinearInterpolator();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);

                    float rot = t * toRotation + (1 -t) * startRotation;

                    marker.setRotation(-rot > 180 ? rot/2 : rot);
                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    }else {
                        isMarkerRotating = false;
                    }
                }
            });
        }
    }

    private void moveTransport(Transport transport){
        rotateMarker(transportMarkers.get(transport.get_id()),transport.getBearing());
        animateMarker(transportMarkers.get(transport.get_id()),new LatLng(transport.getLocation().getCoordinates()[1],transport.getLocation().getCoordinates()[0]),false);
    }

    private void addTransport(Transport transport){
        if(!transportMarkers.containsKey(transport.get_id())){
            Bitmap transportIcon = drawableToBitmap(context,R.drawable.taxi_top);
            LatLng position = new LatLng(transport.getLocation().getCoordinates()[1], transport.getLocation().getCoordinates()[0]);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(position);
            markerOptions.title(transport.getStateNumber());
            markerOptions.flat(true);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(transportIcon));
            Marker marker = mGoogleMap.addMarker(markerOptions);
            marker.setTag(transport);
            String key = transport.get_id();
            transportMarkers.put(key,marker);
        }
    }

    private void removeTransport(Transport transport){
        Marker marker = transportMarkers.get(transport.get_id());
        if (marker != null) {
            marker.remove();
        }
        transportMarkers.remove(transport.get_id());
    }

    private void go(){
        if(!application.isTransportSelected()){
            getTransport(user.get("token"), user.get("email"), application.getUser());
        }else {
            if(!location_changed){
                Toast.makeText(MapActivity.this, R.string.wait_change_location,
                        Toast.LENGTH_LONG).show();
            }else {
                if(application.isTracking()){
                    application.setTrackingDistance(0);
                    mGoogleMap.setMyLocationEnabled(true);
                    autoRotate.setVisibility(View.INVISIBLE);
                    application.setTracking(false);
                    btGo.setText(R.string.open_session);
                    if (mCurrLocationMarker != null) {
                        mCurrLocationMarker.remove();
                        mCurrLocationMarker = null;
                    }
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    emitStop(application.getLastLocation());
                }else {
                    mGoogleMap.setMyLocationEnabled(false);
                    autoRotate.setVisibility(View.VISIBLE);
                    application.setTracking(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        btGo.setText(R.string.close_session);
                    } else {
                        btGo.setText(R.string.close_session);
                    }

                    if(mCurrLocationMarker == null){
                        Bitmap transport = drawableToBitmap(context,R.drawable.taxi_top);
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(new LatLng(application.getLastLocation().getLatitude(), application.getLastLocation().getLongitude()));
                        markerOptions.title(getResources().getString(R.string.you));
                        markerOptions.flat(true);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(transport));
                        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
                        mCurrLocationMarker.setRotation(application.getLastLocation().getBearing());
                        float x = (float) 0.5;
                        float y = (float) 0.5;
                        mCurrLocationMarker.setAnchor(x,y);
                    }
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    emitGo(application.getLastLocation());
                }
            }
        }
    }

    private void getProfile(String token, String email){
        mSubscriptions.add(NetworkUtil.getRetrofit(token)
        .getProfile(email)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe(new Observer<User>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(User resUser) {
                application.setUser(resUser);
                btGo.setVisibility(View.VISIBLE);
                getTransport(user.get("token"), user.get("email"), resUser);
                if(!active_transport_list){
                    emitTransportList(application.getLastLocation());
                }
                authorized = true;
            }
        }));
    }

    private void getLastState(String token, String email, User user){
        mSubscriptions.add(NetworkUtil.getRetrofit(token)
                .getLastState(email, user)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Session>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Session lastState) {
                        isLastState = true;
                        initLastSate(lastState);
                    }
                }));
    }

    private void getTransport(String token, String email, User user){
        mSubscriptions.add(NetworkUtil.getRetrofit(token)
        .getUserTransport(email, user)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe(new Observer<Transport>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Transport transport) {
                application.setTransportSelected(true);
                application.setTransport(transport);
            }
        }));
    }

    private void emitTransportList(Location location){
        Request request = new Request();
        Point point = new Point();
        point.setType("Point");
        Double[] coordinates = new Double[2];
        coordinates[0] = location.getLongitude();
        coordinates[1] = location.getLatitude();
        point.setCoordinates(coordinates);
        request.setLocation(point);
        request.setUser(application.getUser());
        String gRequest = new Gson().toJson(request);
        socket.emit(Constants.SERVER_TRANSPORT_ACTIVE_LIST,gRequest);
    }

    private void emitGo(Location myLocation){
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
        socket.emit(Constants.SERVER_TRANSPORT_GO,sTransport);
    }

    private void emitMove(Location myLocation){
        if(!application.isBackgroundMode()){
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
            transport.setSession(application.getSession());
            String sTransport = new Gson().toJson(transport);
            application.setTransport(transport);
            socket.emit(Constants.SERVER_TRANSPORT_MOVE,sTransport);
        }
    }

    private void emitStop(Location myLocation){
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
        transport.setSession(application.getSession());
        String sTransport = new Gson().toJson(transport);
        application.setTransport(transport);
        socket.emit(Constants.SERVER_TRANSPORT_STOP,sTransport);
    }

    private void setTransportList(Object... args){
        active_transport_list = true;
        JSONObject resJson = (JSONObject)args[0];
        Gson gRes = new Gson();
        TransportList transportList = gRes.fromJson(resJson.toString(),TransportList.class);
        for(Transport transport:transportList.getTransports()){
            addTransport(transport);
        }
    }

    private void setGo(Object... args){
        JSONObject resJson = (JSONObject)args[0];
        Gson gRes = new Gson();
        Transport transport = gRes.fromJson(resJson.toString(),Transport.class);
        addTransport(transport);
    }

    private void setMove(Object... args){
        JSONObject resJson = (JSONObject)args[0];
        Gson gRes = new Gson();
        Transport transport = gRes.fromJson(resJson.toString(),Transport.class);
        if(transportMarkers.containsKey(transport.get_id())){
            moveTransport(transport);
        }else {
            if(application.isTransportSelected()){
                if(!application.getTransport().get_id().equals(transport.get_id())){
                    addTransport(transport);
                }
            }else {
                addTransport(transport);
            }
        }
    }

    private void setStop(Object... args){
        JSONObject resJson = (JSONObject)args[0];
        Gson gRes = new Gson();
        Transport transport = gRes.fromJson(resJson.toString(),Transport.class);
        removeTransport(transport);
    }

    private void setGoResponse(Object... args) {
        JSONObject response = (JSONObject)args[0];
        try {
            application.setSession(response.getString("session"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setStopResponse(Object... args){
        JSONObject response = (JSONObject)args[0];
        try {
            response.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    error_tv.setVisibility(View.INVISIBLE);
                    if(location_changed){
                        getProfile(user.get("token"),user.get("email"));
                    }
                    btGo.setVisibility(View.INVISIBLE);
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    error_tv.setVisibility(View.VISIBLE);
                    btGo.setVisibility(View.INVISIBLE);
                }
            });
        }
    };

    private Emitter.Listener onGoResponse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setGoResponse(args);
                }
            });
        }
    };

    private Emitter.Listener onStopResponse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStopResponse(args);
                }
            });
        }
    };

    private Emitter.Listener onGo = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setGo(args);
                }
            });
        }
    };

    private Emitter.Listener onMove = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setMove(args);
                }
            });
        }
    };

    private Emitter.Listener onStop = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStop(args);
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStop(args);
                }
            });
        }
    };

    private Emitter.Listener onTransportList = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setTransportList(args);
                }
            });
        }
    };
}
