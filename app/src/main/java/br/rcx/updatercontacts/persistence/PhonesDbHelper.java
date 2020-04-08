package br.rcx.updatercontacts.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PhonesDbHelper extends SQLiteOpenHelper {
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PhonesContract.PhonesEntry.TABLE_NAME + " (" +
                    PhonesContract.PhonesEntry._ID + " INTEGER PRIMARY KEY," +
                    PhonesContract.PhonesEntry.COLUMN_CONTACT_ID + " TEXT," +
                    PhonesContract.PhonesEntry.COLUMN_PHONE + " TEXT," +
                    PhonesContract.PhonesEntry.COLUMN_HAS_WHATS + " TEXT," +
                    PhonesContract.PhonesEntry.COLUMN_CREATED_AT + " TEXT,"+
                    PhonesContract.PhonesEntry.COLUMN_PROCESS_AT + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PhonesContract.PhonesEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "phones.db";

    public String[] fields = {
            BaseColumns._ID,
            PhonesContract.PhonesEntry.COLUMN_PHONE,
            PhonesContract.PhonesEntry.COLUMN_CONTACT_ID,
            PhonesContract.PhonesEntry.COLUMN_HAS_WHATS,
            PhonesContract.PhonesEntry.COLUMN_CREATED_AT,
            PhonesContract.PhonesEntry.COLUMN_PROCESS_AT
    };

    public PhonesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
//        db.execSQL(SQL_DELETE_ENTRIES);
//        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        onUpgrade(db, oldVersion, newVersion);
    }

    public long insert(String phone,String contact_id){
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(PhonesContract.PhonesEntry.COLUMN_PHONE, phone);
        values.put(PhonesContract.PhonesEntry.COLUMN_CONTACT_ID, contact_id);
        values.put(PhonesContract.PhonesEntry.COLUMN_HAS_WHATS, 0);
        values.put(PhonesContract.PhonesEntry.COLUMN_CREATED_AT, new Date().getTime());
        values.put(PhonesContract.PhonesEntry.COLUMN_PROCESS_AT, "");

        // Insert the new row, returning the primary key value of the new row
        try {
            long newRowId = db.insert(PhonesContract.PhonesEntry.TABLE_NAME, null, values);
            return newRowId;
        }catch (Exception e){
            return 0;
        }
    }

    public boolean update(String id,JSONObject object){
        try {
            SQLiteDatabase db = getWritableDatabase();
            // Create a new map of values, where column names are the keys

            ContentValues values = new ContentValues();
            if(object.has("phone")) {
                values.put(PhonesContract.PhonesEntry.COLUMN_PHONE, object.getString("phone"));
            }

            if(object.has("has_whats")) {
                values.put(PhonesContract.PhonesEntry.COLUMN_HAS_WHATS, object.getString("has_whats"));
            }

            if(object.has("process_at")) {
                values.put(PhonesContract.PhonesEntry.COLUMN_PROCESS_AT, object.getString("process_at"));
            }

            db.update(PhonesContract.PhonesEntry.TABLE_NAME, values, "_id=" + id, null);
            return true;
        }catch(Exception e){

        }
        return false;
    }

    public ArrayList<JSONObject> get(String selection, String[] selectionArgs) throws JSONException {
        SQLiteDatabase db = getWritableDatabase();

        //String selection = PhonesContract.PhonesEntry.COLUMN_NAME_TITLE + " = ?";
        //String[] selectionArgs = { "My Title" };
        //String sortOrder = FeedEntry.COLUMN_NAME_SUBTITLE + " DESC";

        Cursor cursor = db.query(
                PhonesContract.PhonesEntry.TABLE_NAME,   // The table to query
                fields,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order sortOrder
        );

        ArrayList<JSONObject> rowsObject = new ArrayList<JSONObject>();
        while(cursor.moveToNext()) {

            JSONObject rowObject = new JSONObject();
            for( int row=0 ;  row < cursor.getCount() ; row++ ) {
                for( int field=0;  field < fields.length ; field++ ) {
                    String k = cursor.getColumnName(field);
                    String v = cursor.getString(field);
                    rowObject.put(k,v);
                }
                rowsObject.add(rowObject);
            }
        }

        cursor.close();
        return rowsObject;
    }


}