package com.example.music;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer;
    private List<Song> songList;
    private MediaPlayerManager mediaPlayerManager;
    private int currentSongIndex;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Khởi tạo MediaPlayer
        mediaPlayerManager = MediaPlayerManager.getInstance();
        mediaPlayer = mediaPlayerManager.getMediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Xử lý khi bài hát kết thúc
                // ...
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }
        if (action != null) {
            switch (action) {
                case "previous":
                    playPrevious();
                    break;
                case "play":
                    if (mediaPlayer.isPlaying()) {
                        pause();
                    } else {
                        resume();
                    }
                    break;
                case "next":
                    playNext();
                    break;
                case "setSongList":
                    List<Song> songs = (List<Song>) intent.getSerializableExtra("songList");
                    setSongList(songs);
                    //Log.d("setSongList", String.valueOf(songs.size()));
                    break;
                case "setCurrentSongIndex":
                    currentSongIndex = intent.getIntExtra("currentSongIndex", -1);
                    // Hiển thị thông báo nhạc
                    Song song = songList.get(currentSongIndex);
                    showNotification(song);
                    break;
                case "showNotification":
                    showNotification(songList.get(currentSongIndex));
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void resume() {
        if (currentSongIndex < 0 || currentSongIndex >= songList.size()) {
            // Kiểm tra xem currentSongIndex có hợp lệ hay không
            return;
        }

        Song song = songList.get(currentSongIndex);
        String songData = song.getData();
        // Phát bài hát từ songData
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
        showNotification(song);

        Intent broadcastIntent = new Intent("resumebuttonclicked");
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        LinearLayout currentSongLayout = MainActivity.getCurrentSong();
        ImageView playCurrentSong = currentSongLayout.findViewById(R.id.playCurrentSong);
        playCurrentSong.setImageResource(R.drawable.round_pause_24_noti);

        // Cập nhật giao diện người dùng hoặc làm các thao tác khác khi bắt đầu phát nhạc
        // ...
    }

    private void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Song song = songList.get(currentSongIndex);
            showNotification(song);

            Intent broadcastIntent = new Intent("pausebuttonclicked");
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

            LinearLayout currentSongLayout = MainActivity.getCurrentSong();
            ImageView playCurrentSong = currentSongLayout.findViewById(R.id.playCurrentSong);
            playCurrentSong.setImageResource(R.drawable.round_play_arrow_24_noti);

            // Hiển thị thông báo nhạc với nút Resume
//            updateNotification(false);

            // Cập nhật giao diện người dùng hoặc làm các thao tác khác khi dừng phát nhạc
            // ...
        }
    }

    private void playPrevious() {
        if (currentSongIndex-1 >= 0 && currentSongIndex-1 < songList.size()) {
            currentSongIndex = currentSongIndex - 1;
        } else {
            currentSongIndex = songList.size()-1;
        }
        Song song = songList.get(currentSongIndex);
        String songData = song.getData();
        String songLrc = song.getLrc();
        String songAvt = song.getAvt();
        String songArtist = song.getArtist();
        String songTitle = song.getDisplayName();
        Intent broadcastIntent = new Intent("previous-nextbuttonclicked");
        broadcastIntent.putExtra("songData", songData);
        broadcastIntent.putExtra("songLrc", songLrc);
        broadcastIntent.putExtra("songAvt", songAvt);
        broadcastIntent.putExtra("songArtist", songArtist);
        broadcastIntent.putExtra("songTitle", songTitle);
        LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(broadcastIntent);

        setNewCurrentSong(song);
        saveCurrentSongIndex(currentSongIndex);

        Intent intent = new Intent("notifychanged");
        intent.putExtra("currentSongIndex", currentSongIndex);
        LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intent);
    }

    private void playNext() {
        if (currentSongIndex+1 >= 0 && currentSongIndex+1 < songList.size()) {
            currentSongIndex = currentSongIndex + 1;
        } else {
            currentSongIndex = 0;
        }
        Song song = songList.get(currentSongIndex);
        String songData = song.getData();
        String songLrc = song.getLrc();
        String songAvt = song.getAvt();
        String songArtist = song.getArtist();
        String songTitle = song.getDisplayName();
        Intent broadcastIntent = new Intent("previous-nextbuttonclicked");
        broadcastIntent.putExtra("songData", songData);
        broadcastIntent.putExtra("songLrc", songLrc);
        broadcastIntent.putExtra("songAvt", songAvt);
        broadcastIntent.putExtra("songArtist", songArtist);
        broadcastIntent.putExtra("songTitle", songTitle);
        LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(broadcastIntent);

        setNewCurrentSong(song);
        saveCurrentSongIndex(currentSongIndex);
        //Log.d("playNext", songTitle);

        Intent intent = new Intent("notifychanged");
        intent.putExtra("currentSongIndex", currentSongIndex);
        LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intent);
    }

    @SuppressLint("MissingPermission")
    private void showNotification(Song song) {
        // Tạo trình xây dựng thông báo trực quan
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "music_channel")
                .setSmallIcon(R.drawable.round_music_note_24)
                .setContentTitle(song.getDisplayName())
                .setContentText(song.getArtist())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true);

        // Tạo RemoteViews cho trình xây dựng thông báo trực quan
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_notification_layout);
        contentView.setTextViewText(R.id.notification_title, song.getDisplayName());
        contentView.setTextViewText(R.id.notification_artist, song.getArtist());
        boolean isPlaying = checkIfMusicIsPlaying();
        int playButtonIcon = isPlaying ? R.drawable.round_pause_24_noti : R.drawable.round_play_arrow_24_noti;
        contentView.setImageViewResource(R.id.action_pause, playButtonIcon);

        // Thiết lập RemoteViews vào trình xây dựng thông báo trực quan
        builder.setCustomContentView(contentView);

        // Tạo trình xây dựng thông báo trực quan mở rộng
        NotificationCompat.Style style = new NotificationCompat.DecoratedCustomViewStyle();
        builder.setStyle(style);

        // Tạo Intent cho các hành động
        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction("previous");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, MusicService.class);
        playIntent.setAction("play");
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction("next");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        // Tạo Intent cho hành động khi nhấn vào thông báo
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        // Gắn các PendingIntent vào layout của RemoteViews
        contentView.setOnClickPendingIntent(R.id.action_previous, previousPendingIntent);
        contentView.setOnClickPendingIntent(R.id.action_pause, playPendingIntent);
        contentView.setOnClickPendingIntent(R.id.action_next, nextPendingIntent);

        // Hiển thị thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

    private boolean checkIfMusicIsPlaying() {
        if (mediaPlayer.isPlaying()) {
            return true;
        }
        return false;
    }

    public void setSongList(List<Song> songs) {
        songList = songs;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Music Channel";
            String description = "Channel for Music Playback";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("music_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setNewCurrentSong(Song song) {
        LinearLayout currentSongLayout = MainActivity.getCurrentSong();
        if (currentSongLayout != null) {
            TextView songName = currentSongLayout.findViewById(R.id.currentSongName);
            ImageView playCurrentSong = currentSongLayout.findViewById(R.id.playCurrentSong);
            ImageView currentAvt = currentSongLayout.findViewById(R.id.currentAvt);
            updateAvt(song.getData(), song.getAvt(), currentAvt);
            playCurrentSong.setImageResource(R.drawable.round_pause_24_noti);
            songName.setText(song.getDisplayName());
            currentSongLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MusicService.this, PlaySong.class);
                    intent.putExtra("songData", song.getData());
                    intent.putExtra("songName", song.getDisplayName());
                    intent.putExtra("songArtist", song.getArtist());
                    intent.putExtra("songLrc", song.getLrc());
                    intent.putExtra("songAvt", song.getAvt());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    playCurrentSong.setImageResource(R.drawable.round_pause_24_noti);
                }
            });
        }
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

    private void saveCurrentSongIndex(int currentSongIndex) {
        SharedPreferences sharedPreferences = getSharedPreferences("CurrentSongIndexPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentSongIndex", currentSongIndex);
        editor.apply();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
