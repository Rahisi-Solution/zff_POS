package com.rahisi.zffboarding;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

public class TicketDatabaseAdapter {
    TicketDatabaseHelper ticketDatabaseHelper;

    public TicketDatabaseAdapter(Context context) {
        ticketDatabaseHelper = new TicketDatabaseHelper(context);
    }

    public long insertData (String booking_id, String ticket_number, String passenger, String age, String date, String time, String boarding, int scanned, String status, int count) {
        SQLiteDatabase db = ticketDatabaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Config.BOOKING_ID, booking_id);
        contentValues.put(Config.TICKET_NUMBER, ticket_number);
        contentValues.put(Config.PASSENGER_NAME, passenger);
        contentValues.put(Config.PASSENGER_AGE, age);
        contentValues.put(Config.DEPARTURE_DATE, date);
        contentValues.put(Config.DEPARTURE_TIME, time);
        contentValues.put(Config.BOARDING, boarding);
        contentValues.put(Config.TICKET_SCANNED, scanned);
        contentValues.put(Config.TICKET_STATUS, status);
        contentValues.put(Config.TICKET_SCAN_COUNT, count);

        long id = db.insert(Config.TICKET_TABLE_NAME, null, contentValues);

        return id;
    }

    public JSONArray getAllTikets() {
        SQLiteDatabase db = ticketDatabaseHelper.getWritableDatabase();
        String[] columns = {Config.TICKET_ID, Config.BOOKING_ID, Config.TICKET_NUMBER, Config.PASSENGER_NAME, Config.PASSENGER_AGE,
                Config.DEPARTURE_DATE, Config.DEPARTURE_TIME, Config.BOARDING, Config.TICKET_SCANNED, Config.TICKET_STATUS, Config.TICKET_SCAN_COUNT};
        Cursor cursor = db.query(Config.TICKET_TABLE_NAME, columns, null, null, null, null, null);
        JSONArray tickets = new JSONArray();
        while (cursor.moveToNext()) {
            JSONObject ticket = new JSONObject();
            try {
                for (String column: columns) {
                    ticket.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
                tickets.put(ticket);
            }  catch (Exception e) {
                System.out.print(e.getMessage());
            }
        }

        cursor.close();
        return tickets;
    }

    public JSONObject getTicket(String id) {
        SQLiteDatabase db = ticketDatabaseHelper.getWritableDatabase();
        String[] columns = {Config.TICKET_ID, Config.BOOKING_ID, Config.TICKET_NUMBER, Config.PASSENGER_NAME, Config.PASSENGER_AGE,
                Config.DEPARTURE_DATE, Config.DEPARTURE_TIME, Config.BOARDING, Config.TICKET_SCANNED, Config.TICKET_STATUS, Config.TICKET_SCAN_COUNT};
        Cursor cursor = db.query(Config.TICKET_TABLE_NAME, columns, Config.TICKET_NUMBER + "=?", new String[]{id}, null, null, null);

        cursor.moveToFirst();
        JSONObject ticket = new JSONObject();
        try {
            for (String column: columns) {
                ticket.put(column, cursor.getString(cursor.getColumnIndex(column)));
            }
        }  catch (Exception e) {
            System.out.print(e.getMessage());
        }
        cursor.close();
        return ticket;
    }

    public int update(String id, int counter) {
        SQLiteDatabase db = ticketDatabaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Config.TICKET_SCANNED, 1);
        contentValues.put(Config.BOARDING, "YES");
        contentValues.put(Config.TICKET_SCAN_COUNT, counter);
        String[] whereArgs = {id};

        int count = db.update(Config.TICKET_TABLE_NAME, contentValues, Config.TICKET_ID + " = ?", whereArgs);

        return count;
    }

    public int delete(String id) {
        SQLiteDatabase db = ticketDatabaseHelper.getWritableDatabase();
        String[] whereArgs = {id};

        int count = db.delete(Config.TICKET_TABLE_NAME, Config.TICKET_ID + " = ?", whereArgs);

        return count;
    }

    public int clearDatabase() {
        SQLiteDatabase db = ticketDatabaseHelper.getWritableDatabase();
        int count = db.delete(Config.TICKET_TABLE_NAME, null, null);

        return count;
    }

    static class TicketDatabaseHelper extends SQLiteOpenHelper {
        public static final String CREATE_TABLE = "CREATE TABLE " + Config.TICKET_TABLE_NAME +
                " (" + Config.TICKET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Config.BOOKING_ID + " TEXT, " + Config.TICKET_NUMBER + " TEXT, " +
                Config.PASSENGER_NAME + " TEXT, " + Config.PASSENGER_AGE + " TEXT, " +
                Config.DEPARTURE_DATE + " TEXT, " + Config.DEPARTURE_TIME + " TEXT, " +
                Config.BOARDING + " TEXT, " + Config.TICKET_SCANNED + " INTEGER, " +
                Config.TICKET_STATUS + " TEXT, " + Config.TICKET_SCAN_COUNT + " INTEGER);";

        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + Config.TICKET_TABLE_NAME;

        public Context context;

        public TicketDatabaseHelper(Context context) {
            super(context, Config.TICKET_DATABASE_NAME, null, Config.TICKET_DATABASE_VERSION);
            this.context = context;
        }

        public void onCreate (SQLiteDatabase db) {
            try {
                db.execSQL(CREATE_TABLE);
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                db.execSQL(DROP_TABLE);
                onCreate(db);
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
        }
    }
}
