package com.bartczak.todo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;

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

        int notificationTimeHours = getSharedPreferences("prefs", MODE_PRIVATE).getInt("notification_time", 1);

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
    }

    private void rescheduleNotifications() {
        List<Task> tasks = db.getAllTasks(true);
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