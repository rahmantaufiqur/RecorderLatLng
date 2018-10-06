package barikoi.taufiq.videorecorder;


import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Timer;
import java.util.TimerTask;

import static android.support.v4.app.ActivityCompat.requestPermissions;


/**
 * Created by Sakib on 6/25/2017.
 */

public class LocationTask extends Service {
    public int REQUEST_CHECK_SETTINGS = 0x2;
    public int REQUEST_CHECK_PERMISSION = 0x1;
    private LocationRequest mLocationRequest;

    private LocationCallback mLocationCallback;

    private FusedLocationProviderClient mFusedLocationClient;
    private static final String TAG = "LocationTask";

    Location mlocation;
    private Handler mHandler = new Handler();
    private Timer mTimer = null;

    long notify_interval = 1000;
    public static String str_receiver = "Taufiq";
    Intent intent;
    private long starttime;

    public LocationTask() {

    }

    private class TimerTaskToGetLocation extends TimerTask {
        @Override
        public void run() {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                        fn_update(mlocation);
                }
            });

        }
    }

    private void fn_update(Location location) {
        if (location == null) {

            intent.putExtra("time", (int) ((System.currentTimeMillis() - starttime) / 1000));
            intent.putExtra("speed", "0");
            intent.putExtra("latutide", "0");
            intent.putExtra("longitude", "0");
            sendBroadcast(intent);
        }else {
            if (location.hasAccuracy() && location.getAccuracy() > 150) {
                Log.d(TAG, "location rejected because accuracy " + location.getAccuracy());
            } else {
                Log.d(TAG, "location recieved with accuracy " + location.getAccuracy()+", time "+  (int) ((System.currentTimeMillis() - starttime) / 1000));

                intent.putExtra("time", (int) ((System.currentTimeMillis() - starttime) / 1000));
                intent.putExtra("speed", location.getSpeed() + "");
                intent.putExtra("latutide", location.getLatitude() + "");
                intent.putExtra("longitude", location.getLongitude() + "");
                sendBroadcast(intent);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()" );
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        intent = new Intent(str_receiver);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(500);
        mLocationRequest.setFastestInterval(100);
        mLocationRequest.setSmallestDisplacement(1);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setMaxWaitTime(1000);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    onLocationChanged(location);
                }
            };
        };
        displayLocation();
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        starttime=System.currentTimeMillis();
        mTimer = new Timer();
        mTimer.schedule(new TimerTaskToGetLocation(), 0, notify_interval);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        Log.i(TAG, "onCreate() , service stopped...");
        stoptask();
        stopSelf();
        super.onDestroy();

    }



    public void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling

        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    mlocation=location;
                }
            });
            checkForLocationsettings();
        }
    }

    public void checkForLocationsettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
                .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(Task<LocationSettingsResponse> task) {
                        try {
                            LocationSettingsResponse response = task.getResult(ApiException.class);
                            // All location settings are satisfied. The client can initialize location
                            getLastLocation();

                        } catch (ApiException exception) {
                            switch (exception.getStatusCode()) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied. But could be fixed by showing the
                                    // user a dialog.
                                   /* try {
                                        // Cast to a resolvable exception.
                                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().

                                        *//*resolvable.startResolutionForResult(
                                                this,
                                                REQUEST_CHECK_SETTINGS);*//*
                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    } catch (ClassCastException e) {
                                        // Ignore, should be an impossible error.
                                    }*/
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    // Location settings are not satisfied. However, we have no way to fix the
                                    // settings so we won't show the dialog.
                                    //mGoogleApiClient.disconnect();
                                    break;
                            }
                        }
                    }
                });

    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);

    }
    private void onLocationChanged(Location location){
        mlocation=location;
    }

    public void stoptask(){
        starttime=0;
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        //mGoogleApiClient.disconnect();
    }





}
