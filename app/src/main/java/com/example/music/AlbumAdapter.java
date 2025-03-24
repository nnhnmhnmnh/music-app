package com.example.music;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private HashMap<String, Integer> albumCountMap;
    private Context context;

    public AlbumAdapter(HashMap<String, Integer> albumCountMap, Context context) {
        this.albumCountMap = albumCountMap;
        this.context = context;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        String album = (String) albumCountMap.keySet().toArray()[position];
        int count = albumCountMap.get(album);
        holder.bind(album, count);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Album.class);
                intent.putExtra("artist", album);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumCountMap.size();
    }

    public class AlbumViewHolder extends RecyclerView.ViewHolder {
        private TextView artistTextView;
        private TextView countTextView;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            artistTextView = itemView.findViewById(R.id.albumTextView);
            countTextView = itemView.findViewById(R.id.countTextView);
        }

        public void bind(String artist, int count) {
            artistTextView.setText(artist);
            countTextView.setText(String.valueOf(count));
        }
    }
}
