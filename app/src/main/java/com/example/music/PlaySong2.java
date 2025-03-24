package com.example.music;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaySong2#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaySong2 extends Fragment {
    List<String> lyrics;
    List<Integer> time;
    LinearLayout lyricsContainer;
    private MediaPlayer mediaPlayer;
    private MediaPlayerManager mediaPlayerManager;
    ScrollView scrollView;
    TextView textViewNoLyric;
    private ImageView addLyric;
    String songData, lrcUrl;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PlaySong2() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PlaySong2.
     */
    // TODO: Rename and change types and number of parameters
    public static PlaySong2 newInstance(String param1, String param2) {
        PlaySong2 fragment = new PlaySong2();
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
        View view = inflater.inflate(R.layout.fragment_play_song2, container, false);

        lyricsContainer = view.findViewById(R.id.lyricsContainer);
        scrollView = view.findViewById(R.id.lyricsScrollView);
        textViewNoLyric = view.findViewById(R.id.textViewNoLyric);
        addLyric = view.findViewById(R.id.addLyric);

        mediaPlayerManager = MediaPlayerManager.getInstance();
        mediaPlayer = mediaPlayerManager.getMediaPlayer();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedSongData != null) {
            getLyric(savedSongData, savedLrcUrl);
            if (!savedSongData.contains("firebase")) {
                addLyric.setVisibility(View.GONE);
            } else {
                songData = savedSongData;
            }
            savedSongData = null;
            savedLrcUrl = null;
        } else {
            songData = requireActivity().getIntent().getStringExtra("songData");
            if (!songData.contains("firebase")) {
                addLyric.setVisibility(View.GONE);
            }
            lrcUrl = requireActivity().getIntent().getStringExtra("songLrc");
            getLyric(songData, lrcUrl);
        }

        addLyric.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạo một Intent để mở hộp thoại chọn tệp tin
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/lrc"); // Chỉ cho phép chọn lrc
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Kiểm tra xem có ứng dụng nào có thể xử lý Intent này hay không
                try {
                    startActivityForResult(Intent.createChooser(intent, "Chọn tệp tin lrc"), 123);
                } catch (android.content.ActivityNotFoundException ex){
                    // Xử lý khi không tìm thấy ứng dụng để chọn tệp tin
                    Toast.makeText(requireActivity(), "Không tìm thấy ứng dụng để chọn tệp tin", Toast.LENGTH_SHORT).show();
                }
            }
        });

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

    @SuppressLint("MissingPermission")
    private void showUploadNotification(Uri lrcFile) {
        // Lấy tham chiếu đến Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Tạo tham chiếu đến một vị trí duy nhất trong Storage để lưu tệp tin bài hát
        String fileName = lrcFile.getLastPathSegment(); // Lấy tên tệp tin lrc
        StorageReference lrcRef = storageRef.child("lrc/" + fileName);
        Log.d("fileName", fileName); // primary:Music/Lovely - Billie Eilish_ Khalid.lrc

        // Tải tệp tin bài hát lên Firebase Storage
        UploadTask uploadTask = lrcRef.putFile(lrcFile); // lrcFile là Uri của tệp tin lrc

        // Tạo thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "upload_channel")
                .setSmallIcon(R.drawable.round_upload_24)
                .setContentTitle("Đang tải lên")
                .setContentText("Đang tải lrc...")
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
                            // Lấy URL tải xuống của tệp tin lrc
                            lrcRef.getDownloadUrl().addOnSuccessListener(lrcUri -> {
                                String lrcDownloadUrl = lrcUri.toString();

                                // Lưu thông tin lrc vào document trong Firestore
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                CollectionReference songsCollection = db.collection("songs");

                                HashMap<String, Object> lrcData = new HashMap<>();
                                lrcData.put("lrc", lrcDownloadUrl);

                                // Tìm document có field "data" trùng với "songData" và cập nhật thêm field "lrc"
                                songsCollection.whereEqualTo("data", songData).get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            // Kiểm tra xem có document nào trả về không
                                            if (!queryDocumentSnapshots.isEmpty()) {
                                                // Lấy document đầu tiên
                                                DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                                                // Cập nhật document với thêm field "lrc"
                                                documentSnapshot.getReference().update(lrcData)
                                                        .addOnSuccessListener(aVoid -> {
                                                            // Xử lý khi cập nhật thành công
                                                            Log.d("thành công", "Field 'lrc' đã được cập nhật cho document");
                                                            // Hiển thị thông báo tải lên thành công
                                                            builder.setContentTitle("Tải lên thành công")
                                                                    .setContentText("Tệp tin lrc đã được tải lên và cập nhật thành công!")
                                                                    .setProgress(0, 0, false)
                                                                    .setOngoing(false);
                                                            notificationManager.notify(notificationId, builder.build());
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            // Xử lý khi cập nhật gặp lỗi
                                                            Log.e("lỗi", "Lỗi khi cập nhật field 'lrc' cho document: " + e.getMessage());
                                                            // Hiển thị thông báo tải lên thất bại
                                                            builder.setContentTitle("Tải lên thất bại")
                                                                    .setContentText("Đã xảy ra lỗi khi cập nhật tệp tin lrc!")
                                                                    .setProgress(0, 0, false)
                                                                    .setOngoing(false);
                                                            notificationManager.notify(notificationId, builder.build());
                                                        });
                                            } else {
                                                // Xử lý khi truy vấn Firestore gặp lỗi
                                                Log.e("lỗi", "Không tìm thấy document có field 'data' trùng với 'songData'");
                                                // Hiển thị thông báo tải lên thất bại
                                                builder.setContentTitle("Tải lên thất bại")
                                                        .setContentText("Không tìm thấy bài hát để cập nhật tệp tin lrc!")
                                                        .setProgress(0, 0, false)
                                                        .setOngoing(false);
                                                notificationManager.notify(notificationId, builder.build());
                                            }
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
            }
        });
    }

    private String savedSongData;
    private String savedLrcUrl;
    public void receiveData(String songData, String lrcUrl) {
        if (isAdded()) {
            // Log.d("isAdded() && isResumed()", "isAdded() && isResumed()");
            getLyric(songData, lrcUrl);
        } else {
            // Lưu lại dữ liệu và ghi nhớ gọi getLyric() sau khi fragment được kết nối và hiển thị
            savedSongData = songData;
            savedLrcUrl = lrcUrl;
        }
    }

    private void getLyric(String songData, String lrcUrl) {
        if (songData.contains("firebase")) {
            if (lrcUrl != null) {
                textViewNoLyric.setVisibility(View.GONE);
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(lrcUrl).build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.d("onFailure", e.getMessage());
                    }
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String text = response.body().string();
                        time = new ArrayList<>();
                        lyrics = new ArrayList<>();
                        String[] lines = text.split("\n");
                        for (String line : lines) {
                            String[] parts = line.split("]");
                            if (parts.length >= 2) {
                                time.add(parseTimestamp(parts[0].substring(1).trim()));
                                lyrics.add(parts[1].trim());
                            }
                        }
                        getActivity().runOnUiThread(() -> {
                            lyricsContainer.removeAllViews(); // Xóa các TextView cũ trong lyricsContainer
                            for (String line : lyrics) {
                                TextView textView = new TextView(requireContext());
                                textView.setText(line);
                                textView.setTextSize(16);
                                textView.setPadding(50, 50, 50, 50);
                                lyricsContainer.addView(textView);

                                textView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Toast.makeText(getContext(), textView.getText().toString(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            playLyric();
                        });
                    }
                });
            } else { // not found lrc from firebase
                textViewNoLyric.setVisibility(View.VISIBLE);
            }
        } else { // local
            String text = getTextFromLrcFile(songData);
            if (!text.isEmpty()) {
                time = new ArrayList<>();
                lyrics = new ArrayList<>();
                textViewNoLyric.setVisibility(View.GONE);
                lyricsContainer.removeAllViews(); // Xóa các TextView cũ trong lyricsContainer
                String[] lines = text.split("\n");
                for (String line : lines) {
                    String[] parts = line.split("]");
                    if (parts.length >= 2) {
                        time.add(parseTimestamp(parts[0].substring(1).trim()));
                        lyrics.add(parts[1].trim());
                    }
                }
                for (String line : lyrics) {
                    TextView textView = new TextView(requireContext());
                    textView.setText(line);
                    textView.setTextSize(16);
                    textView.setPadding(50, 50, 50, 50);
                    lyricsContainer.addView(textView);

                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(getContext(), textView.getText().toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                playLyric();
            } else { // not found lyric from local
                lyricsContainer.removeAllViews(); // Xóa các TextView cũ trong lyricsContainer
                textViewNoLyric.setVisibility(View.VISIBLE);
            }
        }
    }

    private void playLyric() {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int currentPosition = mediaPlayer.getCurrentPosition();
                // Tìm và hiển thị lời bài hát tương ứng với thời gian hiện tại
                int currentLyricIndex = findCurrentLyricIndex(time, currentPosition);
                displayLyrics(currentLyricIndex);

                handler.postDelayed(this, 1000); // Cập nhật lời bài hát mỗi 100ms
            }
        };
        handler.postDelayed(runnable, 1000);

    }

    private void displayLyrics(int currentLyricIndex) {
        // Đặt màu chữ của tất cả TextView trong lyricsContainer về mặc định
        for (int i = 0; i < lyricsContainer.getChildCount(); i++) {
            TextView textView = (TextView) lyricsContainer.getChildAt(i);
            textView.setTextColor(Color.GRAY);
            textView.setTextSize(16);
        }

        // Kiểm tra nếu có lời bài hát tương ứng với thời gian hiện tại, thì highlight và scroll đến vị trí đó
        if (currentLyricIndex != -1) {
            TextView currentLyricTextView = (TextView) lyricsContainer.getChildAt(currentLyricIndex);
            if (currentLyricTextView != null) {
                currentLyricTextView.setTextColor(Color.WHITE);
                currentLyricTextView.setTextSize(24);

                // Scroll đến vị trí của TextView chứa lời bài hát tương ứng
                int scrollY = currentLyricTextView.getTop() - (scrollView.getHeight() / 2);
                scrollView.smoothScrollTo(0, scrollY);
            }
        }
    }

    public String getTextFromLrcFile(String musicFilePath) {
        String lrcFilePath = musicFilePath.replaceFirst("\\.[^.]+$", ".lrc"); // Thay đổi phần mở rộng thành .lrc
        File file = new File(lrcFilePath);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                // Bỏ qua các dòng không phải là lời bài hát
                if (line.startsWith("[") && !line.startsWith("[ti:") && !line.startsWith("[ar:") && !line.startsWith("[al:")) {
                    text.append(line).append("\n");
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    private static int parseTimestamp(String time) {
        String[] parts = time.split(":");
        int minutes = Integer.parseInt(parts[0]);
        String[] secondsParts = parts[1].split("\\.");
        int seconds = Integer.parseInt(secondsParts[0]);
        int milliseconds = Integer.parseInt(secondsParts[1]);
        return minutes * 60000 + seconds * 1000 + milliseconds;
    }

    // Tìm chỉ số của lời bài hát dựa trên thời gian hiện tại
    private int findCurrentLyricIndex(List<Integer> timestamps, int currentTimestamp) {
        int lastIndex = timestamps.size() - 1;

        // Nếu thời gian hiện tại nhỏ hơn thời gian đầu tiên trong list timestamps,
        // ta coi lời bài hát tại vị trí đầu tiên đang được hiển thị
        if (currentTimestamp < timestamps.get(0)) {
            return 0;
        }

        // Nếu thời gian hiện tại lớn hơn thời gian cuối cùng trong list timestamps,
        // ta coi lời bài hát tại vị trí cuối cùng đang được hiển thị
        if (currentTimestamp >= timestamps.get(lastIndex)) {
            return lastIndex;
        }

        // Sử dụng thuật toán tìm kiếm nhị phân để tìm vị trí của thời gian hiện tại trong list timestamps
        int left = 0;
        int right = lastIndex;
        int currentIndex = 0;

        while (left <= right) {
            currentIndex = (left + right) / 2;
            long current = timestamps.get(currentIndex);
            long next = timestamps.get(currentIndex + 1);

            if (current <= currentTimestamp && currentTimestamp < next) {
                break;
            } else if (currentTimestamp < current) {
                right = currentIndex - 1;
            } else {
                left = currentIndex + 1;
            }
        }

        return currentIndex;
    }

}