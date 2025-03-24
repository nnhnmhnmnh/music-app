package com.example.music;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private static LinearLayout currentSong;
    private MediaPlayer mediaPlayer;
    private MediaPlayerManager mediaPlayerManager;
    private TextView currentSongName;
    private ImageView currentAvt, playCurrentSong;
    private boolean isPlaysongActivityDestroyed = false;
    private boolean isRepeatOne = false;
    private BroadcastReceiver playsongActivityDestroyedReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case "playsongactivity_destroyed":
                    isPlaysongActivityDestroyed = true;
                    break;
                case "playsongactivity_created":
                    isPlaysongActivityDestroyed = false;
                    break;
            }
        }
    };
    private BroadcastReceiver playNextPreviousSongReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case "previous-nextbuttonclicked":
                    if (isPlaysongActivityDestroyed) {
                        //Log.d("playNextMain", "previous-nextbuttonclicked");
                        String songData = intent.getStringExtra("songData");
                        playNextPreviousSong(songData);
                    }
                    break;
            }
        }
    };
    private BroadcastReceiver repeatSongReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case "repeat-all":
                    //Log.d("repeat-all", "onReceive: ");
                    repeatAll();
                    break;
                case "repeat-one":
                    //Log.d("repeat-one", "onReceive: ");
                    repeatSong();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        currentSong = findViewById(R.id.currentSong);
        currentAvt = findViewById(R.id.currentAvt);
        playCurrentSong = findViewById(R.id.playCurrentSong);
        currentSongName = findViewById(R.id.currentSongName);
        currentSongName.setSelected(true);
        mediaPlayerManager = MediaPlayerManager.getInstance();
        mediaPlayer = mediaPlayerManager.getMediaPlayer();

        getSongFromSharedPreferences();

        IntentFilter filter = new IntentFilter();
        //filter.addAction("previous-nextbuttonclicked");
        filter.addAction("playsongactivity_destroyed");
        filter.addAction("playsongactivity_created");
        LocalBroadcastManager.getInstance(this).registerReceiver(playsongActivityDestroyedReceiver , filter);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("previous-nextbuttonclicked");
        LocalBroadcastManager.getInstance(this).registerReceiver(playNextPreviousSongReceiver , intentFilter);

        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("repeat-all");
        intentFilter2.addAction("repeat-one");
        LocalBroadcastManager.getInstance(this).registerReceiver(repeatSongReceiver , intentFilter2);

        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new Fragment1());
        fragmentList.add(new Fragment2());
        fragmentList.add(new AccountFragment());

        ViewPagerAdapter adapter = new ViewPagerAdapter(this, fragmentList);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // Đặt tiêu đề cho từng tab
            if (position == 0) {
                tab.setText("Ngoại tuyến");
                tab.setIcon(R.drawable.round_folder_open_24);
            } else if (position == 1) {
                tab.setText("Trực tuyến");
                tab.setIcon(R.drawable.round_cloud_queue_24);
            } else if (position == 2) {
                tab.setText("Tài khoản");
                tab.setIcon(R.drawable.round_person_outline_24);
            }
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Xử lý khi tab được chọn
                int selectedTabPosition = tab.getPosition();
                switch (selectedTabPosition) {
                    case 0:
                        setLocalSongs();
                        break;
                    case 1:
                        if (adapter != null && adapter.getItemCount() > 1) {
                            Fragment2 fragment2 = (Fragment2) adapter.createFragment(1);
                            if (fragment2 != null && fragment2.isAdded()) {
                                switch (fragment2.getCurrentFragment()) {
                                    case 0:
                                        fragment2.setDefaultSongs();
                                        break;
                                    case 1:
                                        fragment2.setFavoriteSongs();
                                        break;
                                    case 3:
                                        fragment2.setPlaylistSongs();
                                        break;
                                }
                            }
                        }
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Xử lý khi tab không còn được chọn
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Xử lý khi tab đã được chọn lại
            }
        });
    }

    public static LinearLayout getCurrentSong() {
        return currentSong;
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveCurrentPlaybackTime();
    }

    // Hoặc sử dụng onDestroy() thay cho onStop() nếu bạn muốn lưu khi ứng dụng bị hủy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveCurrentPlaybackTime();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playsongActivityDestroyedReceiver );
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playNextPreviousSongReceiver );
        LocalBroadcastManager.getInstance(this).unregisterReceiver(repeatSongReceiver );
    }

    private void saveCurrentPlaybackTime() {
        SharedPreferences sharedPreferences = getSharedPreferences("PlaybackPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentPosition", mediaPlayer.getCurrentPosition());
        editor.apply();
    }

    private void setLocalSongs() {
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DISPLAY_NAME}, "1=1", null, null);

        List<Song> songList = new ArrayList<>();
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
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("setSongList");
        intent.putExtra("songList", (Serializable) songList);
        startService(intent);
    }

