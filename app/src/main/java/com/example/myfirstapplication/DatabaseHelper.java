package com.example.myfirstapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "flower_shop.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ORDERS = "orders";
    public static final String COL_ORDER_ID = "id";
    public static final String COL_FLOWER_NAME = "flower_name";
    public static final String COL_COLOR_ID = "color_id";
    public static final String COL_PRICE_ID = "price_id";

    public static final String TABLE_COLORS = "colors";
    public static final String TABLE_PRICES = "prices";
    public static final String COL_REF_ID = "id";
    public static final String COL_VALUE = "value";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_COLORS + " (" + COL_REF_ID + " INTEGER PRIMARY KEY, " + COL_VALUE + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_PRICES + " (" + COL_REF_ID + " INTEGER PRIMARY KEY, " + COL_VALUE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_ORDERS + " (" +
                COL_ORDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FLOWER_NAME + " TEXT, " +
                COL_COLOR_ID + " INTEGER, " +
                COL_PRICE_ID + " INTEGER)");

        fillReferenceData(db);
    }

    private void fillReferenceData(SQLiteDatabase db) {
        String[] colors = {"Червоні", "Білі", "Жовті"};
        for (int i = 0; i < colors.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(COL_REF_ID, i);
            cv.put(COL_VALUE, colors[i]);
            db.insert(TABLE_COLORS, null, cv);
        }

        String[] prices = {"До 300 грн", "300–700 грн", "Понад 700 грн"};
        for (int i = 0; i < prices.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(COL_REF_ID, i);
            cv.put(COL_VALUE, prices[i]);
            db.insert(TABLE_PRICES, null, cv);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLORS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRICES);
        onCreate(db);
    }

    public void insertOrder(String name, int colorId, int priceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FLOWER_NAME, name);
        cv.put(COL_COLOR_ID, colorId);
        cv.put(COL_PRICE_ID, priceId);
        db.insert(TABLE_ORDERS, null, cv);
    }

    public Cursor getOrdersWithDetails() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT o." + COL_ORDER_ID + ", o." + COL_FLOWER_NAME + ", c." + COL_VALUE + ", p." + COL_VALUE +
                " FROM " + TABLE_ORDERS + " o " +
                " JOIN " + TABLE_COLORS + " c ON o." + COL_COLOR_ID + " = c." + COL_REF_ID +
                " JOIN " + TABLE_PRICES + " p ON o." + COL_PRICE_ID + " = p." + COL_REF_ID;
        return db.rawQuery(query, null);
    }
}