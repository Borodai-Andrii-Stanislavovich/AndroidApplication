package com.example.myfirstapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, "RadioDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE STATIONS (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME TEXT, URL TEXT)");
        db.execSQL("INSERT INTO STATIONS (NAME, URL) VALUES ('Kiss FM', 'http://online.kissfm.ua/KissFM')");
        db.execSQL("INSERT INTO STATIONS (NAME, URL) VALUES ('Radio ROKS', 'http://195.95.206.13:8000/RadioROKS')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS STATIONS");
        onCreate(db);
    }

    public void addStation(String name, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("NAME", name);
        cv.put("URL", url);
        db.insert("STATIONS", null, cv);
    }

    public Cursor getAllStations() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM STATIONS", null);
    }
}