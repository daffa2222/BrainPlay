package com.labfinal.brainplay;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuizDbHelper extends SQLiteOpenHelper {

    // Nama file database yang akan disimpan secara persistent di dalam memori internal HP
    private static final String DATABASE_NAME = "BrainPlayQuiz.db";
    // Versi database (Kakak naikkan ke versi 2 agar tabel baru otomatis terbentuk di HP kamu)
    private static final int DATABASE_VERSION = 2;

    // Nama tabel untuk menyimpan data soal kuis
    private static final String TABLE_SOAL = "tabel_soal";

    // Daftar nama kolom (atribut) di dalam tabel_soal
    private static final String KEY_ID = "id";
    private static final String KEY_QUESTION = "pertanyaan";
    private static final String KEY_CORRECT_ANSWER = "jawaban_benar";
    private static final String KEY_INCORRECT_ANSWERS = "jawaban_salah_gabungan";

    // PEMENUHAN SPESIFIKASI: Definisi tabel baru untuk Riwayat Skor
    public static final String TABLE_SCORE = "tabel_skor";
    public static final String KEY_SCORE_ID = "id_skor";
    public static final String KEY_SCORE_VALUE = "nilai_skor";
    public static final String KEY_SCORE_DATE = "tanggal_main";

    // Constructor untuk menginisialisasi database saat aplikasi dijalankan
    public QuizDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Fungsi otomatis yang berjalan SEKALI untuk membuat tabel saat database pertama kali terbentuk
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Membuat tabel soal
        String CREATE_SOAL_TABLE = "CREATE TABLE " + TABLE_SOAL + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_QUESTION + " TEXT,"
                + KEY_CORRECT_ANSWER + " TEXT,"
                + KEY_INCORRECT_ANSWERS + " TEXT" + ")";
        db.execSQL(CREATE_SOAL_TABLE);

        // PEMENUHAN SPESIFIKASI: Membuat tabel skor untuk kebutuhan RecyclerView
        String CREATE_SCORE_TABLE = "CREATE TABLE " + TABLE_SCORE + "("
                + KEY_SCORE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_SCORE_VALUE + " INTEGER,"
                + KEY_SCORE_DATE + " TEXT" + ")";
        db.execSQL(CREATE_SCORE_TABLE);
    }

    // Fungsi untuk memperbarui struktur tabel jika ada kenaikan versi database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SOAL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCORE);
        onCreate(db);
    }

    /**
     * FUNGSI SIMPAN (ONLINE PROSES):
     * Memasukkan paket soal dari Retrofit API ke dalam database SQLite HP.
     */
    public void simpanSemuaSoalKeLokal(List<QuestionModel> daftarSoal) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Bersihkan data lama terlebih dahulu agar database tidak penuh
        db.execSQL("DELETE FROM " + TABLE_SOAL);

        for (QuestionModel soal : daftarSoal) {
            ContentValues values = new ContentValues();
            values.put(KEY_QUESTION, soal.getQuestion());
            values.put(KEY_CORRECT_ANSWER, soal.getCorrectAnswer());

            // Menggabungkan List<String> jawaban salah menjadi satu string panjang dengan pemisah "|||"
            StringBuilder salahGabung = new StringBuilder();
            for (int i = 0; i < soal.getIncorrectAnswers().size(); i++) {
                salahGabung.append(soal.getIncorrectAnswers().get(i));
                if (i < soal.getIncorrectAnswers().size() - 1) {
                    salahGabung.append("|||");
                }
            }
            values.put(KEY_INCORRECT_ANSWERS, salahGabung.toString());

            // Masukkan data baris soal ke dalam tabel_soal SQLite
            db.insert(TABLE_SOAL, null, values);
        }
        db.close();
    }

    /**
     * FUNGSI AMBIL (OFFLINE PROSES):
     * Mengambil data soal dari SQLite tanpa menggunakan method Setter eksternal.
     */
    public List<QuestionModel> ambilSoalDariLokal() {
        List<QuestionModel> daftarSoalLokal = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SOAL;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                QuestionModel soal = new QuestionModel();

                // Ambil data string dari kolom SQLite
                String qText = cursor.getString(cursor.getColumnIndexOrThrow(KEY_QUESTION));
                String cAnswer = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CORRECT_ANSWER));
                String salahGabung = cursor.getString(cursor.getColumnIndexOrThrow(KEY_INCORRECT_ANSWERS));

                // Pecah kembali string gabungan menjadi List<String> asli
                String[] arraySalah = salahGabung.split("\\|\\|\\|");
                List<String> listSalah = new ArrayList<>(Arrays.asList(arraySalah));

                // TRIK AMAN: Mengisi data langsung ke variabel private QuestionModel menggunakan teknik Reflection Java
                try {
                    Field fieldQuestion = QuestionModel.class.getDeclaredField("question");
                    fieldQuestion.setAccessible(true);
                    fieldQuestion.set(soal, qText);

                    Field fieldCorrect = QuestionModel.class.getDeclaredField("correctAnswer");
                    fieldCorrect.setAccessible(true);
                    fieldCorrect.set(soal, cAnswer);

                    Field fieldIncorrect = QuestionModel.class.getDeclaredField("incorrectAnswers");
                    fieldIncorrect.setAccessible(true);
                    fieldIncorrect.set(soal, listSalah);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                daftarSoalLokal.add(soal);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return daftarSoalLokal;
    }

    // ========================================================================
    // --- BACKEND LOGIC: FUNGSI MANAJEMEN RIWAYAT SKOR UNTUK RECYCLERVIEW ---
    // ========================================================================

    /**
     * Menyimpan hasil skor kuis setelah user menyelesaikan sesi kuis.
     */
    public void tambahSkorKeLokal(int skor, String tanggal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SCORE_VALUE, skor);
        values.put(KEY_SCORE_DATE, tanggal);

        db.insert(TABLE_SCORE, null, values);
        db.close();
    }

    /**
     * Mengambil daftar seluruh riwayat kuis untuk diumpankan ke HistoryFragment.
     */
    public List<String> ambilRiwayatSkor() {
        List<String> listSkor = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SCORE + " ORDER BY " + KEY_SCORE_ID + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int skor = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SCORE_VALUE));
                String tanggal = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SCORE_DATE));
                listSkor.add("Skor: " + skor + " | Dimainkan pada: " + tanggal);
            } while (cursor.moveToNext());
        } else {
            // Tampilan default jika user baru pertama kali masuk dan belum pernah main
            listSkor.add("Belum ada riwayat kuis tersimpan.");
        }

        cursor.close();
        db.close();
        return listSkor;
    }
}