package com.example.music;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaySong extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter adapter;
    private String songData, avtUrl;
    private BroadcastReceiver pauseButtonClickedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case "previous-nextbuttonclicked":
                    songData = intent.getStringExtra("songData");
                    avtUrl = intent.getStringExtra("songAvt");
                    checkAndDisplayBackground(songData, avtUrl);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_song);

        Intent broadcastIntent = new Intent("playsongactivity_created");
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        viewPager = findViewById(R.id.viewPagerPS);
//        tabLayout = findViewById(R.id.tabLayoutPS);

        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new PlaySong1());
        fragmentList.add(new PlaySong2());

        adapter = new ViewPagerAdapter(this, fragmentList);
        viewPager.setAdapter(adapter);

//        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
//            // Đặt tiêu đề cho từng tab tại đây
//            if (position == 0) {
//                tab.setText("Tab 1");
//            } else if (position == 1) {
//                tab.setText("Tab 2");
//            }
//        }).attach();
        //Log.d("playsong", String.valueOf(getAvt(getIntent().getStringExtra("songData"))));
        // Đăng ký BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("previous-nextbuttonclicked");
        LocalBroadcastManager.getInstance(this).registerReceiver(pauseButtonClickedReceiver, filter);

        songData = getIntent().getStringExtra("songData");
        avtUrl = getIntent().getStringExtra("songAvt");
        checkAndDisplayBackground(songData, avtUrl);
    }

    public void passDataToPlaySong2(String songData, String lrcUrl) {
        if (adapter != null && adapter.getItemCount() > 1) {
            PlaySong2 playSong2 = (PlaySong2) adapter.createFragment(1);
            if (playSong2 != null) {
                playSong2.receiveData(songData, lrcUrl);
            }
        }
    }

    private void checkAndDisplayBackground(String songData, String avtUrl) {
        if (songData.contains("firebase")){
            if (avtUrl != null) {
                Picasso.get().load(avtUrl).into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        // Sử dụng bitmap ở đây
                        setBackground(bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        // Xử lý khi tải ảnh thất bại
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        // Xử lý trước khi tải ảnh
                    }
                });
            } else {
                 viewPager.setBackgroundColor(Color.parseColor("#222222"));
            }
        } else {
            Picasso.get().load(new File(songData)).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    // Đây là phương thức được gọi khi ảnh đã được tải thành công và chuyển đổi thành Bitmap
                    // Bạn có thể sử dụng bitmap ở đây
                    setBackground(bitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    // Đây là phương thức được gọi khi việc tải ảnh thất bại
                    // Xử lý lỗi ở đây nếu cần thiết
                    viewPager.setBackgroundColor(Color.parseColor("#222222"));
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    // Đây là phương thức được gọi trước khi bắt đầu tải ảnh
                    // Bạn có thể hiển thị một placeholder ở đây nếu cần thiết
                }
            });
            if (getAvt(songData) != null) {
                setBackground(getAvt(songData));
            } else {
                viewPager.setBackgroundColor(Color.parseColor("#222222"));
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

    private void setBackground(Bitmap originalBitmap) {
        // Làm mờ ảnh
        Bitmap blurredBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        RenderScript renderScript = RenderScript.create(this);
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

        ViewTreeObserver viewTreeObserver = viewPager.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Center crop ảnh
                int targetWidth = viewPager.getWidth();
                int targetHeight = viewPager.getHeight();
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
                viewPager.setBackground(drawable);
                viewPager.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pauseButtonClickedReceiver);
        Intent broadcastIntent = new Intent("playsongactivity_destroyed");
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

}