package com.hanny.cameraview;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;


public class VideoPlayActivity extends AppCompatActivity {

    public final static String DATA = "URL";
    public static final int RESULT_VEDIO_AGAIN = 0x108;


    private long playPostion = -1;
    private long duration = -1;
    String uri;
    private PlayView playView;
    //    private Button playBtn;
    private RelativeLayout activityPlay;
    private Button back;
    private Button send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recvideo_play);

        playView = (PlayView) findViewById(R.id.playView);
//        playBtn = (Button) findViewById(R.id.playBtn);
        back = (Button) findViewById(R.id.back);
        send = (Button) findViewById(R.id.send);
        activityPlay = (RelativeLayout) findViewById(R.id.activity_play);
//        playBtn.setOnClickListener(this);
        uri = getIntent().getStringExtra(DATA);

        playView.setVideoURI(Uri.parse(uri));

        playView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playView.seekTo(1);
                startVideo();
            }
        });

        playView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //获取视频资源的宽度
                int videoWidth = mp.getVideoWidth();
                //获取视频资源的高度
                int videoHeight = mp.getVideoHeight();
                int duration = mp.getDuration();
                System.out.println(duration + "");
                playView.setSizeH(videoHeight);
                playView.setSizeW(videoWidth);
                playView.requestLayout();
                VideoPlayActivity.this.duration = mp.getDuration();
                play();
            }
        });

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();//如果为true，则表示屏幕“亮”了，否则屏幕“暗”了。
        if (!isScreenOn) {
            pauseVideo();
        }
        initClick();
    }

    private void initClick() {
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtils.deleteFile(uri);
                setResult(RESULT_VEDIO_AGAIN);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //发送视频
                setResult(RESULT_OK);
                finish();
            }
        });
    }

//    public void onClick() {
//        play();
//    }


    @Override
    protected void onResume() {
        super.onResume();
        if (playPostion > 0) {
            pauseVideo();
        }
        playView.seekTo((int) ((playPostion > 0 && playPostion < duration) ? playPostion : 1));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playView.stopPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playView.pause();
        playPostion = playView.getCurrentPosition();
        pauseVideo();

    }

    @Override
    public void onBackPressed() {
        FileUtils.deleteFile(uri);
        setResult(RESULT_CANCELED);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }


    private void pauseVideo() {
        playView.pause();
//        playBtn.setText("播放");
    }

    private void startVideo() {
        playView.start();
//        playBtn.setText("停止");
    }

    /**
     * 播放
     */
    private void play() {
        if (playView.isPlaying()) {
            pauseVideo();
        } else {
            if (playView.getCurrentPosition() == playView.getDuration()) {
                playView.seekTo(0);
            }
            startVideo();
        }
    }


//    @Override
//    public void onClick(View view) {
////        play();
//    }
}
