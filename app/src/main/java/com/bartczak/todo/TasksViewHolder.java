package com.bartczak.todo;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TasksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private final TasksViewClickListener tasksClickListener;

    TextView title;
    TextView description;
    TextView dateCreated;
    TextView dateDone;
    TextView dueDate;
    CheckBox doneCheckBox;
    Button deleteButton;
    Button editButton;
    Button attachmentButton;
    


    public TasksViewHolder(View v, TasksViewClickListener tasksClickListener) {
        super(v);
        this.tasksClickListener = tasksClickListener;
        title = v.findViewById(R.id.task_title);
        description = v.findViewById(R.id.task_desc);
        dateCreated = v.findViewById(R.id.created_date);
        dateDone = v.findViewById(R.id.done_date);
        doneCheckBox = v.findViewById(R.id.done_checkbox);
        deleteButton = v.findViewById(R.id.delete_button);
        editButton = v.findViewById(R.id.edit_button);
        dueDate = v.findViewById(R.id.due_date);
        attachmentButton = v.findViewById(R.id.attachment_button);

        doneCheckBox.setOnClickListener(this);
        deleteButton.setOnClickListener(this);
        editButton.setOnClickListener(this);
        attachmentButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        tasksClickListener.onClick(view, getAdapterPosition());
    }
}
