package com.labfinal.brainplay;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Memanggil layout activity_main.xml yang sudah kita desain cokelat cream
        setContentView(R.layout.activity_main);
    }
}