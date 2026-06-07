package com.labfinal.brainplay;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

    private TextView tvQuestionNumber, tvQuestionText;
    private Button btnOptionA, btnOptionB, btnOptionC, btnOptionD;

    // Variabel baru untuk mengontrol tirai loading screen di XML
    private LinearLayout layoutLoading;

    private List<QuestionModel> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswerCount = 0;
    private ApiService apiService;

    private Translator googleTranslator;
    private boolean isTranslatorReady = false;

    // Variabel penghitung: memastikan 1 soal + 4 tombol opsi selesai di-translate semua
    private int translationTaskCount = 0;

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

        // Hubungkan variabel dengan ID loading screen di XML
        layoutLoading = findViewById(R.id.layoutLoading);

        // LANGKAH AWAL: Langsung tampilkan tirai loading (menutupi kedipan teks bahasa Inggris)
        layoutLoading.setVisibility(View.VISIBLE);

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.INDONESIAN)
                .build();
        googleTranslator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder().build();
        googleTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    isTranslatorReady = true;
                    initRetrofitAndFetch(); // Ambil data dari server jika kamus siap
                })
                .addOnFailureListener(e -> {
                    layoutLoading.setVisibility(View.GONE); // Buka tirai jika error
                    Toast.makeText(this, "Gagal memuat penerjemah", Toast.LENGTH_SHORT).show();
                });

        btnOptionA.setOnClickListener(v -> checkAnswer(btnOptionA.getText().toString()));
        btnOptionB.setOnClickListener(v -> checkAnswer(btnOptionB.getText().toString()));
        btnOptionC.setOnClickListener(v -> checkAnswer(btnOptionC.getText().toString()));
        btnOptionD.setOnClickListener(v -> checkAnswer(btnOptionD.getText().toString()));
    }

    private void initRetrofitAndFetch() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opentdb.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
        fetchQuizData();
    }

    private void fetchQuizData() {
        int[] categories = {22, 27, 17};
        int randomCategory = categories[new Random().nextInt(categories.length)];

        apiService.getQuestions(5, randomCategory, "multiple").enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, Response<QuizResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    questionList = response.body().getResults();
                    currentQuestionIndex = 0;
                    correctAnswerCount = 0;
                    displayQuestion(); // Tampilkan soal
                } else {
                    layoutLoading.setVisibility(View.GONE);
                    tvQuestionText.setText("Gagal mengambil data kuis.");
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                layoutLoading.setVisibility(View.GONE);
                tvQuestionText.setText("Koneksi internet terputus.");
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void displayQuestion() {
        if (currentQuestionIndex < questionList.size()) {
            // Pasang tirai loading setiap kali sistem bersiap memuat nomor soal baru
            layoutLoading.setVisibility(View.VISIBLE);

            QuestionModel currentQuestion = questionList.get(currentQuestionIndex);

            tvQuestionNumber.setText("Pertanyaan: " + (currentQuestionIndex + 1) + " / " + questionList.size());

            List<String> allOptions = new ArrayList<>(currentQuestion.getIncorrectAnswers());
            allOptions.add(currentQuestion.getCorrectAnswer());
            Collections.shuffle(allOptions);

            String rawQuestion = Html.fromHtml(currentQuestion.getQuestion()).toString();
            String optA = Html.fromHtml(allOptions.get(0)).toString().trim();
            String optB = Html.fromHtml(allOptions.get(1)).toString().trim();
            String optC = Html.fromHtml(allOptions.get(2)).toString().trim();
            String optD = Html.fromHtml(allOptions.get(3)).toString().trim();

            // Reset hitungan tugas translasi (ada 5 komponen teks: 1 soal + 4 pilihan tombol)
            translationTaskCount = 0;

            // Jalankan proses translate di latar belakang secara senyap
            translateWithGoogle(rawQuestion, translated -> {
                tvQuestionText.setText(translated);
                checkAllTranslationsDone(); // Cek progress
            });
            translateWithGoogle(optA, translated -> {
                btnOptionA.setText(translated);
                checkAllTranslationsDone();
            });
            translateWithGoogle(optB, translated -> {
                btnOptionB.setText(translated);
                checkAllTranslationsDone();
            });
            translateWithGoogle(optC, translated -> {
                btnOptionC.setText(translated);
                checkAllTranslationsDone();
            });
            translateWithGoogle(optD, translated -> {
                btnOptionD.setText(translated);
                checkAllTranslationsDone();
            });

        } else {
            layoutLoading.setVisibility(View.GONE);
            int finalScore = (correctAnswerCount * 100) / questionList.size();
            Toast.makeText(this, "Kuis Selesai! Skor Kamu: " + finalScore, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // Fungsi internal untuk menghitung tugas translasi yang rampung
    private void checkAllTranslationsDone() {
        translationTaskCount++;
        // Jika ke-5 komponen sudah beres di-translate, buka tirai loading secara instan!
        if (translationTaskCount == 5) {
            layoutLoading.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("deprecation")
    private void checkAnswer(String selectedAnswer) {
        // Tutup tirai loading sekilas saat sistem mencocokkan jawaban
        layoutLoading.setVisibility(View.VISIBLE);

        QuestionModel currentQuestion = questionList.get(currentQuestionIndex);
        String rawCorrectAnswer = Html.fromHtml(currentQuestion.getCorrectAnswer()).toString().trim();

        translateWithGoogle(rawCorrectAnswer, translatedCorrectAnswer -> {
            if (selectedAnswer.trim().equalsIgnoreCase(translatedCorrectAnswer.trim())) {
                correctAnswerCount++;
                Toast.makeText(QuizActivity.this, "Benar! 🎉", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(QuizActivity.this, "Salah! ❌", Toast.LENGTH_SHORT).show();
            }
            currentQuestionIndex++;
            displayQuestion(); // Memanggil displayQuestion() yang otomatis mengelola tirai kembali
        });
    }

    private void translateWithGoogle(String textInput, final GoogleTranslateCallback callback) {
        if (isTranslatorReady && googleTranslator != null) {
            googleTranslator.translate(textInput)
                    .addOnSuccessListener(callback::onSuccess)
                    .addOnFailureListener(e -> callback.onSuccess(textInput));
        } else {
            callback.onSuccess(textInput);
        }
    }

    interface GoogleTranslateCallback {
        void onSuccess(String translatedText);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (googleTranslator != null) {
            googleTranslator.close();
        }
    }
}