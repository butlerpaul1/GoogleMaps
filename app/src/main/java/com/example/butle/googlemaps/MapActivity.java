package com.example.butle.googlemaps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by butle on 3/20/2018.
 */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();

        mMap = googleMap;
        if (permissionGranted) {

            //SET WHERE MAP SCREEN STARTS
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            mMap.setMyLocationEnabled(true);
            //disable button
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(true);
            mMap.getUiSettings().setTiltGesturesEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);


            init();

        }
    }


    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int location_request_code = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    //vars
    private Boolean permissionGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //widgets
    private EditText mSearchText;
    private ImageView mGps;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "map is ready");

        setContentView(R.layout.activity_map);
        mSearchText = (EditText) findViewById(R.id.input_search);
        mGps = (ImageView) findViewById(R.id.ic_gps);


        getLocationPermission();

    }
    private void init(){
        Log.d(TAG, "init: initializing");

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent) {
                if (actionID == EditorInfo.IME_ACTION_SEARCH
                        || actionID == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){

                    //excute search method
                    geoLocate();
                }
                return false;
            }
        });
        hideSoftKeyboard();

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "clikc gps");
                getDeviceLocation();
            }
        });
    }

    private void geoLocate()
    {
        Log.d(TAG, "geoLocate: geoLocating");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "IOException:" +e.getMessage());
        }
        if (list.size() > 0)
        {
            Address address = list.get(0);

            Log.d(TAG, "found a location" + address.toString());

            moveCamera(new LatLng(address.getLatitude(),address.getLongitude()),DEFAULT_ZOOM,address.getAddressLine(0));

        }
    }
    private void getDeviceLocation(){
        Log.d(TAG, "getting devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if (permissionGranted)
            {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location" );



                        }
                        else {
                            Log.d(TAG, "onComplete: couldn't find location");
                            Toast.makeText(MapActivity.this, "Unable to get current location!", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }

        }catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation, securityexeption:" +e.getMessage());

        }

    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "Moving the camera to lat:" + latLng.latitude + ",long:" + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));

        if(!title.equals("My Location")){

            //drop marker
            MarkerOptions options = new MarkerOptions().position(latLng).title(title);
            mMap.addMarker(options);
            hideSoftKeyboard();

        }
    }

    private void initMap(){
        Log.d(TAG, "init map");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapActivity.this);
    }


    private void getLocationPermission(){
        Log.d(TAG, "getting location permissions");


        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                //access granted
                permissionGranted = true;
                initMap();

            }else{
                ActivityCompat.requestPermissions(this,permissions,location_request_code);
            }
        }else{
            ActivityCompat.requestPermissions(this,permissions,location_request_code);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called");

        permissionGranted = false;
        switch (requestCode){
            case location_request_code: {
                if (grantResults.length > 0)
                {
                    for (int i = 0; i < grantResults.length; i++)
                    {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            permissionGranted = false;
                            Log.d(TAG, "permission Failed");

                            return;
                        }
                    }
                    Log.d(TAG, "permission Granted");
                    permissionGranted = true;
                    //initalise map
                    initMap();



                }
            }
        }
    }

    private void hideSoftKeyboard(){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

}
