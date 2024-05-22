package com.example.tv.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.tv.R;

import xyz.doikki.videocontroller.StandardVideoController;
import xyz.doikki.videoplayer.player.VideoView;


public class PlayerActivity extends Activity {
    VideoView videoView;
    private StandardVideoController controller;
    float[] speed = {1.0f,1.25f,1.5f,1.75f,2.0f};
    int speedPos = 0;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        System.out.println("new PlayerActivity created");
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("url");
        String name = getIntent().getStringExtra("name");
        setContentView(R.layout.activity_player);
        videoView = findViewById(R.id.player);
        videoView.setUrl(url); //设置视频地址
        controller = new StandardVideoController(this);
        controller.addDefaultControlComponent(name, false);
        videoView.setVideoController(controller); //设置控制器
        videoView.start(); //开始播放，不调用则不自动播放

    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        int keyCode = event.getKeyCode();

        if(event.getAction()==KeyEvent.ACTION_DOWN) {
            switch (keyCode) {

                case KeyEvent.KEYCODE_DPAD_CENTER:
//                    Toast.makeText(this, "state is " + videoView.isPlaying(), Toast.LENGTH_SHORT).show();
                    if (videoView.isPlaying()) {
                        videoView.pause();
                        controller.show();
                        controller.stopFadeOut();
                    } else {
                        videoView.start();
                        controller.hide();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    videoView.seekTo(videoView.getCurrentPosition() + 10000);
                    controller.show();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    videoView.seekTo(videoView.getCurrentPosition() - 10000);
                    controller.show();
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    speedPos++;
                    videoView.setSpeed(speed[speedPos%speed.length]);
                    Toast.makeText(PlayerActivity.this,"倍数："+speed[speedPos%speed.length],Toast.LENGTH_SHORT).show();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    protected void onPause() {
        super.onPause();
        videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.release();
    }


    @Override
    public void onBackPressed() {
        if (!videoView.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
