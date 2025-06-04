
package com.example.ama;

import android.content.Context;

public abstract class TrackingNotificationHandler {

    protected Context context;

    public TrackingNotificationHandler(Context context) {
        this.context = context;
    }

    // ABSTRACT KAYA MERONG NOTIFICATION CHANNEL PARA SA SUBCLASSES
    public abstract void createNotificationChannel();

    // SEND NOTIF NA ABSTRACT
    public abstract void sendNotification();
}
