package com.labfinal.brainplay;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Import pustaka Google ML Kit untuk translate offline
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

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

    // Komponen tampilan layar HP
    private TextView tvQuestionNumber, tvQuestionText;
    private Button btnOptionA, btnOptionB, btnOptionC, btnOptionD;

    // Variabel logika kuis
    private List<QuestionModel> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswerCount = 0;
    private ApiService apiService;

    // Variabel pengontrol Translator Google AI
    private Translator googleTranslator;
    private boolean isTranslatorReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Menghubungkan variabel kodingan dengan ID di file XML layout
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        btnOptionA = findViewById(R.id.btnOptionA);
        btnOptionB = findViewById(R.id.btnOptionB);
        btnOptionC = findViewById(R.id.btnOptionC);
        btnOptionD = findViewById(R.id.btnOptionD);

        // Status awal saat aplikasi dibuka
        tvQuestionText.setText("Menyiapkan Google AI Penerjemah (Hanya di awal)...");

        // Konfigurasi bahasa penerjemah: Dari INGGRIS ke INDONESIA
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.INDONESIAN)
                .build();
        googleTranslator = Translation.getClient(options);

        // Mengunduh paket bahasa Google AI ke lokal storage HP (Gratis & Mandiri)
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        googleTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    // Jika sukses download kamus, aktifkan translator dan ambil data kuis
                    isTranslatorReady = true;
                    tvQuestionText.setText("Mengunduh Paket Soal dari API Server...");
                    initRetrofitAndFetch();
                })
                .addOnFailureListener(e -> {
                    // Jika gagal download kamus AI
                    tvQuestionText.setText("Gagal menyiapkan AI Penerjemah: " + e.getMessage());
                });

        // Pengatur aksi ketika tombol pilihan jawaban diklik oleh user
        btnOptionA.setOnClickListener(v -> checkAnswer(btnOptionA.getText().toString()));
        btnOptionB.setOnClickListener(v -> checkAnswer(btnOptionB.getText().toString()));
        btnOptionC.setOnClickListener(v -> checkAnswer(btnOptionC.getText().toString()));
        btnOptionD.setOnClickListener(v -> checkAnswer(btnOptionD.getText().toString()));
    }

    // Fungsi inisialisasi jaringan Retrofit
    private void initRetrofitAndFetch() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opentdb.com/") // Alamat server kuis dunia
                .addConverterFactory(GsonConverterFactory.create()) // Pengubah JSON ke Objek Java
                .build();

        apiService = retrofit.create(ApiService.class);
        fetchQuizData(); // Lompat ke fungsi ambil kuis
    }

    // Fungsi mengambil soal secara live via internet
    private void fetchQuizData() {
        // Daftar kategori: 22 (Geografi), 27 (Hewan), 17 (Sains & Alam)
        int[] categories = {22, 27, 17};
        // Mengacak kategori agar kuis bervariasi setiap kali dimainkan
        int randomCategory = categories[new Random().nextInt(categories.length)];

        // Request 5 soal pilihan ganda sesuai kategori acak tadi
        apiService.getQuestions(5, randomCategory, "multiple").enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, Response<QuizResponse> response) {
                // Jika server merespons dengan sukses
                if (response.isSuccessful() && response.body() != null) {
                    questionList = response.body().getResults(); // Masukkan list soal ke variabel
                    currentQuestionIndex = 0;                   // Reset index ke soal nomor 1
                    correctAnswerCount = 0;                     // Reset skor benar
                    displayQuestion();                          // Tampilkan soal ke layar
                } else {
                    tvQuestionText.setText("Gagal mengambil data kuis dari server.");
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                // Jika koneksi internet terputus atau bermasalah
                tvQuestionText.setText("Koneksi internet terputus: " + t.getMessage());
            }
        });
    }

    // Fungsi memproses teks soal dan menampilkannya di layar HP
    @SuppressWarnings("deprecation")
    private void displayQuestion() {
        // Cek apakah masih ada soal yang tersisa
        if (currentQuestionIndex < questionList.size()) {
            QuestionModel currentQuestion = questionList.get(currentQuestionIndex);

            // Perbarui header teks nomor soal (Misal: Pertanyaan 1 / 5)
            tvQuestionNumber.setText("Pertanyaan: " + (currentQuestionIndex + 1) + " / " + questionList.size());
            tvQuestionText.setText("Menerjemahkan teks via Google AI...");

            // Satukan 1 jawaban benar dan 3 jawaban salah ke dalam satu List
            List<String> allOptions = new ArrayList<>(currentQuestion.getIncorrectAnswers());
            allOptions.add(currentQuestion.getCorrectAnswer());
            Collections.shuffle(allOptions); // Acak posisi tombol pilihan ganda (biar tidak di D terus)

            // Bersihkan simbol aneh HTML (seperti &quot; atau &#039;) dari teks API
            String rawQuestion = Html.fromHtml(currentQuestion.getQuestion()).toString();
            String optA = Html.fromHtml(allOptions.get(0)).toString().trim();
            String optB = Html.fromHtml(allOptions.get(1)).toString().trim();
            String optC = Html.fromHtml(allOptions.get(2)).toString().trim();
            String optD = Html.fromHtml(allOptions.get(3)).toString().trim();

            // AI Google menerjemahkan teks Inggris ke Indonesia langsung secara lokal/instan
            translateWithGoogle(rawQuestion, translated -> tvQuestionText.setText(translated));
            translateWithGoogle(optA, translated -> btnOptionA.setText(translated));
            translateWithGoogle(optB, translated -> btnOptionB.setText(translated));
            translateWithGoogle(optC, translated -> btnOptionC.setText(translated));
            translateWithGoogle(optD, translated -> btnOptionD.setText(translated));

        } else {
            // Jika semua soal (5 soal) sudah selesai dijawab, hitung skor akhir
            int finalScore = (correctAnswerCount * 100) / questionList.size();
            Toast.makeText(this, "Kuis Selesai! Skor Kamu: " + finalScore, Toast.LENGTH_LONG).show();
            finish(); // Tutup halaman kuis dan kembali ke menu utama
        }
    }

    // Fungsi mencocokkan jawaban pilihan user dengan kunci jawaban asli API
    @SuppressWarnings("deprecation")
    private void checkAnswer(String selectedAnswer) {
        QuestionModel currentQuestion = questionList.get(currentQuestionIndex);
        String rawCorrectAnswer = Html.fromHtml(currentQuestion.getCorrectAnswer()).toString().trim();

        tvQuestionText.setText("Memverifikasi jawaban...");

        // Terjemahkan kunci jawaban asli ke Indonesia dulu sebelum dibandingkan
        translateWithGoogle(rawCorrectAnswer, translatedCorrectAnswer -> {
            // Jika jawaban yang diklik user sama dengan kunci jawaban terjemahan
            if (selectedAnswer.trim().equalsIgnoreCase(translatedCorrectAnswer.trim())) {
                correctAnswerCount++; // Tambah poin benar
                Toast.makeText(QuizActivity.this, "Benar! 🎉", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(QuizActivity.this, "Salah! ❌", Toast.LENGTH_SHORT).show();
            }
            currentQuestionIndex++; // Naikkan ke soal berikutnya
            displayQuestion();       // Muat soal baru ke layar
        });
    }

    // Mesin Penerjemah Inti Google ML Kit Offline
    private void translateWithGoogle(String textInput, final GoogleTranslateCallback callback) {
        // Jika kamus siap dan objek translator tidak kosong
        if (isTranslatorReady && googleTranslator != null) {
            googleTranslator.translate(textInput)
                    .addOnSuccessListener(callback::onSuccess) // Jika sukses, kirim balik teks Indonesianya
                    .addOnFailureListener(e -> callback.onSuccess(textInput)); // Jika gagal, kirim teks asli Inggrisnya agar tidak crash
        } else {
            callback.onSuccess(textInput);
        }
    }

    // Interface jembatan penahan proses asynchronous
    interface GoogleTranslateCallback {
        void onSuccess(String translatedText);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Mematikan engine translator saat keluar kuis agar menghemat kapasitas memori RAM HP
        if (googleTranslator != null) {
            googleTranslator.close();
        }
    }
}