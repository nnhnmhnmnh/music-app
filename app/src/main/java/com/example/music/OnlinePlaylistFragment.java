package com.example.music;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OnlinePlaylistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OnlinePlaylistFragment extends Fragment {
    private RecyclerView recyclerViewPlaylist;
    private List<Song> songs;
    private FirebaseFirestore db;
    private SearchView searchPlaylist;
    private TextView textViewNoPlaylist;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public OnlinePlaylistFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OnlinePlaylistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OnlinePlaylistFragment newInstance(String param1, String param2) {
        OnlinePlaylistFragment fragment = new OnlinePlaylistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_online_playlist, container, false);

        recyclerViewPlaylist = view.findViewById(R.id.recyclerViewPlaylist);
        textViewNoPlaylist = view.findViewById(R.id.textViewNoPlaylist);
        searchPlaylist = view.findViewById(R.id.searchPlaylist);
        recyclerViewPlaylist.setLayoutManager(new LinearLayoutManager(getActivity()));
        db = FirebaseFirestore.getInstance();

        // Lấy danh sách bài hát từ playlist
        getPlaylistSongs();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchPlaylist.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                List<Song> songListQuery = searchSongs(songs, query);
                Intent intent = new Intent(getContext(), MusicService.class);
                intent.setAction("setSongList");
                intent.putExtra("songList", (Serializable) songListQuery);
                requireContext().startService(intent);
                SongAdapter adapter = new SongAdapter(songListQuery, requireContext());
                recyclerViewPlaylist.setAdapter(adapter);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) getPlaylistSongs();
                return true;
            }
        });
    }

    private List<Song> searchSongs(List<Song> songList, String query) {
        List<Song> matchingSongs = new ArrayList<>();

        // Tạo biểu thức chính quy từ truy vấn
        Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);

        for (Song song : songList) {
            // Kiểm tra trùng khớp theo displayName hoặc artist
            Matcher displayNameMatcher = pattern.matcher(song.getDisplayName());
            Matcher artistMatcher = pattern.matcher(song.getArtist());

            if (displayNameMatcher.find() || artistMatcher.find()) {
                matchingSongs.add(song);
            }
        }

        return matchingSongs;
    }

    private void getPlaylistSongs() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document != null && document.exists()) {
                                    if (document.contains("playlist")) {
                                        // Lấy danh sách các bài hát từ trường "playlist"
                                        List<String> playlistSongIds = new ArrayList<>();
                                        Object playlistObj = document.get("playlist");
                                        if (playlistObj instanceof Map) {
                                            Map<String, Boolean> playlistMap = (Map<String, Boolean>) playlistObj;
                                            for (Map.Entry<String, Boolean> entry : playlistMap.entrySet()) {
                                                if (entry.getValue()) {
                                                    playlistSongIds.add(entry.getKey());
                                                }
                                            }

                                            // Lấy danh sách bài hát từ collection "songs" dựa trên playlistSongIds
                                            getSongs(playlistSongIds);
                                        }
                                    }
                                }
                            } else {
                                // Xử lý lỗi
                            }
                        }
                    });
        }
    }

    private void getSongs(List<String> songIds) {
        if (songIds != null && !songIds.isEmpty()) {
            textViewNoPlaylist.setVisibility(View.GONE);
            db.collection("songs")
                    .whereIn(FieldPath.documentId(), songIds)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                songs = new ArrayList<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Song song = document.toObject(Song.class);
                                    songs.add(song);
                                }
//                                Intent intent = new Intent(getContext(), MusicService.class);
//                                intent.setAction("setSongList");
//                                intent.putExtra("songList", (Serializable) songs);
//                                requireContext().startService(intent);
                                // Ví dụ: Hiển thị danh sách bài hát trong ListView hoặc RecyclerView
                                SongAdapter adapter = new SongAdapter(songs, requireContext());
                                recyclerViewPlaylist.setAdapter(adapter);
                            } else {
                                // Xử lý lỗi
                            }
                        }
                    });
        }
    }
}