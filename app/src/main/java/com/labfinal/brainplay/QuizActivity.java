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

    // Komponen visual teks untuk menampilkan nomor soal dan isi pertanyaan kuis
    private TextView tvQuestionNumber, tvQuestionText;

    // Empat komponen tombol untuk pilihan ganda (A, B, C, D)
    private Button btnOptionA, btnOptionB, btnOptionC, btnOptionD;

    // Komponen container untuk menampilkan tirai halaman loading screen
    private LinearLayout layoutLoading;

    // List objek untuk menampung seluruh daftar paket soal yang didapat dari API
    private List<QuestionModel> questionList = new ArrayList<>();

    // Indikator posisi soal yang sedang dikerjakan sekarang (dimulai dari indeks 0)
    private int currentQuestionIndex = 0;

    // Variabel penyimpan jumlah jawaban yang berhasil dijawab dengan benar oleh pengguna
    private int correctAnswerCount = 0;

    // Objek interface Retrofit untuk menghubungkan jalur request ke server
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Menghubungkan file Java ini dengan layout XML activity_quiz
        setContentView(R.layout.activity_quiz);

        // Menghubungkan variabel dengan ID komponen yang ada di dalam file XML
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        btnOptionA = findViewById(R.id.btnOptionA);
        btnOptionB = findViewById(R.id.btnOptionB);
        btnOptionC = findViewById(R.id.btnOptionC);
        btnOptionD = findViewById(R.id.btnOptionD);
        layoutLoading = findViewById(R.id.layoutLoading);

        // LANGKAH UTAMA: Langsung munculkan tirai loading di awal agar user tidak melihat kedipan teks kosong
        layoutLoading.setVisibility(View.VISIBLE);

        // Memanggil fungsi untuk menyiapkan konfigurasi Retrofit dan mengambil data internet
        initRetrofitAndFetch();

        // Mengatur aksi klik pada Tombol Opsi A untuk langsung memeriksa jawaban
        btnOptionA.setOnClickListener(v -> checkAnswer(btnOptionA.getText().toString()));

        // Mengatur aksi klik pada Tombol Opsi B untuk langsung memeriksa jawaban
        btnOptionB.setOnClickListener(v -> checkAnswer(btnOptionB.getText().toString()));

        // Mengatur aksi klik pada Tombol Opsi C untuk langsung memeriksa jawaban
        btnOptionC.setOnClickListener(v -> checkAnswer(btnOptionC.getText().toString()));

        // Mengatur aksi klik pada Tombol Opsi D untuk langsung memeriksa jawaban
        btnOptionD.setOnClickListener(v -> checkAnswer(btnOptionD.getText().toString()));
    }

    // Fungsi untuk mengonfigurasi library Retrofit dengan alamat server dasar (Base URL)
    private void initRetrofitAndFetch() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opentdb.com/") // Alamat server penyedia bank soal kuis global
                .addConverterFactory(GsonConverterFactory.create()) // Mengonversi data format JSON otomatis menjadi objek Java
                .build();

        // Membuat implementasi dari interface ApiService
        apiService = retrofit.create(ApiService.class);

        // Memanggil fungsi eksekusi penarikan data soal
        fetchQuizData();
    }

    // Fungsi untuk menarik data soal dari API secara asinkronus (di latar belakang)
    private void fetchQuizData() {
        // Array berisi ID kategori dari OpenTDB (22: Geografi, 27: Hewan, 17: Sains)
        int[] categories = {22, 27, 17};

        // Memilih salah satu ID kategori di atas secara acak agar kuis bervariasi setiap kali dibuka
        int randomCategory = categories[new Random().nextInt(categories.length)];

        // Melakukan request ambil 5 soal, tipe pilihan ganda (multiple choice), berdasarkan kategori acak
        apiService.getQuestions(5, randomCategory, "multiple").enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, Response<QuizResponse> response) {
                // Jika server berhasil merespon dengan baik dan datanya tidak kosong
                if (response.isSuccessful() && response.body() != null) {
                    // Memasukkan daftar soal dari server ke dalam variabel kodingan kita
                    questionList = response.body().getResults();
                    currentQuestionIndex = 0; // Reset ke nomor soal pertama
                    correctAnswerCount = 0;  // Reset hitungan skor awal

                    // Panggil fungsi untuk menampilkan soal pertama ke layar HP
                    displayQuestion();
                } else {
                    // Jika server gagal merespon, matikan loading screen dan ganti teks menjadi error
                    layoutLoading.setVisibility(View.GONE);
                    tvQuestionText.setText("Failed to load quiz data.");
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                // Jika koneksi internet mati atau request timeout, matikan loading screen dan beri info
                layoutLoading.setVisibility(View.GONE);
                tvQuestionText.setText("No internet connection.");
            }
        });
    }

    // Fungsi untuk menyusun teks soal dan membagikan jawaban ke 4 tombol secara acak
    @SuppressWarnings("deprecation")
    private void displayQuestion() {
        // Memeriksa apakah indeks soal saat ini masih di bawah jumlah total soal yang ada
        if (currentQuestionIndex < questionList.size()) {
            // Tampilkan kembali tirai loading saat sistem sedang sibuk menata teks soal baru
            layoutLoading.setVisibility(View.VISIBLE);

            // Ambil 1 objek soal berdasarkan nomor urut indeks saat ini
            QuestionModel currentQuestion = questionList.get(currentQuestionIndex);

            // Menampilkan info nomor soal yang sedang aktif ke pengguna (Contoh: Pertanyaan: 1 / 5)
            tvQuestionNumber.setText("Pertanyaan: " + (currentQuestionIndex + 1) + " / " + questionList.size());

            // Membuat list baru untuk menampung semua pilihan jawaban (pilihan salah + pilihan benar)
            List<String> allOptions = new ArrayList<>(currentQuestion.getIncorrectAnswers());
            allOptions.add(currentQuestion.getCorrectAnswer()); // Gabungkan jawaban benar ke dalam list

            // Mengacak urutan isi list agar posisi jawaban benar tidak selalu di tombol yang sama
            Collections.shuffle(allOptions);

            // Mengonversi kode simbol HTML murni dari API menjadi teks normal asli Bahasa Inggris (Contoh: &quot; menjadi ")
            tvQuestionText.setText(Html.fromHtml(currentQuestion.getQuestion()).toString());

            // Memasukkan masing-masing opsi jawaban yang sudah diacak ke dalam teks tombol A, B, C, dan D
            btnOptionA.setText(Html.fromHtml(allOptions.get(0)).toString().trim());
            btnOptionB.setText(Html.fromHtml(allOptions.get(1)).toString().trim());
            btnOptionC.setText(Html.fromHtml(allOptions.get(2)).toString().trim());
            btnOptionD.setText(Html.fromHtml(allOptions.get(3)).toString().trim());

            // PROSES SELESAI: Karena teks Inggris sudah siap dipajang, langsung matikan tirai loading screen secara instan
            layoutLoading.setVisibility(View.GONE);

        } else {
            // Jika semua soal sudah habis terjawab, matikan loading screen
            layoutLoading.setVisibility(View.GONE);

            // Menghitung persentase skor akhir kuis pengguna (Jumlah Benar x 100 dibagi Total Soal)
            int finalScore = (correctAnswerCount * 100) / questionList.size();

            // Menampilkan notifikasi popup hasil skor akhir di layar HP
            Toast.makeText(this, "Quiz Finished! Score: " + finalScore, Toast.LENGTH_LONG).show();

            // Menutup halaman kuis dan otomatis mengembalikan pengguna ke halaman utama (Main Menu)
            finish();
        }
    }

    // Fungsi untuk memverifikasi apakah jawaban yang diklik pengguna itu benar atau salah
    @SuppressWarnings("deprecation")
    private void checkAnswer(String selectedAnswer) {
        // Memunculkan tirai loading sekilas untuk memproses pencocokan data di latar belakang
        layoutLoading.setVisibility(View.VISIBLE);

        // Mengambil data soal yang sedang aktif saat ini
        QuestionModel currentQuestion = questionList.get(currentQuestionIndex);

        // Mengonversi kunci jawaban asli dari API ke teks normal tanpa spasi berlebih
        String correctAnswer = Html.fromHtml(currentQuestion.getCorrectAnswer()).toString().trim();

        // Membandingkan jawaban pilihan pengguna dengan kunci jawaban asli (mengabaikan huruf besar/kecil)
        if (selectedAnswer.trim().equalsIgnoreCase(correctAnswer)) {
            // Jika teksnya sama persis, tambahkan poin ke variabel jumlah jawaban benar
            correctAnswerCount++;
            // Tampilkan popup notifikasi sukses dalam bahasa Inggris agar selaras dengan isi kuis
            Toast.makeText(QuizActivity.this, "Correct! 🎉", Toast.LENGTH_SHORT).show();
        } else {
            // Jika teksnya tidak sama, tampilkan notifikasi salah
            Toast.makeText(QuizActivity.this, "Wrong! ❌", Toast.LENGTH_SHORT).show();
        }

        // Naikkan angka indeks untuk beralih ke nomor soal berikutnya
        currentQuestionIndex++;

        // Panggil kembali fungsi displayQuestion() untuk memuat visualisasi soal selanjutnya
        displayQuestion();
    }
}