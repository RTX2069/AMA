package com.example.ama;

import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import androidx.core.app.NotificationCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TrackingService extends Service {

    private final String CHANNEL_ID = "ama_channel_id";
    private final int NOTIFICATION_ID = 101;
    private Handler handler = new Handler();
    private Runnable trackingRunnable;
    private SharedPreferences securePrefs;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Tracking started..."));
        initSecurePrefs();
        startTracking();
        return START_STICKY;
    }

    private void initSecurePrefs() {
        try {
            String key = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            securePrefs = EncryptedSharedPreferences.create(
                    "secure_prefs",
                    key,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private void startTracking() {
        int intervalMinutes = securePrefs.getInt("notification_interval", 20);
        long intervalMillis = intervalMinutes * 60 * 1000L;

        trackingRunnable = new Runnable() {
            @Override
            public void run() {
                boolean parentalMode = securePrefs.getBoolean("parental_controls_enabled", false);
                sendIntervalNotification(intervalMinutes);
                if (parentalMode) goToHomeScreen();

                handler.postDelayed(this, intervalMillis);
            }
        };

        handler.postDelayed(trackingRunnable, intervalMillis);
    }

    private void sendIntervalNotification(int minutes) {
        NotificationManagerCompat.from(this).notify(
                NOTIFICATION_ID + 1,
                createNotification("You've been using your phone for " + minutes + " minute(s).")
        );
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, FinalActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AMA Screen Time Tracker")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void goToHomeScreen() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(trackingRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AMA Tracker",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
