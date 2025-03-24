package com.example.music;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    private String data;
    private String artist;
    private String album;
    private String displayName;
    private String lrc;
    private String avt;

    public Song() {}

    public Song(String data, String artist, String album, String displayName, String lrc, String avt) {
        this.data = data;
        this.artist = artist;
        this.album = album;
        this.displayName = displayName;
        this.lrc = lrc;
        this.avt = avt;
    }

    protected Song(Parcel in) {
        data = in.readString();
        artist = in.readString();
        album = in.readString();
        displayName = in.readString();
        lrc = in.readString();
        avt = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public String getData() {
        return data;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLrc() {
        return lrc;
    }
    public String getAvt() {
        return avt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(displayName);
        dest.writeString(lrc);
        dest.writeString(avt);
    }
}
