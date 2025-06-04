package com.example.ama;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.NumberPicker;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Calendar;

public class FinalActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "ama_channel_id";
    private static final int NOTIFICATION_ID = 101;
    private Handler handler = new Handler();
    private Runnable screenTimeRunnable;
    private SharedPreferences securePrefs;

    private NumberPicker startHourPicker, startAmPmPicker, stopHourPicker, stopAmPmPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_final);

        // NOTIFICATION CHANNEL PARA SA APP
        createNotificationChannel();

        initSecurePrefs();

        initNumberPickers();

        // PERMISSION PARA SA NOTIFICATION
        requestNotificationPermission();

        // BUTTON B3 PARA PUMUNTA SA PREFERENCES SCREEN
        findViewById(R.id.b3).setOnClickListener(v -> startActivity(new Intent(this, Preferences.class)));

        // BUTTON B1 PARA SIMULAN ANG SCREEN TIME TRACKING
        findViewById(R.id.b1).setOnClickListener(v -> {
            if (scheduleScreenTimeNotifications()) {
                Toast.makeText(this, "Screen time tracking started.", Toast.LENGTH_SHORT).show();
                sendStartTrackingNotification();
                goToHomeScreen();
            }
        });

        // BUTTON B2 PARA ITIGIL ANG TRACKING
        findViewById(R.id.b2).setOnClickListener(v -> stopTracking());
    }

    private void initNumberPickers() {
        startHourPicker = findViewById(R.id.startHourPicker);
        startAmPmPicker = findViewById(R.id.startAmPmPicker);
        stopHourPicker = findViewById(R.id.stopHourPicker);
        stopAmPmPicker = findViewById(R.id.stopAmPmPicker);

        startHourPicker.setMinValue(1);
        startHourPicker.setMaxValue(12);
        stopHourPicker.setMinValue(1);
        stopHourPicker.setMaxValue(12);

        String[] amPmValues = {"AM", "PM"};
        startAmPmPicker.setMinValue(0);
        startAmPmPicker.setMaxValue(1);
        startAmPmPicker.setDisplayedValues(amPmValues);

        stopAmPmPicker.setMinValue(0);
        stopAmPmPicker.setMaxValue(1);
        stopAmPmPicker.setDisplayedValues(amPmValues);
    }

    //NOTIFICATION PERMISSION
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    // ARA SA SCREEN TIME TRACKING
    private boolean scheduleScreenTimeNotifications() {
        int startHour = convertTo24Hour(startHourPicker.getValue(), startAmPmPicker.getValue());
        int stopHour = convertTo24Hour(stopHourPicker.getValue(), stopAmPmPicker.getValue());

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, startHour);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);

        Calendar stop = Calendar.getInstance();
        stop.set(Calendar.HOUR_OF_DAY, stopHour);
        stop.set(Calendar.MINUTE, 0);
        stop.set(Calendar.SECOND, 0);

        long currentTime = System.currentTimeMillis();
        long startTime = start.getTimeInMillis();
        long stopTime = stop.getTimeInMillis();

        if (stopTime <= startTime) {
            stop.add(Calendar.DAY_OF_YEAR, 1);
            stopTime = stop.getTimeInMillis();
        }

        // SINISAVE YUNG ORAS SA SHARED PREFERENCES
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("startHour", startHour)
                .putInt("startMinute", 0)
                .putInt("stopHour", stopHour)
                .putInt("stopMinute", 0)
                .apply();

        int intervalMinutes = securePrefs.getInt("notification_interval", 5);
        long intervalMillis = intervalMinutes * 60 * 1000L;

        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Math.max(currentTime, startTime), intervalMillis, pendingIntent);

        return true;
    }

    // NAGSESEND NG NOTIF
    private void sendStartTrackingNotification() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int startHour = prefs.getInt("startHour", 0);
        int stopHour = prefs.getInt("stopHour", 0);

        String[] amPmValues = {"AM", "PM"};
        int startAmPm = startHour >= 12 ? 1 : 0;
        int stopAmPm = stopHour >= 12 ? 1 : 0;

        int displayStartHour = (startHour == 0 || startHour == 12) ? 12 : startHour % 12;
        int displayStopHour = (stopHour == 0 || stopHour == 12) ? 12 : stopHour % 12;

        long durationMillis = calculateDurationMillis(startHour, stopHour);
        long durationMinutes = durationMillis / (60 * 1000);
        long durationHours = durationMinutes / 60;
        long remainingMinutes = durationMinutes % 60;

        // GINAGAWA ANG TEKSTO PARA SA TAGAL NG TRACKING (HOURS AT MINUTES)
        StringBuilder durationText = new StringBuilder("for ");
        if (durationHours > 0) {
            durationText.append(durationHours).append(durationHours == 1 ? " hour" : " hours");
            if (remainingMinutes > 0) {
                durationText.append(" and ").append(remainingMinutes).append(remainingMinutes == 1 ? " minute" : " minutes");
            }
        } else {
            durationText.append(remainingMinutes).append(remainingMinutes == 1 ? " minute" : " minutes");
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "start_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Start Tracking", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        String displayStart = displayStartHour + " " + amPmValues[startAmPm];
        String displayStop = displayStopHour + " " + amPmValues[stopAmPm];

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("AMA is now tracking your screentime.")
                .setContentText("Tracking from: " + displayStart + " to " + displayStop + " (" + durationText + ")")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(1001, builder.build());
    }

    // MILLISECONDS
    private long calculateDurationMillis(int startHour24, int stopHour24) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, startHour24);
        start.set(Calendar.MINUTE, 0);
        Calendar stop = Calendar.getInstance();
        stop.set(Calendar.HOUR_OF_DAY, stopHour24);
        stop.set(Calendar.MINUTE, 0);

        long startMillis = start.getTimeInMillis();
        long stopMillis = stop.getTimeInMillis();

        // KUNG PAREHO ANG START AT STOP TIME
        if (startMillis == stopMillis) {
            return 12 * 60 * 60 * 1000L; // 12 HOURS SA MILLISECONDS
        }

        if (stopMillis < startMillis) {
            stop.add(Calendar.DAY_OF_YEAR, 1);
            stopMillis = stop.getTimeInMillis();
        }

        return stopMillis - startMillis;
    }

    private int convertTo24Hour(int hour12, int amPm) {
        return (hour12 % 12) + (amPm == 1 ? 12 : 0);
    }

    // STOP ANG SCREEN TIME TRACKING AT ALARM
    private void stopTracking() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        Toast.makeText(this, "Tracking stopped.", Toast.LENGTH_SHORT).show();
    }

    private void initSecurePrefs() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            securePrefs = EncryptedSharedPreferences.create(
                    "secure_prefs",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Secure prefs init failed", Toast.LENGTH_SHORT).show();
        }
    }

    // PUPUNTA SA HOME SCREEN
    private void goToHomeScreen() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    //  NOTIFICATION CHANNEL
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "AMA Notification Channel";
            String description = "Channel for AMA screen time tracker";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (screenTimeRunnable != null) {
            handler.removeCallbacks(screenTimeRunnable);
        }
    }
}
