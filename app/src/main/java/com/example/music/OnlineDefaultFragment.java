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

import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OnlineDefaultFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OnlineDefaultFragment extends Fragment {
    private RecyclerView recyclerView;
    ImageView btnUpload;
    private FirebaseFirestore db;
    private CollectionReference songsCollection;
    SearchView searchOnline;
    List<Song> songList;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public OnlineDefaultFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OnlineDefaultFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OnlineDefaultFragment newInstance(String param1, String param2) {
        OnlineDefaultFragment fragment = new OnlineDefaultFragment();
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
        View view = inflater.inflate(R.layout.fragment_online_default, container, false);

        btnUpload = view.findViewById(R.id.btnUpload);
        recyclerView = view.findViewById(R.id.recyclerView2);
        searchOnline = view.findViewById(R.id.searchOnline);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        createNotificationChannel();

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();
        songsCollection = db.collection("songs");

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạo một Intent để mở hộp thoại chọn tệp tin
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*"); // Chỉ cho phép chọn tệp tin âm thanh
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Kiểm tra xem có ứng dụng nào có thể xử lý Intent này hay không
                try {
                    startActivityForResult(Intent.createChooser(intent, "Chọn tệp tin nhạc"), 123);
                } catch (android.content.ActivityNotFoundException ex) {
                    // Xử lý khi không tìm thấy ứng dụng để chọn tệp tin
                    Toast.makeText(requireActivity(), "Không tìm thấy ứng dụng để chọn tệp tin", Toast.LENGTH_SHORT).show();
                }
            }
        });

        getAllSongs();

        searchOnline.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
                if (newText.isEmpty()) getAllSongs();
                return true;
            }
        });
    }

    private void getAllSongs() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference songsCollection = db.collection("songs"); // Tạo một bộ sưu tập "songs"
        songsCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    songList = new ArrayList<>();

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
                    // Ví dụ:
                    SongAdapter adapter = new SongAdapter(songList, requireContext());
                    recyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    // Xử lý khi có lỗi xảy ra
                    Log.e("lỗi", "Lỗi khi lấy danh sách bài hát từ Firestore: " + e.getMessage());
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 123 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri selectedFileUri = data.getData();
                // Tiếp tục xử lý tệp tin được chọn
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    showUploadNotification(selectedFileUri);
                } else {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
            }
        }
    }

    private Bitmap getAlbumArtFromUri(Uri musicUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        byte[] albumArtBytes = null;
        Bitmap albumArtBitmap = null;
        try {
            retriever.setDataSource(getContext(), musicUri);
            albumArtBytes = retriever.getEmbeddedPicture();
            if (albumArtBytes != null) {
                albumArtBitmap = BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return albumArtBitmap;
    }

    @SuppressLint("MissingPermission")
    private void showUploadNotification(Uri songFile) {
        // Lấy tham chiếu đến Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Tạo tham chiếu đến một vị trí duy nhất trong Storage để lưu tệp tin bài hát
        // String fileName = songFile.getLastPathSegment(); // Lấy tên tệp tin bài hát
        StorageReference songRef = storageRef.child("songs/" + getFileNameFromUri(songFile));

        // Tải tệp tin bài hát lên Firebase Storage
        UploadTask uploadTask = songRef.putFile(songFile); // songFile là Uri của tệp tin bài hát

        // Tạo thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "upload_channel")
                .setSmallIcon(R.drawable.round_upload_24)
                .setContentTitle("Đang tải lên")
                .setContentText("Đang tải bài hát...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, 0, true)
                .setOngoing(true);

        // Hiển thị thông báo
        int notificationId = 1; // ID duy nhất cho thông báo
        notificationManager.notify(notificationId, builder.build());

        final boolean[] isCollectionAdded = {false}; // Biến cờ để theo dõi việc thêm collection vào Firestore

        // Thêm ProgressListener để lấy tiến trình tải lên thực tế
        uploadTask.addOnProgressListener(taskSnapshot -> {
            // Lấy tiến trình tải lên
            long totalBytes = taskSnapshot.getTotalByteCount();
            long bytesUploaded = taskSnapshot.getBytesTransferred();
            int progress = (int) ((bytesUploaded * 100) / totalBytes);

            // Cập nhật tiến độ trên thanh thông báo
            builder.setProgress(100, progress, false);
            notificationManager.notify(notificationId, builder.build());

            if (!isCollectionAdded[0]) {
                isCollectionAdded[0] = true; // Đánh dấu rằng collection đã được thêm vào Firestore
                // Lắng nghe sự kiện khi quá trình tải lên hoàn thành
                uploadTask.addOnSuccessListener(successSnapshot -> {
                    // Lấy URL tải xuống của tệp tin bài hát
                    songRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Lấy ảnh đại diện từ songFile
                        Bitmap avtBitmap = getAlbumArtFromUri(songFile);
//                        Log.d("avtBitmap", String.valueOf(avtBitmap));

                        // Lưu ảnh đại diện vào Firebase Storage
                        String avtFileName = "avt_" + getFileNameFromUri(songFile);
                        StorageReference avtRef = storageRef.child("avt/" + avtFileName);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        avtBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] avtData = baos.toByteArray();
                        UploadTask avtUploadTask = avtRef.putBytes(avtData);

                        avtUploadTask.addOnSuccessListener(avtTaskSnapshot -> {
                                    // Lấy URL tải xuống của ảnh đại diện
                                    avtRef.getDownloadUrl().addOnSuccessListener(avtUri -> {
                                        String avtDownloadUrl = avtUri.toString();

                                        // Lấy các thông tin từ tệp tin bài hát
                                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                        retriever.setDataSource(requireContext(), songFile); // context là đối tượng Context

                                        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                                        String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                                        String displayName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

                                        try {
                                            retriever.release();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        // Kiểm tra và gán giá trị mặc định nếu các trường là null
                                        artist = artist != null ? artist : "<unknown>";
                                        album = album != null ? album : "<unknown>";
                                        displayName = displayName != null ? displayName : getFileNameFromUri(songFile).split(".mp3")[0];

                                        // Lưu thông tin bài hát vào collection "songs" trên Firestore
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        CollectionReference songsCollection = db.collection("songs");

                                        HashMap<String, Object> songData = new HashMap<>();
                                        songData.put("displayName", displayName);
                                        songData.put("artist", artist);
                                        songData.put("album", album);
                                        songData.put("data", downloadUrl);
                                        songData.put("avt", avtDownloadUrl); // Thêm đường dẫn ảnh đại diện vào dữ liệu bài hát

                                        songsCollection.add(songData)
                                                .addOnSuccessListener(documentReference -> {
                                                    // Xử lý khi lưu thông tin bài hát thành công
                                                    String songId = documentReference.getId();
                                                    Log.d("thành công", "Thông tin bài hát đã được lưu trữ với ID: " + songId);
                                                    // Hiển thị thông báo tải lên thành công
                                                    builder.setContentTitle("Tải lên thành công")
                                                            .setContentText("Bài hát đã được tải lên thành công!")
                                                            .setProgress(0, 0, false)
                                                            .setOngoing(false);
                                                    notificationManager.notify(notificationId, builder.build());
                                                })
                                                .addOnFailureListener(e -> {
                                                    // Xử lý khi lưu thông tin bài hát gặp lỗi
                                                    Log.e("lỗi", "Lỗi khi lưu thông tin bài hát vào Firestore: " + e.getMessage());
                                                    // Hiển thị thông báo tải lên thất bại
                                                    builder.setContentTitle("Tải lên thất bại")
                                                            .setContentText("Đã xảy ra lỗi khi tải lên bài hát!")
                                                            .setProgress(0, 0, false)
                                                            .setOngoing(false);
                                                    notificationManager.notify(notificationId, builder.build());
                                                });
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    // Xử lý khi tải lên tệp tin bài hát gặp lỗi
                                    Log.e("lỗi", "Lỗi khi tải lên tệp tin bài hát: " + e.getMessage());
                                    // Hiển thị thông báo tải lên thất bại
                                    builder.setContentTitle("Tải lên thất bại")
                                            .setContentText("Đã xảy ra lỗi khi tải lên bài hát!")
                                            .setProgress(0, 0, false)
                                            .setOngoing(false);
                                    notificationManager.notify(notificationId, builder.build());
                                });
                    });
                }).addOnFailureListener(e -> {
                    // Xử lý khi tải lên tệp tin bài hát gặp lỗi
                    Log.e("lỗi", "Lỗi khi tải lên tệp tin bài hát: " + e.getMessage());
                    // Hiển thị thông báo tải lên thất bại
                    builder.setContentTitle("Tải lên thất bại")
                            .setContentText("Đã xảy ra lỗi khi tải lên bài hát!")
                            .setProgress(0, 0, false)
                            .setOngoing(false);
                    notificationManager.notify(notificationId, builder.build());
                });
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("upload_channel", "Upload Channel", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Channel for upload notifications");

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public String getFileNameFromUri(Uri uri){
        ContentResolver contentResolver = requireContext().getContentResolver(); // getContentResolver() là phương thức của lớp Context

        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            cursor.close();

            if (displayName != null) {
                return displayName;
            }
        }
        return null;
    }
}