package com.example.ama;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Switch;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class Preferences extends AppCompatActivity {
    private Button nextButton, backButton, notificationIntervalButton;
    private Switch overlaySwitch;
    private TextView overlayStatus;
    private SharedPreferences securePrefs;
    private SharedPreferences prefs;
    private PackageManager packageManager;
    private boolean isUserInteracting = false;

    // ON CREATE METHOD - SIMULA NG ACTIVITY

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preferences);

        initSecurePrefs();
        initUI();
    }

    private void initSecurePrefs() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            securePrefs = EncryptedSharedPreferences.create(
                    "secure_prefs",       // FILE NAME NG ENCRYPTED PREFS
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "SECURE PREFS INIT FAILED", Toast.LENGTH_SHORT).show();
        }
    }

    private void initUI() {
        nextButton = findViewById(R.id.c2);
        backButton = findViewById(R.id.back);
        notificationIntervalButton = findViewById(R.id.c5);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        packageManager = getPackageManager();

        // NEXT BUTTON - PUNTA SA FINAL ACTIVITY
        nextButton.setOnClickListener(v -> startActivity(new Intent(this, FinalActivity.class)));

        // BACK BUTTON - BUMALIK SA NAUNA
        backButton.setOnClickListener(v -> onBackPressed());

        // NOTIFICATION INTERVAL BUTTON
        notificationIntervalButton.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setHint("Enter number of minutes (1-30)");
            input.setInputType(InputType.TYPE_CLASS_NUMBER);

            new AlertDialog.Builder(this)
                    .setTitle("Notification Interval")
                    .setMessage("Receive notifications every:")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String value = input.getText().toString().trim();

                        if (value.isEmpty()) {
                            Toast.makeText(this, "Input cannot be empty.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            int minutes = Integer.parseInt(value);
                            if (minutes < 1 || minutes > 30) {
                                Toast.makeText(this, "Please enter a number between 1-30", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            securePrefs.edit().putInt("notification_interval", minutes).apply();

                            // Use singular "minute" if 1, else "minutes"
                            String unit = (minutes == 1) ? "minute" : "minutes";

                            Toast.makeText(this, "Notification Interval Saved: " + minutes + " " + unit, Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid Number.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // TRACK KUNG NAGINTERACT NA SA UI
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        isUserInteracting = true;
    }
}
