package com.example.music;

import android.media.MediaPlayer;

import java.io.IOException;

public class MediaPlayerManager {
    private static MediaPlayerManager instance;
    private MediaPlayer mediaPlayer;

    private MediaPlayerManager() {
        mediaPlayer = new MediaPlayer();
    }

    public static synchronized MediaPlayerManager getInstance() {
        if (instance == null) {
            instance = new MediaPlayerManager();
        }
        return instance;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}