//    public void setCurrentSong(LinearLayout layout) {
//        currentSong = layout;
//    }

    private void playNextPreviousSong(String songData) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }

        saveSongData(songData);
        try {
            mediaPlayer.setDataSource(songData);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.setAction("showNotification");
                    startService(intent);
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSongData(String songData) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("songData", songData);
        editor.apply();
    }

    public boolean getRepeatOne() {
        return isRepeatOne;
    }

    public void setRepeatOne(boolean repeatOne) {
        isRepeatOne = repeatOne;
    }

    public void repeatSong() {
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mp.getCurrentPosition() >= mp.getDuration()) {
                    mp.start();
                }
            }
        });
    }

    public void repeatAll() {
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Bài hát đã kết thúc, phát bài hát tiếp theo
                if (mp.getCurrentPosition() >= mp.getDuration()) {
                    // Bài hát đã kết thúc và MediaPlayer đang trong trạng thái phát
                    // Gọi nextSong() để phát bài hát tiếp theo
                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.setAction("next");
                    startService(intent);
                }
            }
        });
    }

    private String loadSongData() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("songData", null);
    }

    private Map<String, String> loadSong() {
        SharedPreferences sharedPreferences = getSharedPreferences("SongPrefs", Context.MODE_PRIVATE);
        return (Map<String, String>) sharedPreferences.getAll();
    }

    private void updateAvt(String songData, String avtUrl, ImageView currentAvt) {
        if (songData.contains("firebase")) {
            if (avtUrl != null) {
                Picasso.get().load(avtUrl).into(currentAvt);
            } else {
                currentAvt.setImageResource(R.drawable.round_music_note_24);
            }
        } else {
            if (getAvt(songData) != null) {
                currentAvt.setImageBitmap(getAvt(songData));
            } else {
                currentAvt.setImageResource(R.drawable.round_music_note_24);
            }
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

    private void getSongFromSharedPreferences() {
        if (loadSongData() != null) {
            String songData = loadSongData();
            Map<String, String> Song = loadSong();
            String songAvt = Song.get("songAvt");
            String songName = Song.get("songName");
            String songArtist = Song.get("songArtist");
            String songLrc = Song.get("songLrc");
            currentSongName.setText(songName);
            updateAvt(songData, songAvt, currentAvt);
            final boolean[] isPlaying = {false};

            currentSong.setVisibility(View.VISIBLE);
            currentSong.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, PlaySong.class);
                    intent.putExtra("songData", songData);
                    intent.putExtra("songName", songName);
                    intent.putExtra("songArtist", songArtist);
                    intent.putExtra("songLrc", songLrc);
                    intent.putExtra("songAvt", songAvt);
                    playCurrentSong.setImageResource(R.drawable.round_pause_24_noti);
                    isPlaying[0] = true;
                    Intent serviceIntent = new Intent(MainActivity.this, MusicService.class);
                    serviceIntent.setAction("setCurrentSongIndex");
                    serviceIntent.putExtra("currentSongIndex", getCurrentSongIndex());
                    startService(serviceIntent);
                    startActivity(intent);
                }
            });
            playCurrentSong.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Drawable currentDrawable = playCurrentSong.getDrawable();
                    if (currentDrawable != null && currentDrawable.getConstantState() != null) {
                        if (!isPlaying[0]) {
                            playCurrentSong.setImageResource(R.drawable.round_pause_24_noti);
                            isPlaying[0] = true;
                            playSong(songData);
                            Intent intent = new Intent(MainActivity.this, MusicService.class);
                            intent.setAction("showNotification");
                            startService(intent);
                        } else {
                            playCurrentSong.setImageResource(R.drawable.round_play_arrow_24_noti);
                            isPlaying[0] = false;
                            mediaPlayer.pause();
                            saveCurrentPlaybackTime();
                        }
                    }

                    // Gửi thông điệp tới MusicService để hiển thị thông báo và điều khiển phát nhạc
                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.setAction("showNotification");
                    startService(intent);
                }
            });
        }
    }

    private int getCurrentPlaybackTime() {
        SharedPreferences sharedPreferences = getSharedPreferences("PlaybackPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("currentPosition", 0);
    }

    private int getCurrentSongIndex() {
        SharedPreferences sharedPreferences = getSharedPreferences("CurrentSongIndexPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("currentSongIndex", 0);
    }

    private void playSong(String songData) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(songData);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.seekTo(getCurrentPlaybackTime());
                    mediaPlayer.start();

                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.setAction("showNotification");
                    startService(intent);
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}