package com.jmurphy.gimbalsample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final float GEOFENCE_RADIUS_IN_METERS = 50;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = 10000;
    private static final String REQ_ID = "location_request";
    private final int LOCATION_PERMISSION_CODE = 101;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private PendingIntent geofencePendingIntent;
    private GeofencingClient geofencingClient;

    private ApiService apiService;

    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = new GeofencingClient(this);
        apiService = ApiFactory.create(this);

        message = findViewById(R.id.message);

        checkAndRequestPermissionsForLocation();

    }

    @SuppressLint("MissingPermission")
    private void createGeofence(Location location) {

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);


        List<Geofence> geofenceList = new ArrayList<>();
        geofenceList.add(new Geofence.Builder()

                .setRequestId(REQ_ID)

                .setCircularRegion(
                        location.getLatitude(),
                        location.getLongitude(),
                        GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());

        builder.addGeofences(geofenceList);
        GeofencingRequest req = builder.build();

        geofencingClient.addGeofences(req, getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added


                        apiService.getMessage().enqueue(new Callback<Response>() {
                            @Override
                            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {

                                if (response.body() != null){

                                    String res = response.body().text_out;

                                    Document doc = Jsoup.parse(res);
                                    Elements ps = doc.select("p");
                                    message.setText(ps.text());
                                }
                            }

                            @Override
                            public void onFailure(Call<Response> call, Throwable t) {
                                Log.e(TAG, t.getMessage());
                            }
                        });
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // iFailed to add geofences
                        Log.e(TAG, e.getMessage());
                    }
                });
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private void checkAndRequestPermissionsForLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    getPermissions(),
                    LOCATION_PERMISSION_CODE
            );
        } else {
            loadLocation();
        }
    }

    private String[] getPermissions() {
        /*if (Build.VERSION.SDK_INT>=29){
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        }*/
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    @SuppressLint("MissingPermission")
    private void loadLocation() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location == null) {
                    // request the location
                    fusedLocationProviderClient.requestLocationUpdates(
                            LocationRequest.create(),
                            new LocationCallback() {
                                @Override
                                public void onLocationResult(@NonNull LocationResult locationResult) {
                                    super.onLocationResult(locationResult);

                                    Location location = locationResult.getLastLocation();
                                    if (location == null) {
                                        Log.d(TAG, "Location load fail");
                                    } else {
                                        createGeofence(location);
                                    }
                                    fusedLocationProviderClient.removeLocationUpdates(this);
                                }
                            },
                            null
                    );
                } else {
                    createGeofence(location);
                }
            }
        });
    }
}