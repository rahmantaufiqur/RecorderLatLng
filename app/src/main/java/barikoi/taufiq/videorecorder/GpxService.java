package barikoi.taufiq.videorecorder;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GpxService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_GPX = "barikoi.taufiq.videorecorder.action.GPX";
    private static final String ACTION_XML = "barikoi.taufiq.videorecorder.action.XML";
    private static final String ACTION_CLEAR = "barikoi.taufiq.videorecorder.action.CLEAR";
    private static final String TAG="GpxService";

    public static final int MEDIA_TYPE_VIDEO = 2,XML_TYPE = 10, GPX_TYPE=11;
    private static String TIMESTAMP="barikoi.taufiq.videorecorder.action.TIMESTAMP";
    private String xmlFileName;
    private GpxDbHelper db;

    public GpxService() {
        super("GpxService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db=GpxDbHelper.getInstance();
        db.open(this);
    }
    @Override
    public void onDestroy() {
        { Log.d(TAG, "[gpx export stop]"); }
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }
    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionGPX(Context context,String timeStamp) {
        Intent intent = new Intent(context, GpxService.class);
        intent.setAction(ACTION_GPX);
        intent.putExtra(TIMESTAMP,timeStamp);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionXml(Context context,String timeStamp) {
        Intent intent = new Intent(context, GpxService.class);
        intent.setAction(ACTION_XML);
        intent.putExtra(TIMESTAMP,timeStamp);
        context.startService(intent);
    }
    public static void startActionClear(Context context) {
        Intent intent = new Intent(context, GpxService.class);
        intent.setAction(ACTION_CLEAR);

        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_GPX.equals(action)) {

                createGPXFromGpxdata(intent.getStringExtra(TIMESTAMP));
            } else if (ACTION_XML.equals(action)) {

                createXmlFromGpxdata(intent.getStringExtra(TIMESTAMP));
            } else if (ACTION_CLEAR.equals(action)){
                db.deleteAllData();
            }
        }
    }
    private File getOutputMediaFile(int type,String timeStamp){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "VideoRecorder");
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
    private void createGPXFromGpxdata(String timeStamp){

        ArrayList<Gpx> gpsdata = db.getGPXData();

        String ns_gpx = "http://www.topografix.com/GPX/1/1";
        String ns_ulogger = "https://github.com/bfabiszewski/ulogger-android/1";
        String ns_xsi = "http://www.w3.org/2001/XMLSchema-instance";
        String schemaLocation = ns_gpx + " http://www.topografix.com/GPX/1/1/gpx.xsd " +
                ns_ulogger + " https://raw.githubusercontent.com/bfabiszewski/ulogger-server/master/scripts/gpx_extensions1.xsd";

        File newxmlfile =getOutputMediaFile(GPX_TYPE,timeStamp);
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
            serializer.startTag(null, "name");
            serializer.text( timeStamp);
            serializer.endTag(null, "name");
            serializer.startTag(null, "time");
            serializer.text( gpsdata.get(0).getId());
            serializer.endTag(null, "time");
            serializer.endTag(null, "metadata");

            // track
            serializer.startTag(null, "trk");
            serializer.startTag(null, "name");
            serializer.text( timeStamp);
            serializer.endTag(null, "name");
            serializer.startTag(null, "trkseg");
            for (Gpx data : gpsdata) {
                serializer.startTag(null, "trkpt");
                serializer.attribute(null, "lat", data.getLat());
                serializer.attribute(null, "lon", data.getLon());
                serializer.startTag(null, "time");
                serializer.text( data.getId());
                serializer.endTag(null, "time");
                serializer.startTag(null, "speed");
                serializer.text( data.getSpeed()+ " km/h");
                serializer.endTag(null, "speed");

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
    /*private void writeTag(@NonNull XmlSerializer serializer, @NonNull String name, @NonNull String text)
            throws IOException, IllegalArgumentException, IllegalStateException {
        serializer.startTag(null, name);
        serializer.text(text);
        serializer.endTag(null, name);
    }*/

    private void createXmlFromGpxdata(String timeStamp){
        ArrayList<Gpx> gpsdata=db.getGPXData();

        File newxmlfile =getOutputMediaFile(XML_TYPE,timeStamp);
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

}
