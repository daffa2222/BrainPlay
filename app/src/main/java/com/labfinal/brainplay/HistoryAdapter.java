package com.labfinal.brainplay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<String> listHistory;

    public HistoryAdapter(List<String> listHistory) {
        this.listHistory = listHistory;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menggunakan layout bawaan android yang simpel untuk baris teks riwayat
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        String data = listHistory.get(position);
        holder.tvText.setText(data);
        // Mengatur warna teks agar kontras dan terbaca jelas
        holder.tvText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.brown_dark));
        holder.tvText.setTextSize(16);
    }

    @Override
    public int getItemCount() {
        return listHistory.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(android.R.id.text1);
        }
    }
}