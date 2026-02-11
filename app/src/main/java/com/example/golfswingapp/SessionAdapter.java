package com.example.golfswingapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {

    public interface OnClick {
        void onClick(SavedSessionsActivity.SessionItem item);
    }

    private final List<SavedSessionsActivity.SessionItem> items;
    private final OnClick onClick;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN);

    public SessionAdapter(List<SavedSessionsActivity.SessionItem> items, OnClick onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, sub;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txtTitle);
            sub = itemView.findViewById(R.id.txtSub);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        var item = items.get(position);
        holder.title.setText(item.name);

        // DATE_ADDED は秒なのでミリ秒へ
        holder.sub.setText(fmt.format(new Date(item.dateAddedSec * 1000L)));

        holder.itemView.setOnClickListener(v -> onClick.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
