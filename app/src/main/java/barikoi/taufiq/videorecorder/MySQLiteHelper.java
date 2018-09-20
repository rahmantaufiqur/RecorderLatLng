package barikoi.taufiq.videorecorder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Created by Sakib on 19-Jan-17.
 */

public class MySQLiteHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "gpx";
    private static final String TABLE_NAME="gpsData";
    //private static final String REWARD_TABLE_NAME="Place";
    private static final String KEY_TIME= "time";
    private static final String KEY_LON="lon";
    private static final String KEY_LAT="lat";
    private static final String KEY_ID="ID";

    private static MySQLiteHelper instance;

    public MySQLiteHelper(Context context) {
        super(context.getApplicationContext(),  DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static MySQLiteHelper getInstance(Context context){
        if(instance==null){
            instance=new MySQLiteHelper(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_OWN_PLACE_TABLE = "CREATE TABLE "+TABLE_NAME+"(" +
                KEY_ID+" TEXT, "+
                KEY_TIME+" TEXT, "+
                KEY_LON+" TEXT, "+
                KEY_LAT+" TEXT);";

        // create books table
        db.execSQL(CREATE_OWN_PLACE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);


        // create fresh books table
        this.onCreate(db);

    }


}
