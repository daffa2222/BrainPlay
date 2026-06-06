package com.labfinal.brainplay;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestionNumber, tvQuestionText;
    private Button btnOptionA, btnOptionB, btnOptionC, btnOptionD;

    // Ini data lokal sementara untuk TES MESIN KUIS saja
    private String[] questions = {
            "Apa nama planet terdekat dari Matahari?",
            "Siapakah penemu lampu pijar?",
            "Berapakah hasil dari 15 dikali 4?"
    };

    private String[][] options = {
            {"Venus", "Mars", "Merkurius", "Jupiter"},
            {"Albert Einstein", "Thomas Alva Edison", "Isaac Newton", "Nikola Tesla"},
            {"45", "60", "50", "75"}
    };

    private String[] correctAnswers = {
            "Merkurius",
            "Thomas Alva Edison",
            "60"
    };

    private int currentQuestionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        btnOptionA = findViewById(R.id.btnOptionA);
        btnOptionB = findViewById(R.id.btnOptionB);
        btnOptionC = findViewById(R.id.btnOptionC);
        btnOptionD = findViewById(R.id.btnOptionD);

        displayQuestion();

        btnOptionA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(btnOptionA.getText().toString());
            }
        });

        btnOptionB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(btnOptionB.getText().toString());
            }
        });

        btnOptionC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(btnOptionC.getText().toString());
            }
        });

        btnOptionD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(btnOptionD.getText().toString());
            }
        });
    }

    private void displayQuestion() {
        if (currentQuestionIndex < questions.length) {
            tvQuestionNumber.setText("Pertanyaan: " + (currentQuestionIndex + 1) + " / " + questions.length);
            tvQuestionText.setText(questions[currentQuestionIndex]);
            btnOptionA.setText(options[currentQuestionIndex][0]);
            btnOptionB.setText(options[currentQuestionIndex][1]);
            btnOptionC.setText(options[currentQuestionIndex][2]);
            btnOptionD.setText(options[currentQuestionIndex][3]);
        } else {
            Toast.makeText(this, "Kuis Selesai! Saatnya pasang API!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void checkAnswer(String selectedAnswer) {
        String correctAnswer = correctAnswers[currentQuestionIndex];
        if (selectedAnswer.equals(correctAnswer)) {
            Toast.makeText(this, "Jawaban Benar! 🎉", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Jawaban Salah! ❌", Toast.LENGTH_SHORT).show();
        }
        currentQuestionIndex++;
        displayQuestion();
    }
}