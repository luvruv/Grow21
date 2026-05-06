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

import com.example.grow21.api.ApiClient;
import com.example.grow21.api.Grow21ApiService;
import com.example.grow21.api.SyncRequest;
import com.example.grow21.api.SyncResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "grow21.db";
    private static final int DATABASE_VERSION = 3;

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
                + COL_SESSION_DATE + " TEXT NOT NULL, "
                + "is_synced INTEGER DEFAULT 0)";

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
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_SESSION + " ADD COLUMN is_synced INTEGER DEFAULT 0");
        } else {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHILD);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSION);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERFORMANCE);
            onCreate(db);
        }
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
        values.put("is_synced", 0);
        
        long result = db.insert(TABLE_SESSION, null, values);

        syncUnsyncedSessions();

        return result;
    }

    public void syncUnsyncedSessions() {
        int childId = 1;
        if (mContext != null) {
            android.content.SharedPreferences prefs = mContext.getSharedPreferences("grow21_prefs", Context.MODE_PRIVATE);
            childId = prefs.getInt("SERVER_CHILD_ID", 1);
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SESSION, null, "is_synced = 0", null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                Grow21ApiService apiService = ApiClient.getClient().create(Grow21ApiService.class);
                do {
                    long sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SESSION_ID));
                    String gameType = cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_GAME_TYPE));
                    int score = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SESSION_SCORE));
                    int total = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SESSION_TOTAL));

                    SyncRequest request = new SyncRequest(childId, gameType, score, total, 10);
                    apiService.syncSession(request).enqueue(new Callback<SyncResponse>() {
                        @Override
                        public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                SQLiteDatabase wdb = DatabaseHelper.this.getWritableDatabase();
                                ContentValues cv = new ContentValues();
                                cv.put("is_synced", 1);
                                wdb.update(TABLE_SESSION, cv, COL_SESSION_ID + "=?", new String[]{String.valueOf(sessionId)});
                                System.out.println("Sync Success for offline session: " + sessionId);
                            }
                        }

                        @Override
                        public void onFailure(Call<SyncResponse> call, Throwable t) {
                            System.out.println("Backend Sync Error: " + t.getMessage());
                        }
                    });
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
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

    // ==================== Completion & Accuracy Methods ====================

    /**
     * Returns the total number of distinct game categories that have been played.
     * Used to determine if ALL games are complete.
     */
    public int getPlayedCategoryCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(DISTINCT " + COL_SESSION_GAME_TYPE + ") FROM " + TABLE_SESSION, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * Total number of distinct game types available in the app.
     * If the child has played all of these, they are "fully completed".
     */
    public static final int TOTAL_GAME_CATEGORIES = 7;
    // Trace Lines, Draw Shapes, Free Draw, Image-based MCQ, 
    // Color Sorting, Memory Flip, Drag and Match

    /**
     * Returns true only when ALL game categories have been played at least once.
     */
    public boolean isAllGamesCompleted() {
        return getPlayedCategoryCount() >= TOTAL_GAME_CATEGORIES;
    }

    /**
     * Calculates overall accuracy from QuestionPerformance table.
     * Only counts categories that have graded questions (excludes free_draw).
     */
    public float getOverallAccuracy() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) as total, SUM(" + COL_PERF_IS_CORRECT + ") as correct FROM " + TABLE_PERFORMANCE;
        Cursor cursor = db.rawQuery(query, null);
        float accuracy = 0;
        if (cursor != null && cursor.moveToFirst()) {
            int total = cursor.getInt(cursor.getColumnIndexOrThrow("total"));
            int correct = cursor.getInt(cursor.getColumnIndexOrThrow("correct"));
            if (total > 0) {
                accuracy = Math.round(((float) correct / total) * 100);
            }
            cursor.close();
        }
        return (int) accuracy;
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
