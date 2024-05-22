package com.example.tv;

import android.app.Application;

import xyz.doikki.videoplayer.exo.ExoMediaPlayerFactory;
import xyz.doikki.videoplayer.player.VideoViewConfig;
import xyz.doikki.videoplayer.player.VideoViewManager;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VideoViewManager.setConfig(VideoViewConfig.newBuilder()
                //使用使用IjkPlayer解码
//                .setPlayerFactory(IjkPlayerFactory.create())
//                //使用ExoPlayer解码
                .setPlayerFactory(ExoMediaPlayerFactory.create())
//                //使用MediaPlayer解码
//                .setPlayerFactory(AndroidMediaPlayerFactory.create())
                .build());
    }
}
