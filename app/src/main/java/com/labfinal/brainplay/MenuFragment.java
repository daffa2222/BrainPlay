package com.labfinal.brainplay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class MenuFragment extends Fragment {

    public MenuFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        Button btnStartQuiz = view.findViewById(R.id.btnStartQuiz);
        Button btnViewHistory = view.findViewById(R.id.btnViewHistory);
        SwitchCompat switchTheme = view.findViewById(R.id.switchTheme);

        // Menggunakan SharedPreferences agar pilihan tema user tersimpan permanen meskipun aplikasi ditutup
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("IsDarkMode", false);

        // Atur posisi awal switch sesuai status tema saat ini
        switchTheme.setChecked(isDarkMode);

        // PEMENUHAN SPESIFIKASI: Logika pengubah tema global (Dark/Light)
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("IsDarkMode", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Aksi Tombol Mulai Kuis
        btnStartQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            startActivity(intent);
        });

        // Aksi Tombol Lihat Riwayat
        btnViewHistory.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.historyFragment);
        });

        return view;
    }
}