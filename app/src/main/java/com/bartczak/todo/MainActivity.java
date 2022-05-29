package com.bartczak.todo;

import android.Manifest;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements TasksViewClickListener{

    private List<Task> tasks;
    private TasksAdapter adapter;
    private ActivityResultLauncher<Intent> addTaskLauncher;
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
                        updateTasks(db.getAllTasks(sortAscending));
                        adapter.notifyDataSetChanged();
                    }
                });
        addTask.setOnClickListener(view -> {
            Intent intent = new Intent(this, NewTaskActivity.class);
            addTaskLauncher.launch(intent);
        });
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
                tasks.remove(position);
                adapter.notifyItemRemoved(position);
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