package com.labfinal.brainplay;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    // Memanggil sisa link endpoint untuk mengambil soal pilihan ganda secara acak
    @GET("api.php")
    Call<QuizResponse> getQuestions(
            @Query("amount") int amount,
            @Query("type") String type
    );
}