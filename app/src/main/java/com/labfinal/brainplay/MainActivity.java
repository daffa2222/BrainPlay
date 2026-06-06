package com.labfinal.brainplay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Mengenalkan tombol "Mulai Kuis" dari layout XML ke Java
        Button btnStartQuiz = findViewById(R.id.btnStartQuiz);

        // 2. Membuat aksi ketika tombol "Mulai Kuis" diklik
        btnStartQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perintah untuk pindah dari MainActivity ke QuizActivity
                Intent intent = new Intent(MainActivity.this, QuizActivity.class);
                startActivity(intent);
            }
        });
    }
}