package com.hanny.cameraview;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

/**
 * Created by Administrator on 2017/6/6.
 */

public class ImagePreview extends AppCompatActivity {
    public final static String DATA = "URL";
    private String uri;
    private ImageView iv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        iv = (ImageView) findViewById(R.id.iv);
        uri = getIntent().getStringExtra(DATA);
        if (!TextUtils.isEmpty(uri)) {
            Glide.with(this).load(uri)
                    .error(R.mipmap.ic_launcher)//load失敗的Drawable
                    .into(iv);
        }
    }
}
