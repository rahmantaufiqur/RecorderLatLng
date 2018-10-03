package barikoi.taufiq.videorecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

import android.os.Bundle;
import android.os.Handler;

import org.xmlpull.v1.XmlSerializer;

public class MainActivity extends AppCompatActivity implements  ActivityCompat.OnRequestPermissionsResultCallback{

    private static final int XML_TYPE = 10, GPX_TYPE=11;
    private static final int MULTIPLE_PERMISSIONS = 4;
    SharedPreferences mPref;
    SharedPreferences.Editor medit;
    Double latitude, longitude;
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    Button captureButton;
    public static final int MEDIA_TYPE_VIDEO = 2;
    Camera.Parameters params;
    private boolean isRecording = false;
    public static final String TAG = "MainActivityError";
    private FrameLayout preview;
    private static String xmlFileName;
    long startTime = 0;
    int seconds =0;

    String[] permissions= new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };

    //runs without a timer by reposting this handler at the end of the runnable

    private SimpleDateFormat sdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preview =
                findViewById(R.id.camera_preview);
        if(checkPermissions()) createCamera();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        medit = mPref.edit();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    }

    private void createCamera(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Camera permission has not been granted.
            Toast.makeText(getApplicationContext(), "Please Grant Permissions", Toast.LENGTH_SHORT).show();
        } else {

            // Camera permissions is already available, show the camera preview.
            Log.i(TAG,
                    "CAMERA permission has already been granted. Displaying camera preview.");
            mCamera=null;
            mCamera = getCameraInstance();
            params = mCamera.getParameters();
            mPreview = new CameraPreview(this, mCamera);
            preview.addView(mPreview);
        }
        // Add a listener to the Capture button
        captureButton = findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isRecording) {
                            // stop recording and release camera

                            mMediaRecorder.stop();  // stop the recording
                            releaseMediaRecorder(); // release the MediaRecorder object
                            mCamera.lock();         // take camera access back from MediaRecorder

                            // inform the user that recording has stopped
                            captureButton.setText("Capture");
                            isRecording = false;

                            Intent myService = new Intent(MainActivity.this, LocationTask.class);
                            stopService(myService);

                            medit.putString("service", "").commit();
                            GpxDbHelper gpxDbHelper=GpxDbHelper.getInstance(MainActivity.this);
                            gpxDbHelper.open();
                            ArrayList<Gpx> gpxdata=gpxDbHelper.getGPXData();
                            gpxDbHelper.deleteAllData();
                            gpxDbHelper.close();
                            createXmlFromGpxdata(gpxdata);
                            createGPXFromGpxdata(gpxdata);
                            Toast.makeText(getApplicationContext(),"Service Stopped",Toast.LENGTH_LONG).show();
                        }
                        else {
                            // initialize video camera
                            if (prepareVideoRecorder()) {
                                // Camera is available and unlocked, MediaRecorder is prepared,
                                // now you can start recording
                                mMediaRecorder.start();

                                // inform the user that recording has started
                                captureButton.setText("Stop");
                                isRecording = true;
                                if (mPref.getString("service", "").matches("")) {
                                    medit.putString("service", "service").commit();

                                    Intent intent = new Intent(getApplicationContext(), LocationTask.class);
                                    startService(intent);
                                    Toast.makeText(getApplicationContext(), "Service Started", Toast.LENGTH_SHORT).show();
                                } else {
                                    Intent intent = new Intent(getApplicationContext(), LocationTask.class);
                                    stopService(intent);
                                    startService(intent);
                                    //Toast.makeText(getApplicationContext(), "Service is already running", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else {
                                // prepare didn't work, release the camera
                                Log.d(TAG," prepare didn't work");
                                releaseMediaRecorder();
                                // inform user
                            }
                        }
                    }
                }
        );
    }
     //A safe way to get an instance of the Camera object.
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.d(TAG,"Camera Not Exists "+e.toString());
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    @SuppressLint("NewApi")
    private boolean prepareVideoRecorder(){

        if(mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        //mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncodingBitRate(196608);

        mMediaRecorder.setVideoSize(640, 480);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoEncodingBitRate(15000000);
       /* mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);*/


        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "VideoRecorder");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
         if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
            xmlFileName=timeStamp;
         }else if(type==XML_TYPE){
             mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                     "VID_"+ xmlFileName + ".xml");
         }
         else if(type==GPX_TYPE){
             mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                     "VID_"+ xmlFileName + ".gpx");
         }
             else{
            return null;
        }

        return mediaFile;
    }
    private void createGPXFromGpxdata(ArrayList<Gpx> gpsdata){
        String ns_gpx = "http://www.topografix.com/GPX/1/1";
        String ns_ulogger = "https://github.com/bfabiszewski/ulogger-android/1";
        String ns_xsi = "http://www.w3.org/2001/XMLSchema-instance";
        String schemaLocation = ns_gpx + " http://www.topografix.com/GPX/1/1/gpx.xsd " +
                ns_ulogger + " https://raw.githubusercontent.com/bfabiszewski/ulogger-server/master/scripts/gpx_extensions1.xsd";

        File newxmlfile =getOutputMediaFile(GPX_TYPE);
        try {
            newxmlfile.createNewFile();
        } catch (IOException e) {
            Log.e("IOException", "Exception in create new File(");
        }

        FileOutputStream fileos = null;
        try{
            fileos = new FileOutputStream(newxmlfile);

        } catch(FileNotFoundException e) {
            Log.e("FileNotFoundException",e.toString());
        }
        XmlSerializer serializer = Xml.newSerializer();
        try{
            serializer.setOutput(fileos, "UTF-8");
            serializer.startDocument("UTF-8", true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.setPrefix("xsi", ns_xsi);

            serializer.startTag(null, "gpx");
            serializer.attribute(null, "xmlns", ns_gpx);
            serializer.attribute(ns_xsi, "schemaLocation", schemaLocation);
            serializer.attribute(null, "version", "1.1");
            String creator = getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME;
            serializer.attribute(null, "creator", creator);
            serializer.startTag(null, "metadata");

            writeTag(serializer, "name", xmlFileName);
            writeTag(serializer, "time", gpsdata.get(0).getTime());
            serializer.endTag(null, "metadata");

            // track
            serializer.startTag(null, "trk");
            writeTag(serializer, "name", xmlFileName);
            serializer.startTag(null, "trkseg");
            for (Gpx data : gpsdata) {
                serializer.startTag(null, "trkpt");
                serializer.attribute(null, "lat", data.getLat());
                serializer.attribute(null, "lon", data.getLon());

                writeTag(serializer, "time", data.getTime());
                writeTag(serializer, "speed", data.getSpeed()+ " km/h");

                // ulogger extensions (accuracy, speed, bearing, provider)

                serializer.endTag(null, "trkpt");
            }
            serializer.endTag(null, "trkseg");
            serializer.endTag(null, "trk");
            serializer.endTag(null, "gpx");
            serializer.endDocument();
            serializer.flush();
            fileos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void writeTag(@NonNull XmlSerializer serializer, @NonNull String name, @NonNull String text)
            throws IOException, IllegalArgumentException, IllegalStateException {
        serializer.startTag(null, name);
        serializer.text(text);
        serializer.endTag(null, name);
    }

    private void createXmlFromGpxdata(ArrayList<Gpx> gpsdata){
        File newxmlfile =getOutputMediaFile(XML_TYPE);
        try {
            newxmlfile.createNewFile();
        } catch (IOException e) {
            Log.e("IOException", "Exception in create new File(");
        }

        FileOutputStream fileos = null;
        try{
            fileos = new FileOutputStream(newxmlfile);

        } catch(FileNotFoundException e) {
            Log.e("FileNotFoundException",e.toString());
        }
        XmlSerializer serializer = Xml.newSerializer();

        try {
            int time=-1;
            serializer.setOutput(fileos, "UTF-8");
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "root");
            for(Gpx data: gpsdata) {
                if(Integer.parseInt(data.getTime())>time) {
                    time = Integer.parseInt(data.getTime());
                    serializer.startTag(null, "position");
                    serializer.startTag(null, "date");
                    serializer.text(data.getId());
                    serializer.endTag(null, "date");
                    serializer.startTag(null, "x_loc");
                    serializer.text(data.getLat());
                    serializer.endTag(null, "x_loc");
                    serializer.startTag(null, "time");
                    serializer.text(data.getTime());
                    serializer.endTag(null, "time");
                    serializer.startTag(null, "y_loc");
                    serializer.text(data.getLon());
                    serializer.endTag(null, "y_loc");
                    serializer.startTag(null, "speed");
                    serializer.text(data.getSpeedwithUnit());
                    serializer.endTag(null, "speed");
                    serializer.endTag(null, "position");
                }
            }
            serializer.endTag(null,"root");
            serializer.endDocument();
            serializer.flush();
            fileos.close();
            //TextView tv = (TextView)findViewById(R.);

        } catch(Exception e) {
            Log.e("Exception","Exception occured in writing");
        }
    }

    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String speed=intent.getStringExtra("speed");
            int time=intent.getIntExtra("time",0);
            latitude = Double.valueOf(intent.getStringExtra("latutide"));
            longitude = Double.valueOf(intent.getStringExtra("longitude"));

            GpxDbHelper gpxDbHelper= GpxDbHelper.getInstance(MainActivity.this);
            gpxDbHelper.open();

            gpxDbHelper.addGps(sdf.format(new Date()),latitude,longitude,time,speed);

            gpxDbHelper.close();

        }
    };
    
    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();

        unregisterReceiver(broadcastReceiver);// release the camera immediately on pause event
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(LocationTask.str_receiver));
        try
        {
            mCamera.setPreviewCallback(null);
            mCamera = getCameraInstance();
            //mCamera.setPreviewCallback(null);
            mPreview = new CameraPreview(MainActivity.this, mCamera);//set preview
            preview.addView(mPreview);

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }
    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mPreview.getHolder().removeCallback(mPreview);
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:{
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissions) {
                        if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                            permissionsDenied += "\n" + per;

                        }

                    }
                    // Show permissionsDenied
                    if(permissionsDenied.length()>0)
                    new AlertDialog.Builder(this)
                            .setMessage("cannot proceed, permission denied:"+permissionsDenied)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            })
                            .create()
                            .show();
                    else{
                        createCamera();
                    }
                }
                return;
            }
        }
    }

}
