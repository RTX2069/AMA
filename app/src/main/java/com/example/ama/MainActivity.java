package com.example.ama;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //BUTTON PARA PROCEED
        Button buttonProceed = this.<Button>findViewById(R.id.b1);
        buttonProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(MainActivity.this, Preferences.class);
                startActivity(intent1);

            }
        });

        //PAG SECOND TIME NA GINAMIT MAG SKIP

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean firstTime = prefs.getBoolean("firstTime", true);

        if (!firstTime) {
            startActivity(new Intent(this, FinalActivity.class));
            finish();
        }

        // BUTTON TO TERMS & CONDITIONS
        Button buttonTerms = this.<Button>findViewById(R.id.t5);
        buttonTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent3 = new Intent(MainActivity.this, Conditions.class);
                startActivity(intent3);
            }
        });

        //CHECK BOX TO OPEN TERMS
        CheckBox myCheckBox = findViewById(R.id.t4);
        Button myButton = findViewById(R.id.b1);

        // IF CHECKBOX IS CHECKED
        myCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // ENABLES THE BUTTON
                myButton.setEnabled(true);
                myButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#ABABAB")));
                myButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D2B498")));
                myButton.setTextColor(Color.parseColor("black"));

                Intent intent = new Intent(MainActivity.this, Conditions.class);
                startActivity(intent);
            } else {

                // DINIDISABLE ULIT
                myButton.setEnabled(false);
                myButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#ABABAB")));
                myButton.setTextColor(Color.parseColor("#7E7E7E"));
            }
        });


    }
}