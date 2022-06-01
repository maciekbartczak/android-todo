package com.bartczak.todo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class NewTaskActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Path attachmentPath;
    private DatabaseHandler db = new DatabaseHandler(this);
    private int categoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        EditText title = findViewById(R.id.task_title_edit);
        EditText description = findViewById(R.id.task_description_edit);
        EditText dueDate = findViewById(R.id.due_date_edit);
        EditText dueTime = findViewById(R.id.due_time_edit);
        EditText attachment = findViewById(R.id.attachment_edit);
        CheckBox done = findViewById(R.id.new_task_done);
        CheckBox notify = findViewById(R.id.new_task_notify);
        Button save = findViewById(R.id.save_button);
        ImageButton deleteAttachment = findViewById(R.id.delete_attachment_button);
        EditText categoryName = findViewById(R.id.category_input);
        ImageButton newCategory = findViewById(R.id.clear_category_button);

        Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener date = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            dueDate.setText(sdf.format(calendar.getTime()));
        };

        TimePickerDialog.OnTimeSetListener time = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            dueTime.setText(sdf.format(calendar.getTime()));
        };

        dueDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, date,
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        dueTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, time,
                    calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });

        Intent incomingIntent = getIntent();
        Task savedTask = (Task) incomingIntent.getSerializableExtra("task");
        if (savedTask != null) {
            title.setText(savedTask.getTitle());
            description.setText(savedTask.getDescription());
            LocalDateTime localDueDate = savedTask.getDueDate();
            dueDate.setText(localDueDate.toString().substring(0, 10));
            dueTime.setText(localDueDate.toString().substring(11, 16));
            attachment.setText(savedTask.getAttachmentPath());
            done.setChecked(savedTask.isDone());
            notify.setChecked(savedTask.isNotificationEnabled());

            calendar.set(Calendar.YEAR, localDueDate.getYear());
            calendar.set(Calendar.MONTH, localDueDate.getMonthValue() - 1);
            calendar.set(Calendar.DAY_OF_MONTH, localDueDate.getDayOfMonth());
            calendar.set(Calendar.HOUR_OF_DAY, localDueDate.getHour());
            calendar.set(Calendar.MINUTE, localDueDate.getMinute());
        }

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        saveAttachment(result, attachment, savedTask);
                    }
                });

        attachment.setOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("*/*");
            chooseFile = Intent.createChooser(chooseFile, "Choose a file");
            filePickerLauncher.launch(chooseFile);
        });

        deleteAttachment.setOnClickListener(v -> {
            if (savedTask != null && savedTask.getAttachmentPath() != null) {
                File file = new File(savedTask.getAttachmentPath());
                file.delete();
                savedTask.setAttachmentPath(null);
            } else {
                File dir = new File(getExternalFilesDir("attachments").getAbsolutePath());
                File file = new File(dir, attachment.getText().toString());
                file.delete();
            }
            attachment.setText("");
            attachmentPath = null;
        });

        categoryName.setOnClickListener(v -> {
            final List<Category> categories = db.getAllCategories();
            final String[] options = categories.stream()
                    .map(Category::getName)
                    .toArray(String[]::new);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose category");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    categoryName.setText(String.valueOf(options[i]));
                    categoryId = categories.get(i).getId();
                }
            });

            builder.show();
        });

        newCategory.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setHint("Category name");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add category");
            builder.setView(input);
            builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String name = input.getText().toString();
                    if (name.isEmpty()) {
                        return;
                    }
                    Category category = new Category();
                    category.setName(name);

                    db.addCategory(category);

                    categoryName.setText(name);
                    categoryId = category.getId();
                }
            });

            builder.show();
        });

        save.setOnClickListener(view -> {
            if (!validateFields(title, description, dueDate, dueTime)) {
                return;
            }

            if (savedTask != null) {
                savedTask.setTitle(title.getText().toString());
                savedTask.setDescription(description.getText().toString());

                savedTask.setDueDate(getLocalDateTime(calendar));
                savedTask.setDone(done.isChecked());
                savedTask.setNotificationEnabled(notify.isChecked());
                if (attachmentPath != null) {
                    savedTask.setAttachmentPath(attachmentPath.toString());
                }
                if (done.isChecked()) {
                    savedTask.setDoneAt(LocalDateTime.now());
                }
                savedTask.setCategoryId(savedTask.getCategoryId());

                Intent intent = new Intent();
                intent.putExtra("task", savedTask);
                setResult(RESULT_OK, intent);
            } else {
                Task task = new Task();
                task.setTitle(title.getText().toString());
                task.setDescription(description.getText().toString());
                task.setCreatedAt(LocalDateTime.now());
                task.setDueDate(getLocalDateTime(calendar));
                task.setDone(done.isChecked());
                if (attachmentPath != null) {
                    task.setAttachmentPath(attachmentPath.toString());
                }
                if (done.isChecked()) {
                    task.setDoneAt(LocalDateTime.now());
                }
                task.setNotificationEnabled(notify.isChecked());
                task.setCategoryId(categoryId);

                Intent intent = new Intent();
                intent.putExtra("task", task);
                setResult(RESULT_OK, intent);
            }
            finish();
        });

    }

    private void saveAttachment(ActivityResult result, EditText attachmentText, Task savedTask) {
        if (savedTask != null && savedTask.getAttachmentPath() != null) {
            File dir = new File(getExternalFilesDir("attachments").getAbsolutePath());
            File file = new File(dir, savedTask.getAttachmentPath());
            if (file.exists()) {
                file.delete();
            }
        }

        Uri uri = Uri.parse(result.getData().getDataString());

        String filename = getFileName(uri);
        filename = generateUniqueFilename(filename);
        attachmentText.setText(filename);

        File dir = new File(getExternalFilesDir("attachments").getAbsolutePath());
        File destination = new File(dir, filename);

        try(InputStream in = getContentResolver().openInputStream(uri);) {
            destination.createNewFile();
            copy(in, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }

        attachmentPath = destination.toPath();
    }

    private String generateUniqueFilename(String filename) {
        String extension = filename.substring(filename.lastIndexOf("."));
        return UUID.randomUUID() + extension;
    }

    private String getFileName(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        ContentResolver cr = getContentResolver();
        Cursor metaCursor = cr.query(uri, projection, null, null, null);
        if (metaCursor != null) {
            try {
                if (metaCursor.moveToFirst()) {
                    return metaCursor.getString(0);
                }
            } finally {
                metaCursor.close();
            }
        }
        throw new IllegalStateException("Could not get file name");
    }

    private boolean validateFields(EditText title, EditText description, EditText dueDate, EditText dueTime) {
        boolean hasError = false;
        if (title.getText().toString().isEmpty()) {
            title.setError("Title is required");
            hasError = true;
        }
        if (description.getText().toString().isEmpty()) {
            description.setError("Description is required");
            hasError = true;
        }
        if (dueDate.getText().toString().isEmpty()) {
            dueDate.setError("Due date is required");
            hasError = true;
        }
        if (dueTime.getText().toString().isEmpty()) {
            dueTime.setError("Due time is required");
            hasError = true;
        }
        return !hasError;
    }

    private LocalDateTime getLocalDateTime(Calendar calendar) {
        TimeZone tz = calendar.getTimeZone();
        ZoneId zid = tz.toZoneId();
        return LocalDateTime.ofInstant(calendar.toInstant(), zid);
    }

    public static void copy(InputStream in, File dst) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }
}