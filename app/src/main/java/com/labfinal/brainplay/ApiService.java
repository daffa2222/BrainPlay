package com.labfinal.brainplay;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Interface untuk mengatur endpoint API target
public interface ApiService {

    // Menembak url 'api.php' untuk mengambil data kuis
    @GET("api.php")
    Call<QuizResponse> getQuestions(
            @Query("amount") int amount,       // Jumlah soal yang diminta (misal: 5)
            @Query("category") int category,   // Kode kategori (Geografi/Hewan/Sains)
            @Query("type") String type         // Tipe soal (multiple = pilihan ganda)
    );
}