package com.labfinal.brainplay;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class QuizResponse {
    @SerializedName("response_code")
    private int responseCode;

    @SerializedName("results")
    private List<QuestionModel> results;

    public List<QuestionModel> getResults() {
        return results;
    }
}