package com.bartczak.todo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Task> tasks;
    private final TasksViewClickListener tasksViewClickListener;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public TasksAdapter(List<Task> tasks, TasksViewClickListener tasksViewClickListener) {
        this.tasks = tasks;
        this.tasksViewClickListener = tasksViewClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.todo_card, parent, false);
        return new TasksViewHolder(v, tasksViewClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TasksViewHolder viewHolder = (TasksViewHolder) holder;
        viewHolder.title.setText(tasks.get(position).getTitle());
        viewHolder.description.setText(tasks.get(position).getDescription());
        viewHolder.doneCheckBox.setChecked(tasks.get(position).isDone());
        viewHolder.dateCreated.setText(formatter.format(tasks.get(position).getCreatedAt()));
        viewHolder.dueDate.setText(formatter.format(tasks.get(position).getDueDate()));
        if (viewHolder.doneCheckBox.isChecked())
            viewHolder.dateDone.setText(formatter.format(tasks.get(position).getDoneAt()));
        else
            viewHolder.dateDone.setText("");
        if (tasks.get(position).getAttachmentPath() != null)
            viewHolder.attachmentButton.setVisibility(View.VISIBLE);
        else
            viewHolder.attachmentButton.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
