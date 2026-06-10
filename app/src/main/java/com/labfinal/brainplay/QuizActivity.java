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

        // LOGIKA REFRESH: Klik untuk restart kuis dari awal di kondisi apa pun!
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

        // Kembalikan warna tombol ke default bawaan tema saat kuis diulang
        int defaultColor = getResources().getColor(android.R.color.transparent);
        btnOptionA.setBackgroundColor(defaultColor);
        btnOptionB.setBackgroundColor(defaultColor);
        btnOptionC.setBackgroundColor(defaultColor);
        btnOptionD.setBackgroundColor(defaultColor);
        setOptionsClickable(true);

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
                // JIKA OFFLINE (TIDAK ADA JARINGAN): Langsung ambil data dari SQLite
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

            // Format teks tombol dengan prefiks huruf pilihan ganda A, B, C, D yang jelas
            btnOptionA.setText("A. " + Html.fromHtml(allOptions.get(0)).toString().trim());
            btnOptionB.setText("B. " + Html.fromHtml(allOptions.get(1)).toString().trim());
            btnOptionC.setText("C. " + Html.fromHtml(allOptions.get(2)).toString().trim());
            btnOptionD.setText("D. " + Html.fromHtml(allOptions.get(3)).toString().trim());

            // RESET WARNA TOMBOL KEMBALI KE DEFAULT (Setiap memuat soal baru)
            int defaultColor = getResources().getColor(android.R.color.transparent);
            btnOptionA.setBackgroundColor(defaultColor);
            btnOptionB.setBackgroundColor(defaultColor);
            btnOptionC.setBackgroundColor(defaultColor);
            btnOptionD.setBackgroundColor(defaultColor);

            // Aktifkan kembali fungsi klik tombol pilihan ganda
            setOptionsClickable(true);

            layoutLoading.setVisibility(View.GONE);
        } else {
            layoutLoading.setVisibility(View.GONE);

            // Perhitungan nilai skor akhir kuis
            int finalScore = (correctAnswerCount * 100) / questionList.size();

            // Menyimpan Skor ke SQLite secara Real-time
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
    private void checkAnswer(String selectedAnswerWithPrefix) {
        // Kunci semua tombol agar user tidak bisa memicu klik ganda saat warna efek tampil
        setOptionsClickable(false);

        QuestionModel currentQuestion = questionList.get(currentQuestionIndex);
        String correctAnswer = Html.fromHtml(currentQuestion.getCorrectAnswer()).toString().trim();

        // Potong prefiks "A. ", "B. ", dll untuk mendapatkan teks jawaban murni
        String selectedAnswer = selectedAnswerWithPrefix.substring(3).trim();

        // Mengambil warna indikasi dari framework android resources
        int warnaHijau = getResources().getColor(android.R.color.holo_green_light);
        int warnaMerah = getResources().getColor(android.R.color.holo_red_light);

        Button[] allButtons = {btnOptionA, btnOptionB, btnOptionC, btnOptionD};
        Button tombolYangDiklik = null;
        Button tombolYangBenar = null;

        // Cari tahu referensi objek tombol mana yang diklik dan mana yang benar
        for (Button btn : allButtons) {
            String btnTextClean = btn.getText().toString().substring(3).trim();
            if (btnTextClean.equalsIgnoreCase(selectedAnswer)) {
                tombolYangDiklik = btn;
            }
            if (btnTextClean.equalsIgnoreCase(correctAnswer)) {
                tombolYangBenar = btn;
            }
        }

        // Jalankan Logika Koreksi Interaktif Visual (Menggantikan Toast Lama)
        if (selectedAnswer.equalsIgnoreCase(correctAnswer)) {
            // JIKA JAWABAN BENAR: Warnai hijau dan sematkan emoji centang
            correctAnswerCount++;
            if (tombolYangDiklik != null) {
                tombolYangDiklik.setBackgroundColor(warnaHijau);
                tombolYangDiklik.setText(tombolYangDiklik.getText() + "  ✅");
            }
        } else {
            // JIKA JAWABAN SALAH: Warnai tombol salah jadi merah [X], lalu sorot tombol benar jadi hijau [Centang]
            if (tombolYangDiklik != null) {
                tombolYangDiklik.setBackgroundColor(warnaMerah);
                tombolYangDiklik.setText(tombolYangDiklik.getText() + "  ❌");
            }
            if (tombolYangBenar != null) {
                tombolYangBenar.setBackgroundColor(warnaHijau);
                tombolYangBenar.setText(tombolYangBenar.getText() + "  ✅");
            }
        }

        // MEMBERIKAN JEDA 1.5 DETIK UNTUK EVALUASI VISUAL USER, LALU AUTOMATIC NEXT SOAL
        new android.os.Handler().postDelayed(() -> {
            currentQuestionIndex++;
            displayQuestion();
        }, 1500);
    }

    // Fungsi utilitas pembantu untuk mengaktifkan/menonaktifkan interaksi tombol pilihan ganda
    private void setOptionsClickable(boolean isClickable) {
        btnOptionA.setClickable(isClickable);
        btnOptionB.setClickable(isClickable);
        btnOptionC.setClickable(isClickable);
        btnOptionD.setClickable(isClickable);
    }
}