package com.example.music;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment2#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment2 extends Fragment {
    private ViewPager2 viewPagerFragment2;
    private TabLayout tabLayoutFragment2;
    private FirebaseFirestore db;
    private int currentFragment = 0;



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Fragment2() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Fragment2.
     */
    // TODO: Rename and change types and number of parameters
    public static Fragment2 newInstance(String param1, String param2) {
        Fragment2 fragment = new Fragment2();
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
        View view = inflater.inflate(R.layout.fragment_2, container, false);

        viewPagerFragment2 = view.findViewById(R.id.viewPagerFragment2);
        tabLayoutFragment2 = view.findViewById(R.id.tabLayoutFragment2);
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new OnlineDefaultFragment());
        fragmentList.add(new OnlineFavoriteFragment());
        fragmentList.add(new OnlineArtistFragment());
        fragmentList.add(new OnlinePlaylistFragment());

        ViewPagerAdapter adapter = new ViewPagerAdapter(requireActivity(), fragmentList);
        viewPagerFragment2.setAdapter(adapter);

        new TabLayoutMediator(tabLayoutFragment2, viewPagerFragment2, (tab, position) -> {
            // Đặt tiêu đề cho từng tab tại đây
            if (position == 0) {
                tab.setText("Trang chủ");
            } else if (position == 1) {
                tab.setText("Yêu thích");
            } else if (position == 2) {
                tab.setText("Nghệ sĩ");
            } else if (position == 3) {
                tab.setText("D.sách phát");
            }
        }).attach();

        db = FirebaseFirestore.getInstance();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayoutFragment2.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int selectedTabPosition = tab.getPosition();
                switch (selectedTabPosition) {
                    case 0:
                        setDefaultSongs();
                        currentFragment = 0;
                        break;
                    case 1:
                        setFavoriteSongs();
                        currentFragment = 1;
                        break;
                    case 3:
                        setPlaylistSongs();
                        currentFragment = 3;
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    public void setDefaultSongs() {
        CollectionReference songsCollection = db.collection("songs"); // Tạo một bộ sưu tập "songs"
        songsCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Song> songList = new ArrayList<>();

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String displayName = documentSnapshot.getString("displayName");
                        String artist = documentSnapshot.getString("artist");
                        String lrc = documentSnapshot.getString("lrc");
                        String data = documentSnapshot.getString("data");
                        String album = documentSnapshot.getString("album");
                        String avt = documentSnapshot.getString("avt");

                        Song song = new Song(data, artist, album, displayName, lrc, avt);
                        songList.add(song);
                    }

                    // Đưa danh sách bài hát vào adapter hoặc làm gì đó với nó
                    Intent intent = new Intent(getContext(), MusicService.class);
                    intent.setAction("setSongList");
                    intent.putExtra("songList", (Serializable) songList);
                    requireContext().startService(intent);
                })
                .addOnFailureListener(e -> {
                    // Xử lý khi có lỗi xảy ra
                    Log.e("lỗi", "Lỗi khi lấy danh sách bài hát từ Firestore: " + e.getMessage());
                });
    }

    public void setFavoriteSongs() {
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
                                    if (document.contains("favorites")) {
                                        // Lấy danh sách các bài hát từ trường "favorites"
                                        List<String> favoriteSongIds = new ArrayList<>();
                                        Object favoritesObj = document.get("favorites");
                                        if (favoritesObj instanceof Map) {
                                            Map<String, Boolean> favoritesMap = (Map<String, Boolean>) favoritesObj;
                                            for (Map.Entry<String, Boolean> entry : favoritesMap.entrySet()) {
                                                if (entry.getValue()) {
                                                    favoriteSongIds.add(entry.getKey());
                                                }
                                            }

                                            // Lấy danh sách bài hát từ collection "songs" dựa trên favoriteSongIds
                                            getSongs(favoriteSongIds);
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

    public void setPlaylistSongs() {
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
            db.collection("songs")
                    .whereIn(FieldPath.documentId(), songIds)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<Song> songs = new ArrayList<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    // Chuyển đổi dữ liệu từ DocumentSnapshot thành đối tượng Song và thêm vào danh sách
                                    Song song = document.toObject(Song.class);
                                    songs.add(song);
                                }

                                // Xử lý danh sách các bài hát yêu thích ở đây
                                Intent intent = new Intent(getContext(), MusicService.class);
                                intent.setAction("setSongList");
                                intent.putExtra("songList", (Serializable) songs);
                                requireContext().startService(intent);
                            } else {
                                // Xử lý lỗi
                            }
                        }
                    });
        }
    }

    public int getCurrentFragment() {
        return currentFragment;
    }
}