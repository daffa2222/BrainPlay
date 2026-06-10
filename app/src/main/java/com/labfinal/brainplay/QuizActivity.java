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

        // LOGIKA ANTI-ACCIDENTAL EXIT: Mencegah user keluar kuis tanpa sengaja saat menekan tombol Back fisik
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                tampilkanDialogKeluarKuis();
            }
        });
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

    private void tampilkanDialogSkorKustom(int skorAkhir) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layoutCard = new LinearLayout(this);
        layoutCard.setOrientation(LinearLayout.VERTICAL);
        layoutCard.setPadding(60, 50, 60, 50);
        layoutCard.setGravity(android.view.Gravity.CENTER);

        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        int warnaBackgroundCard;
        int warnaTeksKomponen;

        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            warnaBackgroundCard = Color.parseColor("#333333");
            warnaTeksKomponen = Color.WHITE;
        } else {
            warnaBackgroundCard = Color.parseColor("#EFE5DB");
            warnaTeksKomponen = Color.parseColor("#4A3B32");
        }

        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setColor(warnaBackgroundCard);
        backgroundDrawable.setCornerRadius(40f);
        layoutCard.setBackground(backgroundDrawable);

        TextView tvJudul = new TextView(this);
        tvJudul.setText("Hasil Kuis");
        tvJudul.setTextSize(22f);
        tvJudul.setTypeface(null, android.graphics.Typeface.BOLD);
        tvJudul.setTextColor(warnaTeksKomponen);
        tvJudul.setGravity(android.view.Gravity.CENTER);
        tvJudul.setPadding(0, 10, 0, 10);
        layoutCard.addView(tvJudul);

        TextView tvSkorMurni = new TextView(this);
        tvSkorMurni.setText(String.valueOf(skorAkhir));
        tvSkorMurni.setTextSize(48f);
        tvSkorMurni.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSkorMurni.setGravity(android.view.Gravity.CENTER);
        tvSkorMurni.setTextColor(Color.parseColor("#4CAF50"));
        tvSkorMurni.setPadding(0, 10, 0, 10);
        layoutCard.addView(tvSkorMurni);

        TextView tvDetail = new TextView(this);
        tvDetail.setText("Benar " + correctAnswerCount + " dari " + questionList.size() + " Pertanyaan");
        tvDetail.setTextSize(15f);
        tvDetail.setTextColor(warnaTeksKomponen);
        tvDetail.setGravity(android.view.Gravity.CENTER);
        tvDetail.setPadding(0, 0, 0, 40);
        layoutCard.addView(tvDetail);

        LinearLayout layoutTombol = new LinearLayout(this);
        layoutTombol.setOrientation(LinearLayout.HORIZONTAL);
        layoutTombol.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams paramsLayout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutTombol.setLayoutParams(paramsLayout);

        Button btnSelesai = new Button(this, null, android.R.attr.borderlessButtonStyle);
        btnSelesai.setText("SELESAI");
        btnSelesai.setTextSize(14f);
        btnSelesai.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSelesai.setTextColor(currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES ?
                Color.GREEN : Color.parseColor("#2E7D32"));
        btnSelesai.setPadding(30, 20, 30, 20);
        layoutTombol.addView(btnSelesai);

        layoutCard.addView(layoutTombol);
        builder.setView(layoutCard);
        builder.setCancelable(false);

        AlertDialog dialogHasil = builder.create();

        if (dialogHasil.getWindow() != null) {
            dialogHasil.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSelesai.setOnClickListener(v -> {
            dialogHasil.dismiss();
            finish();
        });

        dialogHasil.show();
    }

    private void tampilkanDialogKeluarKuis() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layoutCard = new LinearLayout(this);
        layoutCard.setOrientation(LinearLayout.VERTICAL);
        layoutCard.setPadding(60, 50, 60, 50);
        layoutCard.setGravity(android.view.Gravity.CENTER);

        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        int warnaBackgroundCard;
        int warnaTeksUtama;

        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            warnaBackgroundCard = Color.parseColor("#333333");
            warnaTeksUtama = Color.WHITE;
        } else {
            warnaBackgroundCard = Color.parseColor("#EFE5DB");
            warnaTeksUtama = Color.parseColor("#4A3B32");
        }

        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setColor(warnaBackgroundCard);
        backgroundDrawable.setCornerRadius(40f);
        layoutCard.setBackground(backgroundDrawable);

        TextView tvJudul = new TextView(this);
        tvJudul.setText("Keluar Kuis?");
        tvJudul.setTextSize(22f);
        tvJudul.setTypeface(null, android.graphics.Typeface.BOLD);
        tvJudul.setTextColor(warnaTeksUtama);
        tvJudul.setGravity(android.view.Gravity.CENTER);
        tvJudul.setPadding(0, 10, 0, 20);
        layoutCard.addView(tvJudul);

        TextView tvPesan = new TextView(this);
        tvPesan.setText("Progress kuis sesi ini akan hilang dan tidak disimpan ke riwayat. Kamu yakin ingin menyerah?");
        tvPesan.setTextSize(15f);
        tvPesan.setTextColor(warnaTeksUtama);
        tvPesan.setGravity(android.view.Gravity.CENTER);
        tvPesan.setPadding(0, 0, 0, 40);
        layoutCard.addView(tvPesan);

        LinearLayout layoutTombol = new LinearLayout(this);
        layoutTombol.setOrientation(LinearLayout.HORIZONTAL);
        layoutTombol.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams paramsLayout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutTombol.setLayoutParams(paramsLayout);

        Button btnKeluar = new Button(this, null, android.R.attr.borderlessButtonStyle);
        btnKeluar.setText("YA, KELUAR");
        btnKeluar.setTextSize(14f);
        btnKeluar.setTypeface(null, android.graphics.Typeface.BOLD);
        btnKeluar.setTextColor(Color.parseColor("#EF5350"));
        btnKeluar.setPadding(30, 20, 30, 20);
        layoutTombol.addView(btnKeluar);

        View space = new View(this);
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(40, 1);
        space.setLayoutParams(spaceParams);
        layoutTombol.addView(space);

        Button btnLanjut = new Button(this, null, android.R.attr.borderlessButtonStyle);
        btnLanjut.setText("TIDAK, LANJUTKAN");
        btnLanjut.setTextSize(14f);
        btnLanjut.setTypeface(null, android.graphics.Typeface.BOLD);
        btnLanjut.setTextColor(currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES ?
                Color.GREEN : Color.parseColor("#2E7D32"));
        btnLanjut.setPadding(30, 20, 30, 20);
        layoutTombol.addView(btnLanjut);

        layoutCard.addView(layoutTombol);
        builder.setView(layoutCard);
        builder.setCancelable(true);

        AlertDialog dialogKeluar = builder.create();

        // FIX DI SINI: dialogVerify diganti menjadi dialogKeluar yang benar
        if (dialogKeluar.getWindow() != null) {
            dialogKeluar.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnKeluar.setOnClickListener(v -> {
            dialogKeluar.dismiss();
            finish();
        });

        btnLanjut.setOnClickListener(v -> {
            dialogKeluar.dismiss();
        });

        dialogKeluar.show();
    }

    private void setOptionsClickable(boolean isClickable) {
        btnOptionA.setClickable(isClickable);
        btnOptionB.setClickable(isClickable);
        btnOptionC.setClickable(isClickable);
        btnOptionD.setClickable(isClickable);
    }
}