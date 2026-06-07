package com.labfinal.brainplay;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class QuestionModel {

    // Menangkap teks pertanyaan dari JSON kunci "question"
    @SerializedName("question")
    private String question;

    // Menangkap kunci jawaban yang benar dari JSON kunci "correct_answer"
    @SerializedName("correct_answer")
    private String correctAnswer;

    // Menangkap daftar pilihan jawaban yang salah dari JSON "incorrect_answers"
    @SerializedName("incorrect_answers")
    private List<String> incorrectAnswers;

    // Fungsi mengambil teks pertanyaan
    public String getQuestion() {
        return question;
    }

    // Fungsi mengambil kunci jawaban benar
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    // Fungsi mengambil daftar pilihan jawaban salah
    public List<String> getIncorrectAnswers() {
        return incorrectAnswers;
    }
}