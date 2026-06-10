package com.labfinal.brainplay;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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
    private Button btnRefresh;

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

        // LOGIKA REFRESH: Klik untuk restart kuis dari awal
        btnRefresh.setOnClickListener(v -> {
            muatUlangKuis();
        });

        // Setup Listener Pilihan Jawaban
        btnOptionA.setOnClickListener(v -> checkAnswer(btnOptionA.getText().toString()));
        btnOptionB.setOnClickListener(v -> checkAnswer(btnOptionB.getText().toString()));
        btnOptionC.setOnClickListener(v -> checkAnswer(btnOptionC.getText().toString()));
        btnOptionD.setOnClickListener(v -> checkAnswer(btnOptionD.getText().toString()));
    }

    private void muatUlangKuis() {
        layoutLoading.setVisibility(View.VISIBLE);
        currentQuestionIndex = 0;
        correctAnswerCount = 0;

        int defaultColor = getResources().getColor(android.R.color.transparent);
        btnOptionA.setBackgroundColor(defaultColor);
        btnOptionB.setBackgroundColor(defaultColor);
        btnOptionC.setBackgroundColor(defaultColor);
        btnOptionD.setBackgroundColor(defaultColor);
        setOptionsClickable(true);

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
                    dbHelper.simpanSemuaSoalKeLokal(questionList);
                    displayQuestion();
                } else {
                    handleOfflineCondition();
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                handleOfflineCondition();
            }
        });
    }

    private void handleOfflineCondition() {
        List<QuestionModel> soalLokal = dbHelper.ambilSoalDariLokal();
        if (soalLokal != null && !soalLokal.isEmpty()) {
            questionList = soalLokal;
            layoutLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Offline Mode: Menggunakan database lokal!", Toast.LENGTH_SHORT).show();
            displayQuestion();
        } else {
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

            btnOptionA.setText("A. " + Html.fromHtml(allOptions.get(0)).toString().trim());
            btnOptionB.setText("B. " + Html.fromHtml(allOptions.get(1)).toString().trim());
            btnOptionC.setText("C. " + Html.fromHtml(allOptions.get(2)).toString().trim());
            btnOptionD.setText("D. " + Html.fromHtml(allOptions.get(3)).toString().trim());

            int defaultColor = getResources().getColor(android.R.color.transparent);
            btnOptionA.setBackgroundColor(defaultColor);
            btnOptionB.setBackgroundColor(defaultColor);
            btnOptionC.setBackgroundColor(defaultColor);
            btnOptionD.setBackgroundColor(defaultColor);

            setOptionsClickable(true);
            layoutLoading.setVisibility(View.GONE);
        } else {
            layoutLoading.setVisibility(View.GONE);

            int finalScore = (correctAnswerCount * 100) / questionList.size();

            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
                String tanggalSekarang = sdf.format(new java.util.Date());
                dbHelper.tambahSkorKeLokal(finalScore, tanggalSekarang);
            } catch (Exception e) {
                e.printStackTrace();
            }

            tampilkanDialogSkorKustom(finalScore);
        }
    }

    @SuppressWarnings("deprecation")
    private void checkAnswer(String selectedAnswerWithPrefix) {
        setOptionsClickable(false);

        QuestionModel currentQuestion = questionList.get(currentQuestionIndex);
        String correctAnswer = Html.fromHtml(currentQuestion.getCorrectAnswer()).toString().trim();
        String selectedAnswer = selectedAnswerWithPrefix.substring(3).trim();

        int warnaHijau = getResources().getColor(android.R.color.holo_green_light);
        int warnaMerah = getResources().getColor(android.R.color.holo_red_light);

        Button[] allButtons = {btnOptionA, btnOptionB, btnOptionC, btnOptionD};
        Button tombolYangDiklik = null;
        Button tombolYangBenar = null;

        for (Button btn : allButtons) {
            String btnTextClean = btn.getText().toString().substring(3).trim();
            if (btnTextClean.equalsIgnoreCase(selectedAnswer)) {
                tombolYangDiklik = btn;
            }
            if (btnTextClean.equalsIgnoreCase(correctAnswer)) {
                tombolYangBenar = btn;
            }
        }

        if (selectedAnswer.equalsIgnoreCase(correctAnswer)) {
            correctAnswerCount++;
            if (tombolYangDiklik != null) {
                tombolYangDiklik.setBackgroundColor(warnaHijau);
                tombolYangDiklik.setText(tombolYangDiklik.getText() + "  ✅");
            }
        } else {
            if (tombolYangDiklik != null) {
                tombolYangDiklik.setBackgroundColor(warnaMerah);
                tombolYangDiklik.setText(tombolYangDiklik.getText() + "  ❌");
            }
            if (tombolYangBenar != null) {
                tombolYangBenar.setBackgroundColor(warnaHijau);
                tombolYangBenar.setText(tombolYangBenar.getText() + "  ✅");
            }
        }

        new android.os.Handler().postDelayed(() -> {
            currentQuestionIndex++;
            displayQuestion();
        }, 1500);
    }

    // MODIFIKASI TOTAL: Desain Card Dialog Pop-up Rounded dan Adaptif Nuansa Krem/Gelap
    private void tampilkanDialogSkorKustom(int skorAkhir) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layoutCard = new LinearLayout(this);
        layoutCard.setOrientation(LinearLayout.VERTICAL);
        layoutCard.setPadding(60, 40, 60, 60);
        layoutCard.setGravity(android.view.Gravity.CENTER);

        // 1. Deteksi Otomatis Mode Gelap atau Terang Sistem
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        int warnaBackgroundCard;
        int warnaTeksKomponen;

        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // Pengaturan Visual untuk Mode Gelap
            warnaBackgroundCard = Color.parseColor("#333333"); // Abu-abu gelap material
            warnaTeksKomponen = Color.WHITE;
        } else {
            // Pengaturan Visual untuk Mode Terang (Nuansa Cokelat Krem sesuai request)
            warnaBackgroundCard = Color.parseColor("#EFE5DB"); // Warna Cokelat Krem Susu Lembut
            warnaTeksKomponen = Color.parseColor("#4A3B32");    // Warna Teks Cokelat Tua Gelap Berkelas
        }

        // 2. Membuat Efek Kelengkangan Sudut (Rounded Corner) dan Latar Belakang Card
        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setColor(warnaBackgroundCard);
        backgroundDrawable.setCornerRadius(40f); // Tingkat kelengkungan sudut (bikin bulat halus tidak kaku)
        layoutCard.setBackground(backgroundDrawable);

        // Layout Header untuk tombol silang (Close) di kanan atas
        LinearLayout layoutHeader = new LinearLayout(this);
        layoutHeader.setOrientation(LinearLayout.HORIZONTAL);
        layoutHeader.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams paramsHeader = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutHeader.setLayoutParams(paramsHeader);

        // Membuat Button Silang [✕]
        Button btnClose = new Button(this, null, android.R.attr.borderlessButtonStyle);
        btnClose.setText("✕");
        btnClose.setTextSize(22f);
        btnClose.setTypeface(null, android.graphics.Typeface.BOLD);
        btnClose.setMinWidth(0);
        btnClose.setMinHeight(0);
        btnClose.setPadding(10, 0, 10, 0);
        btnClose.setTextColor(warnaTeksKomponen);

        layoutHeader.addView(btnClose);
        layoutCard.addView(layoutHeader);

        // Komponen Judul "Hasil Kuis"
        TextView tvJudul = new TextView(this);
        tvJudul.setText("Hasil Kuis");
        tvJudul.setTextSize(24f);
        tvJudul.setTypeface(null, android.graphics.Typeface.BOLD);
        tvJudul.setTextColor(warnaTeksKomponen);
        tvJudul.setPadding(0, 0, 0, 25);
        layoutCard.addView(tvJudul);

        // Komponen Nilai Skor Besar di Tengah
        TextView tvSkorMurni = new TextView(this);
        tvSkorMurni.setText(String.valueOf(skorAkhir));
        tvSkorMurni.setTextSize(64f); // Diperbesar dikit biar makin tegas gokil
        tvSkorMurni.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSkorMurni.setGravity(android.view.Gravity.CENTER);

        // Tetap beri aksen warna hijau khas pencapaian skor agar kontrasnya hidup
        tvSkorMurni.setTextColor(Color.parseColor("#4CAF50"));
        layoutCard.addView(tvSkorMurni);

        // Detail keterangan jawaban kuis
        TextView tvDetail = new TextView(this);
        tvDetail.setText("Benar " + correctAnswerCount + " dari " + questionList.size() + " Pertanyaan");
        tvDetail.setTextSize(15f);
        tvDetail.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDetail.setTextColor(warnaTeksKomponen);
        tvDetail.setPadding(0, 20, 0, 10);
        layoutCard.addView(tvDetail);

        builder.setView(layoutCard);
        builder.setCancelable(false);

        AlertDialog dialogHasil = builder.create();

        // Hilangkan background bawaan AlertDialog bawaan android agar background rounded kita terlihat sempurna
        if (dialogHasil.getWindow() != null) {
            dialogHasil.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> {
            dialogHasil.dismiss();
            finish();
        });

        dialogHasil.show();
    }

    private void setOptionsClickable(boolean isClickable) {
        btnOptionA.setClickable(isClickable);
        btnOptionB.setClickable(isClickable);
        btnOptionC.setClickable(isClickable);
        btnOptionD.setClickable(isClickable);
    }
}