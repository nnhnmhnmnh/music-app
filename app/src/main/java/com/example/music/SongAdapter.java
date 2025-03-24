package com.example.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList;
    private Context context;
    private int selectedPosition = RecyclerView.NO_POSITION;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            selectedPosition = intent.getIntExtra("currentSongIndex", RecyclerView.NO_POSITION);
            notifyDataSetChanged();
        }
    };

    public SongAdapter(List<Song> songList, Context context) {
        this.songList = songList;
        this.context = context;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);

        // Đăng ký BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("notifychanged");
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

        return new SongViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.textViewTitle.setText(song.getDisplayName());
        holder.textViewArtist.setText(song.getArtist());
        holder.textViewAlbum.setText(song.getAlbum());

        int currentSongIndex = position;

        // Kiểm tra vị trí itemView được bấm
        if (currentSongIndex == selectedPosition) {
            // Nếu itemView được bấm, đặt màu chữ mới
            holder.textViewTitle.setTextColor(Color.RED);
            holder.textViewArtist.setTextColor(Color.RED);
            holder.textViewAlbum.setTextColor(Color.RED);
        } else {
            // Nếu itemView không được bấm, đặt lại màu chữ mặc định
            holder.textViewTitle.setTextColor(Color.BLACK);
            holder.textViewArtist.setTextColor(Color.BLACK);
            holder.textViewAlbum.setTextColor(Color.BLACK);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout currentSongLayout = null;
                try {
                    currentSongLayout = ((MainActivity) context).getCurrentSong();
                } catch (Exception e) {}
                try {
                    currentSongLayout = MainActivity.getCurrentSong();
                } catch (Exception e) {}
                if (currentSongLayout != null) {
                    currentSongLayout.setVisibility(View.VISIBLE);
                    TextView songName = currentSongLayout.findViewById(R.id.currentSongName);
                    ImageView playCurrentSong = currentSongLayout.findViewById(R.id.playCurrentSong);
                    ImageView currentAvt = currentSongLayout.findViewById(R.id.currentAvt);
                    updateAvt(song.getData(), song.getAvt(), currentAvt);
                    playCurrentSong.setImageResource(R.drawable.round_pause_24_noti);
                    final boolean[] isPlaying = {true};
                    songName.setText(song.getDisplayName());

                    currentSongLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(context, PlaySong.class);
                            intent.putExtra("songData", song.getData());
                            intent.putExtra("songName", song.getDisplayName());
                            intent.putExtra("songArtist", song.getArtist());
                            intent.putExtra("songLrc", song.getLrc());
                            intent.putExtra("songAvt", song.getAvt());

                            playCurrentSong.setImageResource(R.drawable.round_pause_24_noti);
                            isPlaying[0] = true;

                            // Gửi thông điệp tới MusicService để hiển thị thông báo và điều khiển phát nhạc
//                            Intent serviceIntent = new Intent(context, MusicService.class);
//                            serviceIntent.setAction("setCurrentSongIndex");
//                            serviceIntent.putExtra("currentSongIndex", currentSongIndex);
//                            context.startService(serviceIntent);
                            context.startActivity(intent);
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
                                } else {
                                    playCurrentSong.setImageResource(R.drawable.round_play_arrow_24_noti);
                                    isPlaying[0] = false;
                                    saveCurrentPlaybackTime();
                                }
                            }

                            // Gửi thông điệp tới MusicService để hiển thị thông báo và điều khiển phát nhạc
                            Intent serviceIntent = new Intent(context, MusicService.class);
                            serviceIntent.setAction("play");
                            context.startService(serviceIntent);
                        }
                    });
                }

                saveCurrentSongIndex(currentSongIndex);

                selectedPosition = currentSongIndex;
                notifyDataSetChanged();

                Intent intent = new Intent(context, PlaySong.class);
                intent.putExtra("songData", song.getData());
                intent.putExtra("songName", song.getDisplayName());
                intent.putExtra("songArtist", song.getArtist());
                intent.putExtra("songLrc", song.getLrc());
                intent.putExtra("songAvt", song.getAvt());

                // Gửi thông điệp tới MusicService để hiển thị thông báo và điều khiển phát nhạc
                Intent serviceIntent = new Intent(context, MusicService.class);
                serviceIntent.setAction("setCurrentSongIndex");
                serviceIntent.putExtra("currentSongIndex", currentSongIndex);
                context.startService(serviceIntent);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle, textViewArtist, textViewAlbum;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewArtist = itemView.findViewById(R.id.textViewArtist);
            textViewAlbum = itemView.findViewById(R.id.textViewAlbum);
        }
    }

    private MediaPlayer mediaPlayer;
    private MediaPlayerManager mediaPlayerManager;
    private void saveCurrentPlaybackTime() {
        mediaPlayerManager = MediaPlayerManager.getInstance();
        mediaPlayer = mediaPlayerManager.getMediaPlayer();
        SharedPreferences sharedPreferences = context.getSharedPreferences("PlaybackPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentPosition", mediaPlayer.getCurrentPosition());
        editor.apply();
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
        SharedPreferences sharedPreferences = context.getSharedPreferences("CurrentSongIndexPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentSongIndex", currentSongIndex);
        editor.apply();
    }
}
