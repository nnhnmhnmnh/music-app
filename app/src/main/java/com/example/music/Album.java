package com.example.music;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Album extends AppCompatActivity {
    private CollectionReference songsCollectionRef;
    private RecyclerView recyclerViewListSongAlbum;
    private TextView tvArtistAlbum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        songsCollectionRef = FirebaseFirestore.getInstance().collection("songs");
        recyclerViewListSongAlbum = findViewById(R.id.recyclerViewListSongAlbum);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewListSongAlbum.setLayoutManager(layoutManager);
        String artist = getIntent().getStringExtra("artist");
        getSongsByArtist(artist);
        tvArtistAlbum = findViewById(R.id.tvArtistAlbum);
        tvArtistAlbum.setText(artist);
    }

    public void getSongsByArtist(String artistName) {
        songsCollectionRef.whereEqualTo("artist", artistName)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<Song> songList = new ArrayList<>();

                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Song song = documentSnapshot.toObject(Song.class);
                            songList.add(song);
                        }

                        // Sử dụng danh sách bài hát songList
                        // Đưa danh sách bài hát vào adapter hoặc làm gì đó với nó
                        Intent intent = new Intent(Album.this, MusicService.class);
                        intent.setAction("setSongList");
                        intent.putExtra("songList", (Serializable) songList);
                        startService(intent);

                        SongAdapter adapter = new SongAdapter(songList, Album.this);
                        recyclerViewListSongAlbum.setAdapter(adapter);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Xử lý khi có lỗi xảy ra
                    }
                });
    }
}