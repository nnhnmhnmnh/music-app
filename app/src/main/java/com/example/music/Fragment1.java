package com.example.music;

import android.Manifest;
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

import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import static android.app.Activity.RESULT_OK;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment1#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment1 extends Fragment {
    private RecyclerView recyclerView;
    private SearchView searchLocal;
    private List<Song> songList;
//    private LinearLayout currentSong;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Fragment1() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Fragment1.
     */
    // TODO: Rename and change types and number of parameters
    public static Fragment1 newInstance(String param1, String param2) {
        Fragment1 fragment = new Fragment1();
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
        View view = inflater.inflate(R.layout.fragment_1, container, false);

        searchLocal = view.findViewById(R.id.searchLocal);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 1234);
            return;
        }

        // Lấy danh sách bài hát từ Firestore hoặc MediaStore
        getAllLocalSongs();

        searchLocal.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                List<Song> songListQuery = searchSongs(songList, query);
                Intent intent = new Intent(getContext(), MusicService.class);
                intent.setAction("setSongList");
                intent.putExtra("songList", (Serializable) songListQuery);
                requireContext().startService(intent);
                SongAdapter adapter = new SongAdapter(songListQuery, requireContext());
                recyclerView.setAdapter(adapter);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) getAllLocalSongs();
                return true;
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1234) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getAllLocalSongs();
            } else {
                Toast.makeText(requireContext(), "Ứng dụng cần quyền để truy cập danh sách bài hát", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getAllLocalSongs() {
        Cursor cursor = requireContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DISPLAY_NAME}, "1=1", null, null);

        Log.d("aaa", String.valueOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI));
        songList = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int displayNameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);

                int id = cursor.getInt(idIndex);
                String data = cursor.getString(dataIndex);
                String artist = cursor.getString(artistIndex);
                String album = cursor.getString(albumIndex);
                String displayName = cursor.getString(displayNameIndex);

                Song song = new Song(data, artist, album, displayName, null, null);
                songList.add(song);
            } while (cursor.moveToNext());

            cursor.close();
        }
        // Đưa danh sách bài hát vào adapter hoặc làm gì đó với nó
        Intent intent = new Intent(getContext(), MusicService.class);
        intent.setAction("setSongList");
        intent.putExtra("songList", (Serializable) songList);
        requireContext().startService(intent);

        SongAdapter adapter = new SongAdapter(songList, requireContext());
        recyclerView.setAdapter(adapter);
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
}