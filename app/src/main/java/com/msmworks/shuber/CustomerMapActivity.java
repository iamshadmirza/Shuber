package com.msmworks.shuber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private LatLng mLatLng;
    public static final int REQ_LOCATION_CODE = 99;
    private String mUserID;
    private DatabaseReference mDatabaseReference;
    private GeoFire mGeoFire;
    private Button mLogout, mRequest, mSettings ;
    private LatLng mPickupLocation;
    private int radius = 1;
    private boolean mDriverFound = false;
    private String mDriverFoundID;
    private Marker mDriverMarker;
    private Boolean reqBol = false;
    private Marker mPickupMarker;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        mLogout = (Button) findViewById(R.id.logout_button);
        mRequest = (Button) findViewById(R.id.call_shuber);
        mSettings = (Button) findViewById(R.id.settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkLocationPermission();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMapActivity.this, FirstPage.class));
                finish();
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(reqBol){
                    reqBol = false;
                    mGeoQuery.removeAllListeners();
                    if (driverLocationRefListener!=null)
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if (mDriverFoundID != null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(mDriverFoundID);
                        driverRef.setValue(true);
                        mDriverFoundID = null;
                    }
                    mDriverFound = false;
                    radius = 1;

                    mUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    mDatabaseReference = FirebaseDatabase.getInstance().getReference("customerRequest");
                    mGeoFire = new GeoFire(mDatabaseReference);
                    mGeoFire.removeLocation(mUserID, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            Toast.makeText(CustomerMapActivity.this, "Ride Cancelled", Toast.LENGTH_SHORT).show();

                        }
                    });
                    if(mPickupMarker != null){
                        mPickupMarker.remove();
                    }
                    mRequest.setText("Call Shuber");
                }else{
                    reqBol = true;
                    mUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    mDatabaseReference = FirebaseDatabase.getInstance().getReference("customerRequest");
                    mGeoFire = new GeoFire(mDatabaseReference);
                    mGeoFire.setLocation(mUserID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            Toast.makeText(CustomerMapActivity.this, "Customer ID saved", Toast.LENGTH_SHORT).show();
                        }
                    });
                    mPickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    mPickupMarker = mMap.addMarker(new MarkerOptions().position(mPickupLocation).title("Pick Me Up!!").icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));
                    mRequest.setText("Getting your Driver...");
                    getClosestDriver();
                }

            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class));
                return;
            }
        });
    }

    private GeoQuery mGeoQuery;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getClosestDriver() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");
        mGeoFire = new GeoFire(driverLocationRef);
        mGeoQuery = mGeoFire.queryAtLocation(new GeoLocation(mPickupLocation.latitude, mPickupLocation.longitude), radius);
        mGeoQuery.removeAllListeners();
        mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!mDriverFound && reqBol){
                    mDriverFound = true;
                    mDriverFoundID = key;
                    DatabaseReference driversRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(mDriverFoundID);
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideID", customerID );
                    driversRef.updateChildren(map);

                    getDriverLocation();
                    mRequest.setText("Looking for driver location");
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!mDriverFound){
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void getDriverLocation() {
        DatabaseReference mDriverLocationReference = FirebaseDatabase.getInstance().getReference()
                .child("driversWorking")
                .child(mDriverFoundID)
                .child("l");
        mDriverLocationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Driver has been found");
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if(mDriverMarker!=null){
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(mPickupLocation.latitude);
                    loc1.setLongitude(mPickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);
                    if(distance<100){
                        mRequest.setText("Driver has arrived");
                    }else{
                        mRequest.setText("Driver Found "+String.valueOf(distance));
                        Toast.makeText(CustomerMapActivity.this, "Drivers Available", Toast.LENGTH_SHORT).show();
                    }


                    mDriverMarker= mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.drawable.cab)));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public boolean checkLocationPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION_CODE);
            }else{
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION_CODE);
            }
            return false;
        }
        else
            return false;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        mLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(mLatLng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQ_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission is granted
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                        if(mGoogleApiClient == null){
                            //create client
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else{
                    //permission not granted
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}
