package com.test2;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.test2.databinding.ActivityMapsBinding;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements LocationListener,OnMapReadyCallback, TaskLoadedCallback, CallBack  {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private DatabaseReference databaseReference;
    private LocationManager locationManager;

    private final int MIN_TIME = 1000;
    private final int MIN_DISTANCE = 1;

    private final LatLng CURRENT_LOCATION = new LatLng(32.432680,74.115017); // magna food office
    private final LatLng FINAL_DESTINATION = new LatLng(32.4000796, 74.126708); // haji park lcation
    Marker marker;


    private Polyline currentpolyline;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("User-101");


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getlocationUpdate();
        
        readchanges();
    }

    private void readchanges() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull  DataSnapshot snapshot) {

                if(snapshot.exists()){
                    try{
                        MyLocation location = snapshot.getValue(MyLocation.class);
                        if(location!=null){
                            mMap.clear();

                            marker =  mMap.addMarker(new MarkerOptions().position(CURRENT_LOCATION).title("MagnaFoodOffice")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            marker =  mMap.addMarker(new MarkerOptions().position(FINAL_DESTINATION).title("HajiPark"));

                            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                            Bitmap bmp = Bitmap.createBitmap(70, 60, conf);
                            Canvas canvas1 = new Canvas(bmp);

                            // paint defines the text color, stroke width and size
                            Paint color = new Paint();
                            color.setTextSize(40);
                            color.setColor(Color.BLACK);

                            // modify canvas
                            canvas1.drawBitmap(BitmapFactory.decodeResource(getResources(),
                                    R.drawable.gray_background), 0,0, color);

                            canvas1.drawText("I'm", 10, 40, color);

                            Log.v("Test",location.getLatitude() +" -- " + location.getLongitude() + "");

                            LatLng changing_location = new LatLng(location.getLatitude(),location.getLongitude());
                            marker =  mMap.addMarker(new MarkerOptions().position(changing_location)
                                    .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                                    .anchor(0.5f, 1));
                           // marker.setPosition(new LatLng(location.getLatitude(),location.getLongitude()));
                            MarkerOptions markerOptions = new MarkerOptions().position(changing_location);

                            new FetchURL(MapsActivity.this, getApplicationContext())
                                    .execute(getURL(markerOptions.getPosition(), new MarkerOptions().position(FINAL_DESTINATION).getPosition(), "driving"), "driving");
                        }
                    }catch (Exception e){
                        Toast.makeText(MapsActivity.this,e.getMessage() ,Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull  DatabaseError error) {

            }
        });
    }

    private void getlocationUpdate() {

        if(locationManager!=null){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME,MIN_DISTANCE,this);
                }else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,MIN_TIME,MIN_DISTANCE,this);
                }else{
                    Toast.makeText(this,"No Provider Enabled" ,Toast.LENGTH_SHORT).show();
                }
            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 101){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getlocationUpdate();
            }else{
                Toast.makeText(this,"Permission Required" ,Toast.LENGTH_SHORT).show();

            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(32.4000796, 74.126708);
        marker =  mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in HajiPark"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {

        if(location!=null){
            savelocation(location);
        }else{
            Toast.makeText(this,"No Location" ,Toast.LENGTH_LONG).show();
        }
    }

    private void savelocation(Location location) {
        databaseReference.setValue(location);
    }

    private void showAllMarkers() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        mMap.setTrafficEnabled(true); // traffic enables
        mMap.setBuildingsEnabled(true);

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(magnawarehouseLatLng));

    }

    private String getURL(LatLng origin, LatLng destination, String directionMode) {

        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + destination.latitude + "," + destination.longitude;
        String mode = "mode=" + directionMode;
        String parameter = str_origin + "&" + str_dest + "&" + mode;
        String format = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + format + "?" +
                parameter + "&key=AIzaSyAjm2I-uVKodfHbHq5DsGTzs2ckZNxKVF4";
        return url;

    }

    @Override
    public void onTaskDone(Object... values) {
//        if (currentpolyline != null)
//            currentpolyline.remove();
        currentpolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }

    @Override
    public void onPostExecute(String result) {

    }

    @Override
    public void onPreExecute(String caller) {

    }
}