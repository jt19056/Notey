package thomas.jonathan.notey;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;

public class MySQLiteHelper extends SQLiteOpenHelper {
    // table name
    private static final String TABLE = "NoteyTable";

    // columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NOTE = "note";
    private static final String KEY_ICON = "icon";
    private static final String KEY_SPINNER_LOC = "spinnerLoc";
    private static final String KEY_IMG_BTN_NUM = "imgBtnNum";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ICON_NAME = "iconName";
    private static final String KEY_ALARM = "alarm";

    private static final String[] COLUMNS = {KEY_ID,KEY_NOTE,KEY_ICON,KEY_SPINNER_LOC,KEY_IMG_BTN_NUM,KEY_TITLE,KEY_ICON_NAME,KEY_ALARM};
    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "notey.db";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE NoteyTable ( " +
                "id INTEGER PRIMARY KEY, " +
                "note TEXT, "+
                "icon INTEGER," +
                "spinnerLoc INTEGER," +
                "imgBtnNum INTEGER, " +
                "title TEXT, " +
                "iconName TEXT," +
                "alarm TEXT )";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(newVersion<=2) {
            try {
                db.execSQL("ALTER TABLE NoteyTable ADD COLUMN title TEXT"); //add the 'title' column if on old version of db
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
        if(newVersion<=3) {
            try {
                db.execSQL("ALTER TABLE NoteyTable ADD COLUMN iconName TEXT"); //add the 'iconName' column if on old version of db
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }

        if(newVersion<=4) {
            try {
                db.execSQL("ALTER TABLE NoteyTable ADD COLUMN alarm TEXT"); //add the 'alarm' column if on old version of db
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    }

    public void addNotey(NoteyNote notey){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, notey.getId());
        values.put(KEY_NOTE, notey.getNote());
        values.put(KEY_ICON, notey.getIcon());
        values.put(KEY_SPINNER_LOC, notey.getSpinnerLoc());
        values.put(KEY_IMG_BTN_NUM, notey.getImgBtnNum());
        values.put(KEY_TITLE, notey.getTitle());
        values.put(KEY_ICON_NAME, notey.getIconName());
        values.put(KEY_ALARM, notey.getAlarm());

        db.insert(TABLE, // table
                null, //nullColumnHack
                values); // key/value -> keys = column names/ values = column values

        db.close();
    }

    public NoteyNote getNotey(int id){
        SQLiteDatabase db = this.getReadableDatabase();

        //cursor used to get query results of db
        Cursor cursor =
                db.query(TABLE, //table
                        COLUMNS, //column names
                        " id = ?", //selections
                        new String[] { String.valueOf(id) }, //selections args
                        null, //group by
                        null, //having
                        null, //order by
                        null); //limit

        //if we got results get the first one
        if (cursor != null)
            cursor.moveToFirst();

        //build the object
        NoteyNote notey = new NoteyNote();
        notey.setId(Integer.parseInt(cursor.getString(0)));
        notey.setNote(cursor.getString(1));
        notey.setIcon(cursor.getInt(2));
        notey.setSpinnerLoc(cursor.getInt(3));
        notey.setImgBtnNum(cursor.getInt(4));
        notey.setTitle(cursor.getString(5));
        notey.setIconName(cursor.getString(6));
        notey.setAlarm(cursor.getString(7));

        return notey;
    }

    public List<NoteyNote> getAllNoteys() {
        List<NoteyNote> noteyList = new LinkedList<NoteyNote>();
        String query = "SELECT  * FROM " + TABLE;

        //get query
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        //go through each row, build notey and add it to list
        NoteyNote notey;
        if (cursor.moveToFirst()) {
            do {
                notey = new NoteyNote();
                notey.setId(Integer.parseInt(cursor.getString(0)));
                notey.setNote(cursor.getString(1));
                notey.setIcon(cursor.getInt(2));
                notey.setSpinnerLoc(cursor.getInt(3));
                notey.setImgBtnNum(cursor.getInt(4));
                notey.setTitle(cursor.getString(5));
                notey.setIconName(cursor.getString(6));
                notey.setAlarm(cursor.getString(7));

                noteyList.add(notey);
            } while (cursor.moveToNext());
        }
        return noteyList;
    }

    public int updateNotey(NoteyNote notey) {
        SQLiteDatabase db = this.getWritableDatabase();

        //create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_ID, notey.getId());
        values.put(KEY_NOTE, notey.getNote());
        values.put(KEY_ICON, notey.getIcon());
        values.put(KEY_SPINNER_LOC, notey.getSpinnerLoc());
        values.put(KEY_IMG_BTN_NUM, notey.getImgBtnNum());
        values.put(KEY_TITLE, notey.getTitle());
        values.put(KEY_ICON_NAME, notey.getIconName());
        values.put(KEY_ALARM, notey.getAlarm());

        //update row
        int i = db.update(TABLE, //table
                values, // column/value
                KEY_ID+" = ?", // selections
                new String[] { String.valueOf(notey.getId()) }); //selection args
        db.close();
        return i;
    }

    public void deleteNotey(NoteyNote notey) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE, //table name
                KEY_ID+" = ?",  // selections
                new String[] { String.valueOf(notey.getId()) }); //selections args
        db.close();
    }

    boolean checkIfExist(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        //get query
        try {
            cursor = db.query(TABLE, COLUMNS, KEY_ID + "=?",
                    new String[]{Integer.toString(id)}, null, null, null, null);
        }catch (SQLiteException e) {
            e.printStackTrace();
        }
            if (cursor.getCount() > 0) return true;
            else return false;

    }
}