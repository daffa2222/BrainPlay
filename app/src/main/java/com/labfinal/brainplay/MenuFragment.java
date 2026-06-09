package com.labfinal.brainplay;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class MenuFragment extends Fragment {

    public MenuFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Menghubungkan Fragment dengan layout fragment_menu.xml
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        Button btnStartQuiz = view.findViewById(R.id.btnStartQuiz);
        Button btnViewHistory = view.findViewById(R.id.btnViewHistory);

        // Aksi Tombol Mulai Kuis: Berpindah ke QuizActivity menggunakan Intent
        btnStartQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            startActivity(intent);
        });

        // Aksi Tombol Lihat Riwayat: Berpindah ke HistoryFragment menggunakan Navigation Component
        btnViewHistory.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.historyFragment);
        });

        return view;
    }
}