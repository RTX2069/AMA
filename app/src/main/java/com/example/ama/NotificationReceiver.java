package com.example.ama;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "ama_channel_id";
    private static final int NOTIFICATION_ID = 101;

    // NOTIFICATION CHANNEL PARA SA  APP
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "AMA Channel";
            String description = "Notifications for AMA app";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        ScreenTimeNotificationHandler notificationHandler = new ScreenTimeNotificationHandler(context);

        notificationHandler.createNotificationChannel();

        // PAG ON YUNG SCREEN TAPOS PASOK SA TIME INTERVAL, SEND NOTIF NA
        notificationHandler.sendNotification();


        // PREFERENCES PARA SA SETTINGS
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        boolean overlayEnabled = prefs.getBoolean("overlay_enabled", false);


        // ORAS NG STARTTIME AND ENDTIME
        int startHour = prefs.getInt("startHour", 0);
        int stopHour = prefs.getInt("stopHour", 0);
        int startMinute = prefs.getInt("startMinute", 0);
        int stopMinute = prefs.getInt("stopMinute", 0);

        Calendar now = Calendar.getInstance();

        //TIME CALENDAR OBJECT
        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, startHour);
        startTime.set(Calendar.MINUTE, startMinute);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        Calendar stopTime = Calendar.getInstance();
        stopTime.set(Calendar.HOUR_OF_DAY, stopHour);
        stopTime.set(Calendar.MINUTE, stopMinute);
        stopTime.set(Calendar.SECOND, 0);
        stopTime.set(Calendar.MILLISECOND, 0);

        if (stopTime.before(startTime) || stopTime.equals(startTime)) {
            stopTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (now.before(startTime) || now.after(stopTime)) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent receiverIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, receiverIntent, PendingIntent.FLAG_IMMUTABLE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            return;
        }

        // PAG NAKA-ON LANG YUNG SCREEN BAGO MAGSEND NG NOTIFICATION
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null || !powerManager.isInteractive()) {
            return; // HINDI NAKA-ON YUNG SCREEN, WALANG NOTIFICATION
        }

        // GINAGAWA YUNG INTENT PARA PAG KINLICK NG NOTIFICATION, MAPUNTA SA FINAL ACTIVITY
        Intent activityIntent = new Intent(context, FinalActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // BUILD NG NOTIFICATION
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("AMA Reminder")
                .setContentText("You've been using your phone for a while, take a break.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // NOTIFICATION MANAGER
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
