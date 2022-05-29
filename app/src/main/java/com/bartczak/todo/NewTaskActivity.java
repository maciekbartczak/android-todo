package com.bartczak.todo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.Locale;
import java.util.TimeZone;

public class NewTaskActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Path attachmentPath;

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


        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        saveAttachment(result, attachment);
                    }
                });

        attachment.setOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("*/*");
            chooseFile = Intent.createChooser(chooseFile, "Choose a file");
            filePickerLauncher.launch(chooseFile);
        });


        save.setOnClickListener(view -> {
            if (!validateFields(title, description, dueDate, dueTime)) {
                return;
            }

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

            Intent intent = new Intent();
            intent.putExtra("task", task);
            setResult(RESULT_OK, intent);
            finish();
        });



    }

    private void saveAttachment(ActivityResult result, EditText attachmentText) {
        Uri uri = Uri.parse(result.getData().getDataString());

        String filename = getFileName(uri);
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