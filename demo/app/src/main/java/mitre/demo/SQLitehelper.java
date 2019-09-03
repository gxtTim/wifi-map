package mitre.demo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.Toast;

public class SQLitehelper extends SQLiteOpenHelper {
    Context c;

    private static final String TABLE_NAME = "map_table";
    private static final String COL = "map";

    public SQLitehelper (Context context) {
        super(context, TABLE_NAME, null, 1);
        c = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            String sql = "CREATE TABLE " + TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         COL + " TEXT)";
            db.execSQL(sql);
            Toast.makeText(c, "Table Created Successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("MyDB", "Table Creation Error ", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    //Persistence Logic for Record Insertion into Database Table
    public boolean addData(String map) {
        try {
            String sql = "INSERT INTO " + TABLE_NAME +"(" + COL + ")" +
                         " VALUES(" + "'" + map + "')";
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(sql);
            Toast.makeText(c, "Table Inserted Successfully", Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception e) {
            Log.e("MyDB", "Record Insertion Failed ", e);
            return false;
        }
    }

    public Cursor getData(){
        SQLiteDatabase db = getWritableDatabase();
        String sql = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(sql, null);
        return data;
    }
}
