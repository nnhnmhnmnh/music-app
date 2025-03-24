package com.example.music;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaySong1#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaySong1 extends Fragment {
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView current, duration, tvTitle, tvArtist;
    private ImageView stop, previous, next, download, avt, favorite, share, timer, repeat, playlist;
    private MediaPlayerManager mediaPlayerManager;
    private String songData, avtUrl, lrcUrl, songArtist, songTitle;
    private LinearLayout linearLayout;
    private BroadcastReceiver pauseButtonClickedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case "pausebuttonclicked":
                    stop.setImageResource(R.drawable.round_play_arrow_24);
                    break;
                case "resumebuttonclicked":
                    stop.setImageResource(R.drawable.round_pause_24);
                    break;
                case "previous-nextbuttonclicked":
                    //Log.d("playNext", "previous-nextbuttonclicked");
                    songData = intent.getStringExtra("songData");
                    lrcUrl = intent.getStringExtra("songLrc");
                    avtUrl = intent.getStringExtra("songAvt");
                    songArtist = intent.getStringExtra("songArtist");
                    songTitle = intent.getStringExtra("songTitle");
                    playSong(songData);
                    ((PlaySong) requireActivity()).passDataToPlaySong2(songData, lrcUrl);
                    break;
            }
        }
    };



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PlaySong1() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PlaySong1.
     */
    // TODO: Rename and change types and number of parameters
    public static PlaySong1 newInstance(String param1, String param2) {
        PlaySong1 fragment = new PlaySong1();
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
        View view = inflater.inflate(R.layout.fragment_play_song1, container, false);

        mediaPlayerManager = MediaPlayerManager.getInstance();
        mediaPlayer = mediaPlayerManager.getMediaPlayer();

        seekBar = view.findViewById(R.id.seekBar);
        current = view.findViewById(R.id.current);
        duration = view.findViewById(R.id.duration);
        stop = view.findViewById(R.id.stop);
        previous = view.findViewById(R.id.previous);
        next = view.findViewById(R.id.next);
        download = view.findViewById(R.id.download);
        avt = view.findViewById(R.id.avt);
        favorite = view.findViewById(R.id.favorite);
        share = view.findViewById(R.id.share);
        timer = view.findViewById(R.id.timer);
        repeat = view.findViewById(R.id.repeat);
        playlist = view.findViewById(R.id.playlist);
        tvArtist = view.findViewById(R.id.tvArtist);
        tvTitle = view.findViewById(R.id.tvTitle);
        linearLayout = view.findViewById(R.id.linearLayout);
        //Log.d("playsong1", String.valueOf(getAvt(requireActivity().getIntent().getStringExtra("songData"))));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Lấy đường dẫn bài hát từ Intent
//        songData = getArguments().getString("songData");
        songData = requireActivity().getIntent().getStringExtra("songData");
        avtUrl = requireActivity().getIntent().getStringExtra("songAvt");
        songArtist = requireActivity().getIntent().getStringExtra("songArtist");
        songTitle = requireActivity().getIntent().getStringExtra("songName");
        lrcUrl = requireActivity().getIntent().getStringExtra("songLrc");
        tvTitle.setSelected(true);
        // Log.d("songData", songData);

        // Phát bài hát
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            playSong(songData);
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        createNotificationChannel();

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    stopSong();
                } else {
                    resumeSong();
                }
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousSong();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextSong();
            }
        });

        if (getRepeatState()) {
            repeat.setImageResource(R.drawable.ic_repeat_one);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(new Intent("repeat-one"));
        } else {
            repeat.setImageResource(R.drawable.ic_repeat);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(new Intent("repeat-all"));
        }
        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getRepeatState()) {
                    repeat.setImageResource(R.drawable.ic_repeat);
                    saveRepeatState(false);
                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(new Intent("repeat-all"));
                } else {
                    repeat.setImageResource(R.drawable.ic_repeat_one);
                    saveRepeatState(true);
                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(new Intent("repeat-one"));
                }
            }
        });

        timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimerDialog();
            }
        });

        // ...
        // Đăng ký BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("pausebuttonclicked");
        filter.addAction("resumebuttonclicked");
        filter.addAction("previous-nextbuttonclicked");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(pauseButtonClickedReceiver, filter);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("download_channel", "Download Channel", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Channel for download notifications");

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Xử lý kết quả yêu cầu quyền từ người dùng
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, tiến hành hiển thị thông báo
//                showDownloadNotification();
            } else {
                // Quyền bị từ chối, thông báo cho người dùng
                Toast.makeText(requireContext(), "Ứng dụng cần quyền để hiển thị thông báo tải xuống.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void showDownloadNotification(String songData) {
        // Tạo thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "download_channel")
                .setSmallIcon(R.drawable.round_download_24)
                .setContentTitle("Đang tải xuống")
                .setContentText("Đang tải bài hát...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100, 0, true)
                .setOngoing(true);

        // Hiển thị thông báo
        int notificationId = 1; // ID duy nhất cho thông báo
        notificationManager.notify(notificationId, builder.build());

        // Tham chiếu đến Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Tách phần audio:1000000038 từ đường dẫn
        String audioId = null;
        try {
            // Giải mã URL
            String decodedUrl = URLDecoder.decode(songData, "UTF-8");

            // Tìm vị trí audioId trong URL đã giải mã
            int startIndex = decodedUrl.lastIndexOf("audio:");
            int endIndex = decodedUrl.indexOf("?alt=media");

            // Trích xuất audioId từ URL đã giải mã
            audioId = decodedUrl.substring(startIndex, endIndex);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Tham chiếu đến thư mục chứa bài hát trên Firebase Storage
        StorageReference storageRef = storage.getReference().child("songs/" + audioId);

        // Tạo một tệp tin trên thiết bị để lưu trữ bài hát tải xuống
        File localFile = new File(requireContext().getExternalFilesDir(null), requireActivity().getIntent().getStringExtra("songName") + ".mp3");

        // Tải xuống bài hát từ Firebase Storage và lưu vào tệp tin trên thiết bị
        FileDownloadTask downloadTask = storageRef.getFile(localFile);

        // Thêm ProgressListener để lấy tiến trình tải xuống thực tế
        downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // Lấy tiến trình tải xuống
                        long totalBytes = taskSnapshot.getTotalByteCount();
                        long bytesDownloaded = taskSnapshot.getBytesTransferred();
                        int progress = (int) ((bytesDownloaded * 100) / totalBytes);

                        // Cập nhật tiến độ trên thanh thông báo
                        builder.setProgress(100, progress, false);
                        notificationManager.notify(notificationId, builder.build());
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // Khi tải xuống hoàn thành, cập nhật thông báo và ẩn nó
                        builder.setContentText("Tải xuống hoàn thành")
                                .setProgress(0, 0, false)
                                .setOngoing(false);
                        notificationManager.notify(notificationId, builder.build());

                        // Xử lý khi tải xuống thành công
                        Toast.makeText(requireContext(), "Tải xuống thành công", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Khi tải xuống thất bại, cập nhật thông báo và ẩn nó
                        builder.setContentText("Tải xuống thất bại")
                                .setProgress(0, 0, false)
                                .setOngoing(false);
                        notificationManager.notify(notificationId, builder.build());

                        // Xử lý khi tải xuống thất bại
                        Toast.makeText(requireContext(), "Tải xuống thất bại", Toast.LENGTH_SHORT).show();
                        Log.d("onFailure", String.valueOf(exception));
                    }
                });
    }

    private void saveRepeatState(boolean isRepeatOne) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("RepeatStatePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isRepeatOne", isRepeatOne);
        editor.apply();
    }

    private boolean getRepeatState() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("RepeatStatePrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("isRepeatOne", false);
    }

    private void saveSongData(String songData) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("songData", songData);
        editor.apply();
    }

    private void saveSong(String songAvt, String songName, String songArtist, String songLrc) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SongPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("songAvt", songAvt);
        editor.putString("songName", songName);
        editor.putString("songArtist", songArtist);
        editor.putString("songLrc", songLrc);
        editor.apply();
    }

    private String loadSongData() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("songData", null);
    }

    private int getCurrentPlaybackTime() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("PlaybackPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("currentPosition", 0);
    }

    private boolean isMediaPlayerDataSourceValid(MediaPlayer mediaPlayer) {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private void playSong(String songData) {
//        Log.d("playSongggggggggggg1", loadSongData());
//        Log.d("playSongggggggggggg2", songData);
        if (loadSongData() != null) {
            if (mediaPlayer != null && loadSongData().equals(songData)) {
                if (!isMediaPlayerDataSourceValid(mediaPlayer)) {
                    // Log.d("playSongggggggggggg3", "invalid");
                    // Hiển thị ProgressDialog
                    ProgressDialog progressDialog = new ProgressDialog(requireContext());
                    progressDialog.setMessage("Đang tải bài hát...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    try {
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(songData);
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                // mp.start();
                                mp.seekTo(getCurrentPlaybackTime());
                                resumeSong();
                                updateUI();
                                progressDialog.dismiss();
                                // Log.d("playSonggggggggggggValid", String.valueOf(isMediaPlayerDataSourceValid(mediaPlayer)));
                            }
                        });
                        mediaPlayer.prepareAsync();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                resumeSong();
                updateUI();
                return;
            }
        }

        saveSongData(songData);
        saveSong(avtUrl, songTitle, songArtist, lrcUrl);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        // Hiển thị ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang tải bài hát...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            mediaPlayer.setDataSource(songData);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // Cập nhật trạng thái của giao diện người dùng, ví dụ: nút play/pause và thanh trạng thái
                    updateUI();
                    mp.start();

                    Intent intent = new Intent(requireContext(), MusicService.class);
                    intent.setAction("showNotification");
                    requireContext().startService(intent);

                    progressDialog.dismiss();
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            progressDialog.dismiss(); // Ẩn ProgressDialog nếu xảy ra lỗi
        }
    }

    private void updateUI() {
        int songDuration = getSongDuration(songData);
        seekBar.setMax(songDuration);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                    resumeSong();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        duration.setText(formatDuration(songDuration));
        updateCurrentTime(current, seekBar);

        tvTitle.setText(songTitle);
        tvArtist.setText(songArtist);

        if (songData.contains("firebase")) {
            download.setImageResource(R.drawable.round_download_24);
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        showDownloadNotification(songData);
                    } else {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                    }
                }
            });

            if (avtUrl != null) {
                Picasso.get().load(avtUrl).into(avt);
//                Picasso.get().load(avtUrl).into(new Target() {
//                    @Override
//                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                        // Sử dụng bitmap ở đây
//                        setBackground(bitmap);
//                    }
//
//                    @Override
//                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
//                        // Xử lý khi tải ảnh thất bại
//                    }
//
//                    @Override
//                    public void onPrepareLoad(Drawable placeHolderDrawable) {
//                        // Xử lý trước khi tải ảnh
//                    }
//                });
            } else {
                avt.setImageResource(R.drawable.round_music_note_24);
                // linearLayout.setBackgroundColor(Color.parseColor("#222222"));
            }

            findSongIdAndCheckFavoriteList(songData);
            findSongIdAndCheckPlaylist(songData);

            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String songArtist = requireActivity().getIntent().getStringExtra("songArtist");
                    String songName = requireActivity().getIntent().getStringExtra("songName");
                    shareSong(songData, songArtist, songName);
                }
            });
        } else {
            favorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(requireContext(), "Thêm vào yêu thích chỉ khả dụng trực tuyến", Toast.LENGTH_SHORT).show();
                }
            });
            playlist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(requireContext(), "Thêm vào danh sách phát chỉ khả dụng trực tuyến", Toast.LENGTH_SHORT).show();
                }
            });
            download.setImageResource(R.drawable.round_download_done_24);
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(requireContext(), "Bài hát đã được tải xuống", Toast.LENGTH_SHORT).show();
                }
            });

            if (getAvt(songData) != null) {
                avt.setImageBitmap(getAvt(songData));
                // setBackground(getAvt(songData));
            } else {
                avt.setImageResource(R.drawable.round_music_note_24);
                // linearLayout.setBackgroundColor(Color.parseColor("#222222"));
            }

            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareSongFile(songData);
                }
            });
        }
    }

    private void showTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Hẹn giờ tắt nhạc sau");
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_timer, null);
        builder.setView(view);

        final TextView timeTextView = view.findViewById(R.id.timeTextView);
        final SeekBar seekBar = view.findViewById(R.id.seekBarTimer);

        // Thiết lập giá trị mặc định cho thanh trượt
        seekBar.setProgress(5);

        // Hiển thị giá trị mặc định trên TextView
        timeTextView.setText("5 phút");

        // Xử lý sự kiện khi giá trị thanh trượt thay đổi
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int selectedTime = progress + 1; // +1 do giá trị progress bắt đầu từ 0
                timeTextView.setText(String.valueOf(selectedTime) + " phút");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        builder.setPositiveButton("Đặt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int selectedTime = seekBar.getProgress() + 1; // +1 do giá trị progress bắt đầu từ 0
                // selectedTimeTextView.setText(String.valueOf(selectedTime));
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopSong();
                    }
                }, selectedTime * 60000); // Gọi stopSong() sau khoảng thời gian đã chọn
            }
        });

        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void shareSong(String songData, String songArtist, String songName) {
        String shareTitle = songName;
        String shareText;
        if (songArtist != null && !songArtist.equals("<unknown>")) {
            shareText = "Nghe bài hát " + songName + " của " + songArtist + ":\n" + songData;
        } else {
            shareText = "Nghe bài hát " + songName + ":\n" + songData;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareTitle);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Chia sẻ bài hát"));
    }

    private void shareSongFile(String filePath) {
        if (filePath != null) {
            // Tạo Uri từ đường dẫn tệp tin
            Uri fileUri = Uri.parse(filePath);

            // Tạo Intent với hành động ACTION_SEND
            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            // Đặt loại nội dung là audio/mp3
            shareIntent.setType("audio/mp3");

            // Đặt Uri của tệp tin là dữ liệu cần chia sẻ
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

            // Khởi chạy Intent để chia sẻ tệp tin
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ tệp tin bài hát"));
        }
    }

    private void addFavorite(String songId)  {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            // Log.d("addFavorite2", userId);
            // Log.d("addFavorite1", String.valueOf(songId));
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.update("favorites." + songId, true)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Bài hát đã được thêm vào danh sách yêu thích thành công
                            Toast.makeText(requireContext(), "Bài hát đã được thêm vào danh sách yêu thích", Toast.LENGTH_SHORT).show();
                            favorite.setImageResource(R.drawable.ic_favorite_fill);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xảy ra lỗi khi thêm bài hát vào danh sách yêu thích
                            Toast.makeText(requireContext(), "Xảy ra lỗi", Toast.LENGTH_SHORT).show();
                            Log.d("addFavorite", String.valueOf(e));
                        }
                    });
        }
    }

    private void removeFavorite(String songId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = db.collection("users").document(userId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("favorites." + songId, false); // Cập nhật favorites.songId thành false

            userRef.update(updates)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Bài hát đã được xóa khỏi danh sách yêu thích thành công
                            Toast.makeText(requireContext(), "Bài hát đã được xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                            favorite.setImageResource(R.drawable.ic_favorite);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xảy ra lỗi khi xóa bài hát khỏi danh sách yêu thích
                            Toast.makeText(requireContext(), "Xảy ra lỗi", Toast.LENGTH_SHORT).show();
                            Log.d("removeFavorite", String.valueOf(e));
                        }
                    });
        }
    }

    private void findSongIdAndCheckFavoriteList(String songData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (songData != null) {
            db.collection("songs")
                    .whereEqualTo("data", songData)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                                String songId = documentSnapshot.getId();
                                // Sử dụng songId theo nhu cầu của bạn
                                checkFavoriteList(songId);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xảy ra lỗi khi truy vấn collection "songs"
                        }
                    });
        }
    }

    private void checkFavoriteList(String songId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Gson gson = new Gson();
                            String favoritesJson = gson.toJson(document.get("favorites"));
                            Map<String, Boolean> favorites = gson.fromJson(favoritesJson, new TypeToken<Map<String, Boolean>>() {}.getType());
                            if (favorites != null && favorites.containsKey(songId) && favorites.get(songId).equals(true)) {
                                // `songId` tồn tại trong favorites và có giá trị là true
                                favorite.setImageResource(R.drawable.ic_favorite_fill);
                                favorite.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        removeFavorite(songId);
                                        findSongIdAndCheckFavoriteList(songData);
                                    }
                                });
                            } else {
                                // `songId` không tồn tại trong favorites hoặc có giá trị khác true
                                favorite.setImageResource(R.drawable.ic_favorite);
                                favorite.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        addFavorite(songId);
                                        findSongIdAndCheckFavoriteList(songData);
                                    }
                                });
                            }
                        } else {
                            // Tài liệu người dùng không tồn tại
                            // Thực hiện các hành động cần thiết trong trường hợp này
                        }
                    } else {
                        // Xảy ra lỗi khi truy vấn tài liệu người dùng
                        // Thực hiện các hành động cần thiết trong trường hợp này
                    }
                }
            });
        }
    }

    private void addPlaylist(String songId)  {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.update("playlist." + songId, true)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Bài hát đã được thêm vào danh sách yêu thích thành công
                            Toast.makeText(requireContext(), "Bài hát đã được thêm vào danh sách phát", Toast.LENGTH_SHORT).show();
                            playlist.setImageResource(R.drawable.round_playlist_add_check_24);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xảy ra lỗi khi thêm bài hát vào danh sách yêu thích
                            Toast.makeText(requireContext(), "Xảy ra lỗi", Toast.LENGTH_SHORT).show();
                            Log.d("addPlaylist", String.valueOf(e));
                        }
                    });
        }
    }

    private void removePlaylist(String songId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = db.collection("users").document(userId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("playlist." + songId, false);

            userRef.update(updates)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Bài hát đã được xóa khỏi danh sách yêu thích thành công
                            Toast.makeText(requireContext(), "Bài hát đã được xóa khỏi danh sách phát", Toast.LENGTH_SHORT).show();
                            playlist.setImageResource(R.drawable.round_playlist_add_24);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xảy ra lỗi khi xóa bài hát khỏi danh sách yêu thích
                            Toast.makeText(requireContext(), "Xảy ra lỗi", Toast.LENGTH_SHORT).show();
                            Log.d("removePlaylist", String.valueOf(e));
                        }
                    });
        }
    }

    private void findSongIdAndCheckPlaylist(String songData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (songData != null) {
            db.collection("songs")
                    .whereEqualTo("data", songData)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                                String songId = documentSnapshot.getId();
                                // Sử dụng songId theo nhu cầu của bạn
                                checkPlaylist(songId);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xảy ra lỗi khi truy vấn collection "songs"
                        }
                    });
        }
    }

    private void checkPlaylist(String songId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Gson gson = new Gson();
                            String playlistJson = gson.toJson(document.get("playlist"));
                            Map<String, Boolean> playlistMap = gson.fromJson(playlistJson, new TypeToken<Map<String, Boolean>>() {}.getType());
                            if (playlistMap != null && playlistMap.containsKey(songId) && playlistMap.get(songId).equals(true)) {
                                // `songId` tồn tại trong playlistMap và có giá trị là true
                                playlist.setImageResource(R.drawable.round_playlist_add_check_24);
                                playlist.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        removePlaylist(songId);
                                        findSongIdAndCheckPlaylist(songData);
                                    }
                                });
                            } else {
                                // `songId` không tồn tại trong playlistMap hoặc có giá trị khác true
                                playlist.setImageResource(R.drawable.round_playlist_add_24);
                                playlist.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        addPlaylist(songId);
                                        findSongIdAndCheckPlaylist(songData);
                                    }
                                });
                            }
                        } else {
                            // Tài liệu người dùng không tồn tại
                            // Thực hiện các hành động cần thiết trong trường hợp này
                        }
                    } else {
                        // Xảy ra lỗi khi truy vấn tài liệu người dùng
                        // Thực hiện các hành động cần thiết trong trường hợp này
                    }
                }
            });
        }
    }

    private Bitmap getAvt(String songData) {
        Bitmap bitmap = null;
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(songData);
            byte[] artworkBytes = retriever.getEmbeddedPicture();
            if (artworkBytes != null) {
                bitmap = BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes.length);
            }
            retriever.release();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bitmap;
    }

    private void stopSong() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            stop.setImageResource(R.drawable.round_play_arrow_24);

            Intent intent = new Intent(requireContext(), MusicService.class);
            intent.setAction("showNotification");
            requireContext().startService(intent);

            LinearLayout currentSongLayout = MainActivity.getCurrentSong();
            ImageView playCurrentSong = currentSongLayout.findViewById(R.id.playCurrentSong);
            playCurrentSong.setImageResource(R.drawable.round_play_arrow_24_noti);
        }
    }

    private void resumeSong() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            stop.setImageResource(R.drawable.round_pause_24);

            Intent intent = new Intent(requireContext(), MusicService.class);
            intent.setAction("showNotification");
            requireContext().startService(intent);

            LinearLayout currentSongLayout = MainActivity.getCurrentSong();
            ImageView playCurrentSong = currentSongLayout.findViewById(R.id.playCurrentSong);
            playCurrentSong.setImageResource(R.drawable.round_pause_24_noti);
        }
    }

    private void previousSong() {
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction("previous");
        requireContext().startService(intent);
    }

    private void nextSong() {
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction("next");
        requireContext().startService(intent);
    }

    private void setBackground(Bitmap originalBitmap) {
        // Làm mờ ảnh
        Bitmap blurredBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        RenderScript renderScript = RenderScript.create(requireContext());
        Allocation input = Allocation.createFromBitmap(renderScript, originalBitmap);
        Allocation output = Allocation.createFromBitmap(renderScript, blurredBitmap);
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        script.setInput(input);
        script.setRadius(25); // Điều chỉnh độ mờ tại đây
        script.forEach(output);
        output.copyTo(blurredBitmap);

        // Điều chỉnh giá trị màu để làm ảnh tối hơn
        int width = blurredBitmap.getWidth();
        int height = blurredBitmap.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = blurredBitmap.getPixel(x, y);
                int alpha = Color.alpha(pixel);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // Giảm giá trị của red, green, blue để làm ảnh tối hơn
                red = (int) (red * 0.3);
                green = (int) (green * 0.3);
                blue = (int) (blue * 0.3);

                int darkenedPixel = Color.argb(alpha, red, green, blue);
                blurredBitmap.setPixel(x, y, darkenedPixel);
            }
        }

        // Center crop ảnh
        int targetWidth = linearLayout.getWidth();
        int targetHeight = linearLayout.getHeight();
        float scaleX = (float) targetWidth / blurredBitmap.getWidth();
        float scaleY = (float) targetHeight / blurredBitmap.getHeight();
        float scaleFactor = Math.max(scaleX, scaleY);
        int scaledWidth = Math.round(scaleFactor * blurredBitmap.getWidth());
        int scaledHeight = Math.round(scaleFactor * blurredBitmap.getHeight());
        Bitmap croppedBitmap = Bitmap.createScaledBitmap(blurredBitmap, scaledWidth, scaledHeight, true);
        int x = (scaledWidth - targetWidth) / 2;
        int y = (scaledHeight - targetHeight) / 2;
        Bitmap centerCropBitmap = Bitmap.createBitmap(croppedBitmap, x, y, targetWidth, targetHeight);

        Drawable drawable = new BitmapDrawable(getResources(), centerCropBitmap);
        linearLayout.setBackground(drawable);
    }

    private String formatDuration(int duration) {
        // int hours = (duration / (1000 * 60 * 60)) % 24;
        int minutes = (duration / (1000 * 60)) % 60;
        int seconds = (duration / 1000) % 60;

        // return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private int getSongDuration(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);
        String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int duration = Integer.parseInt(durationString);
        try {
            retriever.release();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return duration;
    }

    private void updateCurrentTime(TextView current, SeekBar seekBar) {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    String currentTime = formatTime(currentPosition);
                    current.setText(currentTime);
                    seekBar.setProgress(currentPosition);
                }
                handler.postDelayed(this, 100); // Cập nhật thời gian mỗi 100 mili giây
            }
        });
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        String time = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        return time;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(pauseButtonClickedReceiver);
    }

}