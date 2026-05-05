package com.example.grow21;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.grow21.models.CategoryProgress;
import com.example.grow21.models.Child;
import com.example.grow21.models.Session;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "grow21.db";
    private static final int DATABASE_VERSION = 2;

    // User table
    private static final String TABLE_USER = "User";
    private static final String COL_USER_ID = "id";
    private static final String COL_USER_EMAIL = "email";
    private static final String COL_USER_PASSWORD = "password";

    // Child table
    private static final String TABLE_CHILD = "Child";
    private static final String COL_CHILD_ID = "id";
    private static final String COL_CHILD_NAME = "name";
    private static final String COL_CHILD_AGE = "age";
    private static final String COL_CHILD_AVATAR = "avatar";

    // Session table
    private static final String TABLE_SESSION = "Session";
    private static final String COL_SESSION_ID = "id";
    private static final String COL_SESSION_GAME_TYPE = "game_type";
    private static final String COL_SESSION_SCORE = "score";
    private static final String COL_SESSION_TOTAL = "total";
    private static final String COL_SESSION_DATE = "date";

    // QuestionPerformance table
    private static final String TABLE_PERFORMANCE = "QuestionPerformance";
    private static final String COL_PERF_ID = "id";
    private static final String COL_PERF_QUESTION_ID = "question_id";
    private static final String COL_PERF_CATEGORY = "category";
    private static final String COL_PERF_IS_CORRECT = "is_correct";
    private static final String COL_PERF_TIMESTAMP = "timestamp";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private Context mContext;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USER + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_EMAIL + " TEXT NOT NULL UNIQUE, "
                + COL_USER_PASSWORD + " TEXT NOT NULL)";

        String createChildTable = "CREATE TABLE " + TABLE_CHILD + " ("
                + COL_CHILD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CHILD_NAME + " TEXT NOT NULL, "
                + COL_CHILD_AGE + " INTEGER NOT NULL, "
                + COL_CHILD_AVATAR + " INTEGER DEFAULT 0)";

        String createSessionTable = "CREATE TABLE " + TABLE_SESSION + " ("
                + COL_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_SESSION_GAME_TYPE + " TEXT NOT NULL, "
                + COL_SESSION_SCORE + " INTEGER NOT NULL, "
                + COL_SESSION_TOTAL + " INTEGER NOT NULL, "
                + COL_SESSION_DATE + " TEXT NOT NULL)";

        String createPerformanceTable = "CREATE TABLE " + TABLE_PERFORMANCE + " ("
                + COL_PERF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PERF_QUESTION_ID + " TEXT NOT NULL, "
                + COL_PERF_CATEGORY + " TEXT NOT NULL, "
                + COL_PERF_IS_CORRECT + " INTEGER NOT NULL, "
                + COL_PERF_TIMESTAMP + " TEXT NOT NULL)";

        db.execSQL(createUserTable);
        db.execSQL(createChildTable);
        db.execSQL(createSessionTable);
        db.execSQL(createPerformanceTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHILD);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERFORMANCE);
        onCreate(db);
    }

    // ==================== Child Methods ====================

    public long insertChild(String name, int age, int avatar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CHILD_NAME, name);
        values.put(COL_CHILD_AGE, age);
        values.put(COL_CHILD_AVATAR, avatar);
        long result = db.insert(TABLE_CHILD, null, values);
        return result;
    }

    public Child getChild() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHILD, null, null, null, null, null,
                COL_CHILD_ID + " DESC", "1");
        Child child = null;
        if (cursor != null && cursor.moveToFirst()) {
            child = new Child();
            child.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_CHILD_ID)));
            child.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHILD_NAME)));
            child.setAge(cursor.getInt(cursor.getColumnIndexOrThrow(COL_CHILD_AGE)));
            child.setAvatar(cursor.getInt(cursor.getColumnIndexOrThrow(COL_CHILD_AVATAR)));
            cursor.close();
        }
        return child;
    }

    public boolean childExists() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CHILD, null);
        boolean exists = false;
        if (cursor != null && cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
            cursor.close();
        }
        return exists;
    }

    // ==================== Session Methods ====================

    public long insertSession(String gameType, int score, int total) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SESSION_GAME_TYPE, gameType);
        values.put(COL_SESSION_SCORE, score);
        values.put(COL_SESSION_TOTAL, total);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        values.put(COL_SESSION_DATE, sdf.format(new Date()));
        
        long result = db.insert(TABLE_SESSION, null, values);

        // SYNC WITH NODE.JS BACKEND FOR CRM
        new Thread(() -> {
            try {
                // Get dynamic ChildID from SharedPreferences set during LoginActivity
                int childId = 1;
                if (mContext != null) {
                    android.content.SharedPreferences prefs = mContext.getSharedPreferences("grow21_prefs", Context.MODE_PRIVATE);
                    childId = prefs.getInt("SERVER_CHILD_ID", 1);
                }
                
                java.net.URL url = new java.net.URL("http://" + BuildConfig.BACKEND_IP + ":5000/api/app-sync");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                String json = "{\"childId\":" + childId + ",\"gameType\":\"" + gameType + "\",\"score\":" + score + ",\"total\":" + total + "}";
                java.io.OutputStream os = conn.getOutputStream();
                os.write(json.getBytes());
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                System.out.println("Backend Sync Response: " + responseCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return result;
    }

    public List<Session> getAllSessions() {
        List<Session> sessions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SESSION, null, null, null, null, null,
                COL_SESSION_DATE + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Session session = new Session();
                session.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SESSION_ID)));
                session.setGameType(cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_GAME_TYPE)));
                session.setScore(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SESSION_SCORE)));
                session.setTotal(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SESSION_TOTAL)));
                session.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_DATE)));
                sessions.add(session);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return sessions;
    }

    // ==================== Performance Methods ====================

    public long insertPerformance(String questionId, String category, boolean correct) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PERF_QUESTION_ID, questionId);
        values.put(COL_PERF_CATEGORY, category);
        values.put(COL_PERF_IS_CORRECT, correct ? 1 : 0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        values.put(COL_PERF_TIMESTAMP, sdf.format(new Date()));
        return db.insert(TABLE_PERFORMANCE, null, values);
    }

    public List<CategoryProgress> getProgressByCategory() {
        List<CategoryProgress> progressList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_PERF_CATEGORY + ", "
                + "COUNT(*) as attempted, "
                + "SUM(" + COL_PERF_IS_CORRECT + ") as correct "
                + "FROM " + TABLE_PERFORMANCE + " "
                + "GROUP BY " + COL_PERF_CATEGORY;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_PERF_CATEGORY));
                int attempted = cursor.getInt(cursor.getColumnIndexOrThrow("attempted"));
                int correct = cursor.getInt(cursor.getColumnIndexOrThrow("correct"));
                float accuracy = attempted > 0 ? ((float) correct / attempted) * 100 : 0;
                CategoryProgress progress = new CategoryProgress(category, attempted, correct, accuracy);
                progressList.add(progress);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return progressList;
    }

    // ==================== Streak / Calendar Methods ====================

    /**
     * Returns a set of day-of-month integers that have at least one session
     * in the given year and month. Used by the streak calendar to highlight active days.
     *
     * @param year  the calendar year (e.g. 2026)
     * @param month the calendar month (1-12)
     * @return set of day numbers with activity
     */
    public java.util.Set<Integer> getActiveDaysForMonth(int year, int month) {
        java.util.Set<Integer> activeDays = new java.util.HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Session date format is "yyyy-MM-dd HH:mm:ss"
        // Build prefix for the month, e.g. "2026-04"
        String monthPrefix = String.format(Locale.getDefault(), "%04d-%02d", year, month);
        String query = "SELECT DISTINCT substr(" + COL_SESSION_DATE + ", 9, 2) as day_num "
                + "FROM " + TABLE_SESSION + " "
                + "WHERE " + COL_SESSION_DATE + " LIKE ?";
        Cursor cursor = db.rawQuery(query, new String[]{monthPrefix + "%"});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String dayStr = cursor.getString(cursor.getColumnIndexOrThrow("day_num"));
                try {
                    activeDays.add(Integer.parseInt(dayStr));
                } catch (NumberFormatException e) {
                    // Skip malformed entries
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return activeDays;
    }

    /**
     * Computes the current activity streak — the number of consecutive days
     * ending at today (or yesterday if no session today yet) that have at least
     * one session recorded.
     *
     * @return streak count (0 if no recent activity)
     */
    public int getCurrentStreak() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Get all distinct session dates sorted descending
        String query = "SELECT DISTINCT substr(" + COL_SESSION_DATE + ", 1, 10) as session_day "
                + "FROM " + TABLE_SESSION + " "
                + "ORDER BY session_day DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) cursor.close();
            return 0;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        // Start from today
        String today = sdf.format(cal.getTime());
        String firstDate = cursor.getString(cursor.getColumnIndexOrThrow("session_day"));

        // If the most recent session is not today or yesterday, streak is 0
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        String yesterday = sdf.format(cal.getTime());
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1); // reset to today

        if (!firstDate.equals(today) && !firstDate.equals(yesterday)) {
            cursor.close();
            return 0;
        }

        // If latest is yesterday, start checking from yesterday
        if (!firstDate.equals(today)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        }

        // Collect all distinct dates into a set for O(1) lookup
        java.util.Set<String> dateSet = new java.util.HashSet<>();
        do {
            dateSet.add(cursor.getString(cursor.getColumnIndexOrThrow("session_day")));
        } while (cursor.moveToNext());
        cursor.close();

        // Count consecutive days backwards
        int streak = 0;
        while (dateSet.contains(sdf.format(cal.getTime()))) {
            streak++;
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }
}
