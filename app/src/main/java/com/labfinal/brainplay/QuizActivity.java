package com.labfinal.brainplay;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestionNumber, tvQuestionText;
    private Button btnOptionA, btnOptionB, btnOptionC, btnOptionD;

    // Menampung daftar soal yang diambil dari API internet
    private List<QuestionModel> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswerCount = 0; // Untuk menghitung skor akhir

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Inisialisasi komponen tampilan UI
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        btnOptionA = findViewById(R.id.btnOptionA);
        btnOptionB = findViewById(R.id.btnOptionB);
        btnOptionC = findViewById(R.id.btnOptionC);
        btnOptionD = findViewById(R.id.btnOptionD);

        // Set teks pemberitahuan awal
        tvQuestionText.setText("Sedang mengambil soal dari internet...");

        // Ambil data kuis dari server internet
        fetchQuizData();

        // Mengatur aksi klik pada tombol pilihan jawaban kuis
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

        // TUGAS ASLI DAFFA: Memastikan semua tombol merujuk ke teks tombolnya masing-masing
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

    private void fetchQuizData() {
        // Membangun Retrofit penembak URL API bank soal sesuai arahan tugas
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opentdb.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        // Meminta kuis sebanyak 5 pertanyaan bertipe pilihan ganda dari API
        apiService.getQuestions(5, "multiple").enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, Response<QuizResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    questionList = response.body().getResults();
                    currentQuestionIndex = 0;
                    correctAnswerCount = 0;
                    displayQuestion();
                } else {
                    tvQuestionText.setText("Gagal memuat soal dari server.");
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                tvQuestionText.setText("Koneksi internet bermasalah: " + t.getMessage());
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void displayQuestion() {
        if (currentQuestionIndex < questionList.size()) {
            QuestionModel currentQuestion = questionList.get(currentQuestionIndex);

            // Tampilkan info nomor urutan kuis
            tvQuestionNumber.setText("Pertanyaan: " + (currentQuestionIndex + 1) + " / " + questionList.size());

            // Format teks HTML universal agar simbol-simbol aneh terbaca normal
            tvQuestionText.setText(Html.fromHtml(currentQuestion.getQuestion()));

            // Satukan jawaban benar dan salah, lalu acak posisinya
            List<String> allOptions = new ArrayList<>(currentQuestion.getIncorrectAnswers());
            allOptions.add(currentQuestion.getCorrectAnswer());
            Collections.shuffle(allOptions);

            // Masukkan teks ke dalam tombol pilihan kuis dan bersihkan dari format HTML
            btnOptionA.setText(Html.fromHtml(allOptions.get(0)).toString().trim());
            btnOptionB.setText(Html.fromHtml(allOptions.get(1)).toString().trim());
            btnOptionC.setText(Html.fromHtml(allOptions.get(2)).toString().trim());
            btnOptionD.setText(Html.fromHtml(allOptions.get(3)).toString().trim());
        } else {
            // RUMUS AUTOMATIC SCORE: (Total Benar / Total Soal dari API) * 100
            int finalScore = (correctAnswerCount * 100) / questionList.size();

            // Tampilkan pop-up skor akhir kuis
            Toast.makeText(this, "Kuis Selesai! Skor Kamu: " + finalScore, Toast.LENGTH_LONG).show();
            finish(); // Selesai dan kembali ke menu utama
        }
    }

    @SuppressWarnings("deprecation")
    private void checkAnswer(String selectedAnswer) {
        QuestionModel currentQuestion = questionList.get(currentQuestionIndex);

        // PERBAIKAN UTAMA: Bersihkan teks jawaban dari API agar spasi/karakter HTML tidak merusak perbandingan skor
        String correctAnswerText = Html.fromHtml(currentQuestion.getCorrectAnswer()).toString().trim();
        String userSelection = selectedAnswer.trim();

        // Periksa kebenaran jawaban user secara presisi
        if (userSelection.equals(correctAnswerText)) {
            correctAnswerCount++;
            Toast.makeText(this, "Benar! 🎉", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Salah! ❌", Toast.LENGTH_SHORT).show();
        }

        // Lanjut ke soal berikutnya
        currentQuestionIndex++;
        displayQuestion();
    }
}