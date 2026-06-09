package com.labfinal.brainplay;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private QuizDbHelper dbHelper;

    // PEMENUHAN SPESIFIKASI: Menggunakan Executor untuk Background Thread
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    // PEMENUHAN SPESIFIKASI: Menggunakan Handler untuk melempar data kembali ke UI Thread
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(getActivity()));

        dbHelper = new QuizDbHelper(getActivity());

        // Jalankan fungsi memuat data secara asinkronus
        muatRiwayatSkorDariDatabase();

        return view;
    }

    private void muatRiwayatSkorDariDatabase() {
        // 1. Mulai operasi di latar belakang (Background Thread)
        databaseExecutor.execute(() -> {
            // Mengambil data dari SQLite dijalankan di thread terpisah
            List<String> dataRiwayat = dbHelper.ambilRiwayatSkor();

            // 2. Kirim data hasil query ke UI Thread menggunakan Handler agar bisa ditampilkan
            mainThreadHandler.post(() -> {
                if (getActivity() != null) {
                    adapter = new HistoryAdapter(dataRiwayat);
                    rvHistory.setAdapter(adapter);
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Bersihkan executor saat fragment dihancurkan agar tidak terjadi memory leak
        databaseExecutor.shutdown();
    }
}