package com.bartczak.todo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreferencesActivity extends AppCompatActivity {

    private final DatabaseHandler db = new DatabaseHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        EditText notificationTime = findViewById(R.id.notification_time_input);
        CheckBox hideCompleted = findViewById(R.id.hide_completed_checkbox);
        EditText filterCategory = findViewById(R.id.category_input);
        ImageButton clearCategory = findViewById(R.id.clear_category_button);
        Button deleteCategory = findViewById(R.id.delete_category_button);

        int notificationTimeHours = getSharedPreferences("prefs", MODE_PRIVATE).getInt("notification_time", 1);
        int filterCategoryId = getSharedPreferences("prefs", MODE_PRIVATE).getInt("filter_category", -1);
        if (filterCategoryId != -1) {
            filterCategory.setText(db.getCategoryById(filterCategoryId).getName());
        }

        notificationTime.setText(createTimeString(notificationTimeHours));

        notificationTime.setOnClickListener(v -> {
            final int[] time = {1, 2, 4, 8};
            final String[] options = createOptions(time);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose notification time");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    notificationTime.setText(String.valueOf(options[i]));
                    getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("notification_time", time[i]).apply();
                    if (notificationTimeHours != time[i]) {
                        rescheduleNotifications();
                    }
                }
            });

            builder.show();
        });

        hideCompleted.setChecked(getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("hide_completed", false));
        hideCompleted.setOnClickListener(v -> {
            getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("hide_completed", hideCompleted.isChecked())
                    .apply();
        });

        filterCategory.setOnClickListener(v -> {
            final List<Category> categories = db.getAllCategories();
            final String[] options = categories.stream()
                    .map(Category::getName)
                    .toArray(String[]::new);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose category");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    filterCategory.setText(String.valueOf(options[i]));
                    getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("filter_category", categories.get(i).getId())
                            .apply();
                }
            });

            builder.show();
        });

        clearCategory.setOnClickListener(v -> {
            filterCategory.setText("");
            getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit()
                    .putInt("filter_category", -1)
                    .apply();
        });

        deleteCategory.setOnClickListener(v -> {
            final List<Category> categories = db.getAllCategories();
            final String[] options = categories.stream()
                    .map(Category::getName)
                    .toArray(String[]::new);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose category to delete");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    db.deleteCategory(categories.get(i).getId());
                    filterCategory.setText("");
                    getSharedPreferences("prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("filter_category", -1)
                            .apply();
                }
            });

            builder.show();
        });
    }

    private void rescheduleNotifications() {
        List<Task> tasks = db.getAllTasks(true, -1);
        WorkManager.getInstance(this).cancelAllWork();

        for (Task task : tasks) {
            if (task.isNotificationScheduled() != null && task.isNotificationScheduled()) {

                int notificationTimeHours = getSharedPreferences("prefs", MODE_PRIVATE).getInt("notification_time", 1);
                if (task.getDueDate().minusHours(notificationTimeHours).isBefore(LocalDateTime.now())) {
                    continue;
                }
                Duration duration = Duration.between(LocalDateTime.now(), task.getDueDate().minusHours(notificationTimeHours));

                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(OneTimeScheduleWorker.class)
                        .setInitialDelay(duration.toMillis(), TimeUnit.MILLISECONDS)
                        .setInputData(new Data.Builder()
                                .putString("task_title", task.getTitle())
                                .putString("task_description", task.getDescription())
                                .build())
                        .addTag(String.valueOf(task.getId()))
                        .build();
                WorkManager.getInstance(this).enqueue(workRequest);
                task.setNotificationScheduled(true);
            }
        }
    }

    private String createTimeString(int hours) {
        return hours == 1 ? "1 hour before" : hours + " hours before";
    }

    private String[] createOptions(int[] time) {
        String[] options = new String[time.length];
        for (int i = 0; i < time.length; i++) {
            options[i] = createTimeString(time[i]);
        }
        return options;
    }
}