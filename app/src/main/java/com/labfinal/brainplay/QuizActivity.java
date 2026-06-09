package com.labfinal.brainplay;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    // Komponen visual teks untuk nomor soal dan isi pertanyaan kuis
    private TextView tvQuestionNumber, tvQuestionText;

    // Empat komponen tombol untuk pilihan ganda (A, B, C, D)
    private Button btnOptionA, btnOptionB, btnOptionC, btnOptionD;

    // Komponen container untuk layar loading dan layar error offline
    private LinearLayout layoutLoading, layoutError;

    // Tombol refresh untuk memuat ulang data saat jaringan gagal
    private Button btnRefresh;

    // List objek untuk menampung data paket soal kuis aktif
    private List<QuestionModel> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswerCount = 0;
    private ApiService apiService;

    // Objek database helper SQLite yang sudah kita buat di Step 2
    private QuizDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Inisialisasi komponen UI dari layout XML
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        btnOptionA = findViewById(R.id.btnOptionA);
        btnOptionB = findViewById(R.id.btnOptionB);
        btnOptionC = findViewById(R.id.btnOptionC);
        btnOptionD = findViewById(R.id.btnOptionD);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutError = findViewById(R.id.layoutError);
        btnRefresh = findViewById(R.id.btnRefresh);

        // Inisialisasi objek database helper lokal
        dbHelper = new QuizDbHelper(this);

        // Tampilkan loading screen di awal buka halaman
        layoutLoading.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);

        // Jalankan fungsi inisialisasi dan penarikan data kuis
        initRetrofitAndFetch();

        // Mengatur aksi klik pada Tombol Refresh (Coba Lagi) saat gagal jaringan
        btnRefresh.setOnClickListener(v -> {
            layoutError.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.VISIBLE);
            fetchQuizData(); // Lakukan request ulang ke API server
        });

        // Setup Listener Tombol Pilihan Jawaban
        btnOptionA.setOnClickListener(v -> checkAnswer(btnOptionA.getText().toString()));
        btnOptionB.setOnClickListener(v -> checkAnswer(btnOptionB.getText().toString()));
        btnOptionC.setOnClickListener(v -> checkAnswer(btnOptionC.getText().toString()));
        btnOptionD.setOnClickListener(v -> checkAnswer(btnOptionD.getText().toString()));
    }

    // Mengonfigurasi Retrofit untuk jalur koneksi internet API OpenTDB
    private void initRetrofitAndFetch() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opentdb.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
        fetchQuizData();
    }

    // Melakukan request data ke server API OpenTDB
    private void fetchQuizData() {
        int[] categories = {22, 27, 17};
        int randomCategory = categories[new Random().nextInt(categories.length)];

        apiService.getQuestions(5, randomCategory, "multiple").enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, Response<QuizResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // JIKA ONLINE BERHASIL: Ambil data soal dari server API
                    questionList = response.body().getResults();

                    // OTOMATIS PERSISTENT: Simpan/backup semua soal yang sukses didapat ini ke database SQLite HP
                    dbHelper.simpanSemuaSoalKeLokal(questionList);

                    currentQuestionIndex = 0;
                    correctAnswerCount = 0;
                    displayQuestion(); // Tampilkan soal pertama
                } else {
                    // Jika server merespon tidak valid, alihkan penanganan ke fungsi penyelemat offline
                    handleOfflineCondition();
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                // JIKA JARINGAN ERROR / OFFLINE MALAHAN: Alihkan langsung ke fungsi penyelamat offline
                handleOfflineCondition();
            }
        });
    }

    /**
     * FUNGSI PENYELAMAT OFFLINE (Sesuai Poin 6 dan 7 Spesifikasi Teknis Tugas Final):
     * Memeriksa ketersediaan data di database internal SQLite saat jaringan internet terputus.
     */
    private void handleOfflineCondition() {
        // Ambil data backup soal kuis dari database SQLite internal HP
        List<QuestionModel> soalLokal = dbHelper.ambilSoalDariLokal();

        if (soalLokal != null && !soalLokal.isEmpty()) {
            // Jika di SQLite ternyata ada simpanan data kuis lama, pakai data tersebut!
            questionList = soalLokal;
            currentQuestionIndex = 0;
            correctAnswerCount = 0;

            // Matikan tirai loading screen dan tampilkan popup notifikasi offline mode
            layoutLoading.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
            Toast.makeText(this, "Offline Mode: Memuat kuis dari database lokal SQLite!", Toast.LENGTH_LONG).show();

            displayQuestion(); // Tampilkan soal dari database internal HP
        } else {
            // JIKA SQLITE KOSONG DAN INTERNET MATI: Matikan loading dan munculkan layar penutup error + tombol refresh
            layoutLoading.setVisibility(View.GONE);
            layoutError.setVisibility(View.VISIBLE);
        }
    }

    // Mengatur visual teks dan pengacakan tombol opsi ganda
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
            int finalScore = (correctAnswerCount * 100) / questionList.size();
            Toast.makeText(this, "Quiz Finished! Score: " + finalScore, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // Menguji kebenaran jawaban klik pengguna
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