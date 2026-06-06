package com.labfinal.brainplay;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class QuizActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Menghubungkan file Java ini dengan layout activity_quiz.xml
        setContentView(R.layout.activity_quiz);
    }
}