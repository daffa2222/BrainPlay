package com.labfinal.brainplay;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestionNumber, tvQuestionText;
    private Button btnOptionA, btnOptionB, btnOptionC, btnOptionD;
    private LinearLayout layoutLoading;
    private Button btnRefresh; // Tombol refresh permanen

    private List<QuestionModel> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswerCount = 0;
    private ApiService apiService;
    private QuizDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Inisialisasi komponen UI
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        btnOptionA = findViewById(R.id.btnOptionA);
        btnOptionB = findViewById(R.id.btnOptionB);
        btnOptionC = findViewById(R.id.btnOptionC);
        btnOptionD = findViewById(R.id.btnOptionD);
        layoutLoading = findViewById(R.id.layoutLoading);
        btnRefresh = findViewById(R.id.btnRefresh);

        dbHelper = new QuizDbHelper(this);

        // Menyiapkan konfigurasi jaringan Retrofit API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opentdb.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Muat data kuis pertama kali saat halaman dibuka
        muatUlangKuis();

        // LOGIKA REFRESH YANG BENAR: Klik untuk restart kuis dari awal di kondisi apa pun!
        btnRefresh.setOnClickListener(v -> {
            muatUlangKuis();
        });

        // Setup Listener Pilihan Jawaban
        btnOptionA.setOnClickListener(v -> checkAnswer(btnOptionA.getText().toString()));
        btnOptionB.setOnClickListener(v -> checkAnswer(btnOptionB.getText().toString()));
        btnOptionC.setOnClickListener(v -> checkAnswer(btnOptionC.getText().toString()));
        btnOptionD.setOnClickListener(v -> checkAnswer(btnOptionD.getText().toString()));
    }

    // Fungsi pusat untuk mereset indeks kuis dan mencoba mengambil data fresh
    private void muatUlangKuis() {
        layoutLoading.setVisibility(View.VISIBLE);
        currentQuestionIndex = 0;
        correctAnswerCount = 0;
        fetchQuizData(); // Mencoba hubungi internet lewat Retrofit
    }

    private void fetchQuizData() {
        int[] categories = {22, 27, 17};
        int randomCategory = categories[new Random().nextInt(categories.length)];

        apiService.getQuestions(5, randomCategory, "multiple").enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, Response<QuizResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // JIKA ONLINE: Ambil data dari server API lalu update backup SQLite
                    questionList = response.body().getResults();
                    dbHelper.simpanSemuaSoalKeLokal(questionList);

                    displayQuestion();
                } else {
                    // Jika server API bermasalah, langsung ambil data offline
                    handleOfflineCondition();
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                // JIKA OFFLINE (TIDAK ADA JARINGAN): Langsung ambil data dari SQLite tanpa banyak tanya!
                handleOfflineCondition();
            }
        });
    }

    private void handleOfflineCondition() {
        // Ambil data soal yang sudah pernah disimpan di SQLite HP kamu
        List<QuestionModel> soalLokal = dbHelper.ambilSoalDariLokal();

        if (soalLokal != null && !soalLokal.isEmpty()) {
            // SOLUSI UTAMA: Soal dan jawaban langsung muncul normal secara offline!
            questionList = soalLokal;
            layoutLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Offline Mode: Menggunakan database lokal!", Toast.LENGTH_SHORT).show();
            displayQuestion();
        } else {
            // Kondisi darurat jika aplikasi baru diinstal, belum pernah online, dan langsung dimainkan offline
            layoutLoading.setVisibility(View.GONE);
            tvQuestionText.setText("Gagal menampilkan data dari API.\n(Tidak ada jaringan & Cache kosong).\n\nSilakan hubungkan internet sekali saja untuk mengambil kuis awal.");
            btnOptionA.setText("-");
            btnOptionB.setText("-");
            btnOptionC.setText("-");
            btnOptionD.setText("-");
        }
    }

    @SuppressWarnings("deprecation")
    private void displayQuestion() {
        if (currentQuestionIndex < questionList.size()) {
            layoutLoading.setVisibility(View.VISIBLE);
            QuestionModel currentQuestion = questionList.get(currentQuestionIndex);

            tvQuestionNumber.setText("Pertanyaan: " + (currentQuestionIndex + 1) + " / " + questionList.size());

            List<String> allOptions = new ArrayList<>(currentQuestion.getIncorrectAnswers());
            allOptions.add(currentQuestion.getCorrectAnswer());
            Collections.shuffle(allOptions);

            tvQuestionText.setText(Html.fromHtml(currentQuestion.getQuestion()).toString());
            btnOptionA.setText(Html.fromHtml(allOptions.get(0)).toString().trim());
            btnOptionB.setText(Html.fromHtml(allOptions.get(1)).toString().trim());
            btnOptionC.setText(Html.fromHtml(allOptions.get(2)).toString().trim());
            btnOptionD.setText(Html.fromHtml(allOptions.get(3)).toString().trim());

            layoutLoading.setVisibility(View.GONE);
        } else {
            layoutLoading.setVisibility(View.GONE);

            // Perhitungan nilai skor akhir kuis
            int finalScore = (correctAnswerCount * 100) / questionList.size();

            // ====================================================================
            // BACKEND INTERCEPT: Proses Menyimpan Skor ke SQLite secara Real-time
            // ====================================================================
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
                String tanggalSekarang = sdf.format(new java.util.Date());
                dbHelper.tambahSkorKeLokal(finalScore, tanggalSekarang);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Toast.makeText(this, "Quiz Finished! Score: " + finalScore, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressWarnings("deprecation")
    private void checkAnswer(String selectedAnswer) {
        layoutLoading.setVisibility(View.VISIBLE);
        QuestionModel currentQuestion = questionList.get(currentQuestionIndex);
        String correctAnswer = Html.fromHtml(currentQuestion.getCorrectAnswer()).toString().trim();

        if (selectedAnswer.trim().equalsIgnoreCase(correctAnswer)) {
            correctAnswerCount++;
            Toast.makeText(QuizActivity.this, "Correct! 🎉", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(QuizActivity.this, "Wrong! ❌", Toast.LENGTH_SHORT).show();
        }

        currentQuestionIndex++;
        displayQuestion();
    }
}