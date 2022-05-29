package com.bartczak.todo;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

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

public class MainActivity extends AppCompatActivity implements TasksViewClickListener{

    private ArrayList<Task> tasks;
    private RecyclerView.Adapter adapter;
    private ActivityResultLauncher<Intent> addTaskLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},1);

        Task task = new Task();
        task.setTitle("Zadanie 1");
        task.setDescription("Opis zadania 1, moze byc dosc dlugi, zobaczymy czy sie zmiesci. Otuz miesci sie no i gituwa");
        task.setCreatedAt(LocalDateTime.now());
        task.setDueDate(LocalDateTime.now().plusDays(1));
        task.setDone(false);

        tasks = new ArrayList<>();
        tasks.add(task);

        RecyclerView rv = findViewById(R.id.rv_todo);
        rv.setItemAnimator(new DefaultItemAnimator());

        LinearLayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);

        adapter = new TasksAdapter(tasks, this);
        rv.setAdapter(adapter);

        FloatingActionButton addTask = findViewById(R.id.button_add);

        addTaskLauncher = registerForActivityResult(new StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task task1 = (Task) result.getData().getSerializableExtra("task");
                        tasks.add(task1);
                        adapter.notifyDataSetChanged();
                    }
                });
        addTask.setOnClickListener(view -> {
            Intent intent = new Intent(this, NewTaskActivity.class);
            addTaskLauncher.launch(intent);
        });
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
                break;
            case R.id.delete_button:
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