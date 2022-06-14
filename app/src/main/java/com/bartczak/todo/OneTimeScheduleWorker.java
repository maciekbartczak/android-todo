package com.bartczak.todo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class OneTimeScheduleWorker extends Worker {

    private final Context context;
    private final WorkerParameters workerParameters;

    public OneTimeScheduleWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.workerParameters = workerParams;
    }

    @Override
    public Result doWork() {
        String taskTitle = workerParameters.getInputData().getString("task_title");
        String taskDescription = workerParameters.getInputData().getString("task_description");

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.putExtra("task_title", taskTitle);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "todoChannel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Your task " + taskTitle + " is due today!")
                .setContentText(taskDescription)
                .setContentIntent(resultPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, builder.build());

        return Result.success();
    }
}
