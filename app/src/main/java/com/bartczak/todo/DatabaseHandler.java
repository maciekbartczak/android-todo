package com.bartczak.todo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "todo";

    private static final String TABLE_TASKS = "tasks";

    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ATTACHMENT = "attachment";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_DUE_DATE = "due_date";
    private static final String KEY_DONE_AT = "done_at";
    private static final String KEY_DONE = "done";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_TASKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_TITLE + " TEXT,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_ATTACHMENT + " TEXT,"
                + KEY_CREATED_AT + " TEXT,"
                + KEY_DUE_DATE + " TEXT,"
                + KEY_DONE_AT + " TEXT,"
                + KEY_DONE + " BOOL,"
                + KEY_NOTIFICATION_ENABLED + " BOOL"
                + ")";
        db.execSQL(CREATE_TASKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }

    public void addTask(final Task task) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, task.getTitle());
        values.put(KEY_DESCRIPTION, task.getDescription());
        values.put(KEY_ATTACHMENT, task.getAttachmentPath());
        values.put(KEY_CREATED_AT, task.getCreatedAt().toString());
        values.put(KEY_DUE_DATE, task.getDueDate().toString());
        if (task.getDoneAt() != null) {
            values.put(KEY_DONE_AT, task.getDoneAt().toString());
        }
        values.put(KEY_DONE, task.isDone());
        values.put(KEY_NOTIFICATION_ENABLED, task.getNotificationEnabled());

        db.insert(TABLE_TASKS, null, values);
        db.close();
    }

    public List<Task> getAllTasks(boolean sortAscending) {
        List<Task> tasks = new ArrayList<>();
        final String SELECT_QUERY = "SELECT * FROM " + TABLE_TASKS
                + " ORDER BY " + KEY_DUE_DATE + " " + (sortAscending ? "ASC" : "DESC");

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(SELECT_QUERY, null);

        if (cursor.moveToFirst()) {
            do {
                tasks.add( createTaskFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return tasks;
    }

    public List<Task> getTasksByTitle(String query, boolean sortAscending) {
        List<Task> tasks = new ArrayList<>();
        final String SELECT_QUERY = "SELECT * FROM " + TABLE_TASKS + " WHERE " + KEY_TITLE + " LIKE '%" + query + "%'"
                + " ORDER BY " + KEY_DUE_DATE + " " + (sortAscending ? "ASC" : "DESC");

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(SELECT_QUERY, null);

        if (cursor.moveToFirst()) {
            do {
                tasks.add(createTaskFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return tasks;
    }

    public void deleteTask(final int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, KEY_ID + " = ?", new String[] { String.valueOf(id) });
        db.close();
    }

    public void updateTask(final Task task) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, task.getTitle());
        values.put(KEY_DESCRIPTION, task.getDescription());
        values.put(KEY_ATTACHMENT, task.getAttachmentPath());
        values.put(KEY_CREATED_AT, task.getCreatedAt().toString());
        values.put(KEY_DUE_DATE, task.getDueDate().toString());
        if (task.getDoneAt() != null) {
            values.put(KEY_DONE_AT, task.getDoneAt().toString());
        }
        values.put(KEY_DONE, task.isDone());
        values.put(KEY_NOTIFICATION_ENABLED, task.getNotificationEnabled());

        db.update(TABLE_TASKS, values, KEY_ID + " = ?", new String[] { String.valueOf(task.getId()) });
        db.close();
    }

    private Task createTaskFromCursor(Cursor cursor) {
        Task task = new Task();
        task.setId(cursor.getInt(0));
        task.setTitle(cursor.getString(1));
        task.setDescription(cursor.getString(2));
        task.setAttachmentPath(cursor.getString(3));
        task.setCreatedAt(LocalDateTime.parse(cursor.getString(4)));
        task.setDueDate(LocalDateTime.parse(cursor.getString(5)));
        if (cursor.getString(6) != null) {
            task.setDoneAt(LocalDateTime.parse(cursor.getString(6)));
        }
        task.setDone(cursor.getInt(7) == 1);
        task.setNotificationEnabled(cursor.getInt(8) == 1);
        return task;
    }
}
