package barikoi.taufiq.videorecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class GpxDbHelper {

    private static final String DATABASE_NAME = "gpx";
    private static final String TABLE_NAME="gpsData";
    //private static final String REWARD_TABLE_NAME="Place";
    private static final String KEY_TIME= "time";
    private static final String KEY_LON="lon";
    private static final String KEY_LAT="lat";
    private static final String KEY_ID="ID";
    private static final String KEY_SPEED="speed";
    private static GpxDbHelper instance;
    private static SQLiteDatabase db;
    private static MySQLiteHelper dbHelper;

    public GpxDbHelper(Context context){

        dbHelper=MySQLiteHelper.getInstance(context);
    }

    public static synchronized GpxDbHelper getInstance(Context context){
        if(instance==null){
            instance=new GpxDbHelper(context);
        }
        return instance;
    }

    public synchronized boolean isOpen(){
        return db.isOpen();
    }
    public synchronized void open() throws SQLException {
        db = dbHelper.getWritableDatabase();
    }

    public synchronized void close() {
        dbHelper.close();
    }

    public synchronized Boolean addGps(String id, double lat, double lon, int time,String speed){


        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_ID, id);
        values.put(KEY_TIME, time);
        values.put(KEY_LON, lon);
        values.put(KEY_LAT, lat);
        values.put(KEY_SPEED, speed);


        // 3. insert
        if(db.insert(TABLE_NAME, null, values)!=-1){
            return true;
        } // key/value -> keys = column names/ values = column values
        else{
            return false;
        }
        // 4. close

    }
    public synchronized ArrayList<Gpx> getGPXData(){
        String[] projection={
                KEY_ID,
                KEY_LON,
                KEY_LAT,
                KEY_TIME,
                KEY_SPEED
        };
        Gpx newplace=null;
        ArrayList<Gpx> places = new ArrayList<Gpx>();
        Cursor cursor = db.query(TABLE_NAME, projection, null,
                null, null, null, null, null);
        //Cursor cursor=db.rawQuery(selectquery,null);
        if (cursor.moveToFirst()) {
            do {
                newplace=new Gpx(cursor.getString(cursor.getColumnIndex(KEY_ID)),cursor.getString(cursor.getColumnIndex(KEY_LAT)),cursor.getString(cursor.getColumnIndex(KEY_LON)),cursor.getString(cursor.getColumnIndex(KEY_TIME)),Float.parseFloat(cursor.getString(cursor.getColumnIndex(KEY_SPEED))));
                places.add(newplace);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return places;

    }
    public void deleteAllData(){
        dbHelper.onUpgrade(db,1,1);
    }

}
