package com.example.myfirstapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "LevelMarks.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_MARKS = "marks";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ANGLE_X = "angle_x";
    public static final String COLUMN_ANGLE_Y = "angle_y";
    public static final String COLUMN_DESC = "description";
    public static final String COLUMN_TIME = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_MARKS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ANGLE_X + " INTEGER, " +
                COLUMN_ANGLE_Y + " INTEGER, " +
                COLUMN_DESC + " TEXT, " +
                COLUMN_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARKS);
        onCreate(db);
    }

    public void addMark(int angleX, int angleY, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ANGLE_X, angleX);
        values.put(COLUMN_ANGLE_Y, angleY);
        values.put(COLUMN_DESC, description);

        long id = db.insert(TABLE_MARKS, null, values);
        db.close();
    }

    public Cursor getAllMarks() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_MARKS, null, null, null, null, null, COLUMN_TIME + " DESC");
    }

    public void updateMarkDescription(long id, String newDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DESC, newDescription);

        db.update(TABLE_MARKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteMark(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MARKS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}