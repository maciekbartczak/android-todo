package com.bartczak.todo;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements TasksViewClickListener{

    private List<Task> tasks;
    private TasksAdapter adapter;
    private ActivityResultLauncher<Intent> addTaskLauncher;
    private ActivityResultLauncher<Intent> editTaskLauncher;
    private DatabaseHandler db = new DatabaseHandler(this);
    private boolean sortAscending = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},1);

        String channelName = getString(R.string.default_notification_channel_name);

        NotificationChannel channel = new NotificationChannel(channelName, channelName,
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        RecyclerView rv = findViewById(R.id.rv_todo);
        rv.setItemAnimator(new DefaultItemAnimator());

        LinearLayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);

        tasks = db.getAllTasks(sortAscending);

        adapter = new TasksAdapter(tasks, this);
        rv.setAdapter(adapter);

        FloatingActionButton addTask = findViewById(R.id.button_add);
        EditText searchInput = findViewById(R.id.search_input);
        Button searchButton = findViewById(R.id.button_search);
        ImageButton sortButton = findViewById(R.id.button_sort);

        sortButton.setOnClickListener(v -> {
            sortAscending = !sortAscending;
            sortButton.setImageResource(sortAscending ? android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float);
            if (searchInput.getText().toString().isEmpty()) {
                updateTasks(db.getAllTasks(sortAscending));
                adapter.notifyDataSetChanged();
            } else {
                searchTasks(searchInput);
            }
        });

        searchButton.setOnClickListener(v -> {
            searchTasks(searchInput);
        });

        addTaskLauncher = registerForActivityResult(new StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task newTask = (Task) result.getData().getSerializableExtra("task");
                        db.addTask(newTask);
                        if (newTask.getNotificationEnabled()) {
                            scheduleNotification(newTask);
                        }
                        updateTasks(db.getAllTasks(sortAscending));
                        adapter.notifyDataSetChanged();
                    }
                });
        addTask.setOnClickListener(view -> {
            Intent intent = new Intent(this, NewTaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        editTaskLauncher = registerForActivityResult(new StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task task = (Task) result.getData().getSerializableExtra("task");
                        if (task.getNotificationEnabled()) {
                            scheduleNotification(task);
                        } else {
                            cancelNotification(task);
                        }
                        db.updateTask(task);
                        updateTasks(db.getAllTasks(sortAscending));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void scheduleNotification(Task task) {
        Duration delay = Duration.between(LocalDateTime.now(), LocalDateTime.now().plusSeconds(10));

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(OneTimeScheduleWorker.class)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putString("task_title", task.getTitle())
                        .putString("task_description", task.getDescription())
                        .build())
                .addTag(String.valueOf(task.getId()))
                .build();
        WorkManager.getInstance(this).enqueue(workRequest);
    }

    private void cancelNotification(Task task) {
        WorkManager.getInstance(this).cancelAllWorkByTag(String.valueOf(task.getId()));
    }

    private void searchTasks(EditText searchInput) {
        String query = searchInput.getText().toString();
        if (query.equals("")) {
            updateTasks(db.getAllTasks(sortAscending));
            adapter.notifyDataSetChanged();
        } else {
            updateTasks(db.getTasksByTitle(query, sortAscending));
            adapter.notifyDataSetChanged();
        }
    }

    private void updateTasks(List<Task> tempTasks) {
        tasks.clear();
        tasks.addAll(tempTasks);
    }

    @Override
    public void onClick(View v, int position) {
        switch(v.getId()) {
            case R.id.done_checkbox:
                if (tasks.get(position).isDone()) {
                    tasks.get(position).setDone(false);
                    tasks.get(position).setDoneAt(null);
                } else {
                    tasks.get(position).setDone(true);
                    tasks.get(position).setDoneAt(LocalDateTime.now());
                }
                db.updateTask(tasks.get(position));
                break;
            case R.id.delete_button:
                if (tasks.get(position).getAttachmentPath() != null) {
                    File file = new File(tasks.get(position).getAttachmentPath());
                    file.delete();
                }
                db.deleteTask(tasks.get(position).getId());
                cancelNotification(tasks.get(position));
                tasks.remove(position);
                adapter.notifyItemRemoved(position);
                break;
            case R.id.edit_button:
                Intent intent = new Intent(this, NewTaskActivity.class);
                intent.putExtra("task", tasks.get(position));
                editTaskLauncher.launch(intent);
                break;
            case R.id.attachment_button:
                openAttachment(position);
                break;
        }
        adapter.notifyItemChanged(position);
    }

    private void openAttachment(int position) {
        File file = new File(tasks.get(position).getAttachmentPath());

        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        String mimeType = getContentResolver().getType(uri);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}