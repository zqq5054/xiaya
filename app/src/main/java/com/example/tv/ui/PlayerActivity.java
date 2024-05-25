package com.example.tv.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.tv.MainActivity;
import com.example.tv.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import xyz.doikki.videocontroller.StandardVideoController;
import xyz.doikki.videoplayer.player.BaseVideoView;
import xyz.doikki.videoplayer.player.VideoView;


public class PlayerActivity extends Activity implements Runnable {
    VideoView videoView;
    private StandardVideoController controller;
    float[] speed = {1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
    int speedPos = 0;
    private Map<String, String> videoTransCoding = new LinkedHashMap();
    private JSONObject jsonObject;
    private JSONArray array;
    private String path;
    private int currentPos = 0;
    private String url;
    public static String list;
    private int type;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        System.out.println("new PlayerActivity created");
        super.onCreate(savedInstanceState);
        url = getIntent().getStringExtra("url");
        path = getIntent().getStringExtra("path");
        type = getIntent().getIntExtra("type", 2);
        final String name = getIntent().getStringExtra("name");
        if (!TextUtils.isEmpty(list)) {
            try {
                jsonObject = new JSONObject(list);
                JSONArray content = jsonObject.getJSONObject("data").getJSONArray("content");
                array = new JSONArray();
                int pos = 0;
                for (int i = 0; i < content.length(); i++) {
                    JSONObject item = content.getJSONObject(i);
                    int type = item.getInt("type");
                    String itemName = item.getString("name");
                    System.out.println(itemName);
                    if (type == 2 || type == 3) {
                        array.put(item);
                        if (itemName.equals(name)) {
                            currentPos = pos;
                        }
                        pos++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setContentView(R.layout.activity_player);
        videoView = findViewById(R.id.player);
        videoView.setUrl(url); //设置视频地址
        controller = new StandardVideoController(this);
        controller.addDefaultControlComponent(name, false);
        videoView.setVideoController(controller); //设置控制器
        videoView.start(); //开始播放，不调用则不自动播放
        if (type == 2) {
            new Thread(this).start();
        }
        videoView.addOnStateChangeListener(new BaseVideoView.OnStateChangeListener() {
            @Override
            public void onPlayerStateChanged(int playerState) {

            }

            @Override
            public void onPlayStateChanged(int playState) {

                if (playState == BaseVideoView.STATE_PLAYBACK_COMPLETED) {
                    if (array != null && array.length() != 0) {
                        next();
                    }
                } else if (playState == BaseVideoView.STATE_ERROR) {
                    next();
                }
            }
        });
    }

    private void next() {
        currentPos++;
        if (currentPos < array.length()) {
            try {
                JSONObject item = array.getJSONObject(currentPos);
                videoView.release();
                Toast.makeText(PlayerActivity.this, "播放" + item.getString("name"), Toast.LENGTH_LONG).show();
                path = path.substring(0, path.lastIndexOf("/")) + "/" + item.getString("name");
                new Thread(new NextVideo()).start();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        int keyCode = event.getKeyCode();

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
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
                    videoView.setSpeed(speed[speedPos % speed.length]);
                    Toast.makeText(PlayerActivity.this, "倍速：" + speed[speedPos % speed.length], Toast.LENGTH_SHORT).show();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    break;
                case KeyEvent.KEYCODE_MENU:
                    if (!videoTransCoding.isEmpty()) {
                        showResolutionDialog();
                    }

                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void showResolutionDialog() {

        final String[] resolutionOptions = new String[videoTransCoding.size()];
        int i = 0;
        for (String key : videoTransCoding.keySet()) {
            resolutionOptions[i] = key;
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle("选中分辨率");
        builder.setItems(resolutionOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用户选择的内容
                String selectedResolution = resolutionOptions[which];
                String playUrl = videoTransCoding.get(selectedResolution);
                if (!playUrl.equals(PlayerActivity.this.url)) {
                    videoView.release();
                    videoView.setUrl(playUrl);
                    videoView.start();
                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    protected void onPause() {
        super.onPause();
        if (type == 2) {
            videoView.pause();
        }
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
        list = null;
    }


    @Override
    public void onBackPressed() {
        if (!videoView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    private void getVideo() {
        OkHttpClient client = new OkHttpClient();

        JSONObject requestBody = new JSONObject();

        try {
            requestBody.put("path", path);
            requestBody.put("password", "");
            requestBody.put("method", "video_preview");
            requestBody.put("refresh", false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SharedPreferences data = getSharedPreferences("data", 0);
        String domain = data.getString("domain", "");
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, requestBody.toString());
        Request request = new Request.Builder()
                .url(domain + "api/fs/other")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {


            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {

                        String responseBody = response.body().string();
                        System.out.println(responseBody);
                        JSONObject jsonObject = new JSONObject(responseBody);
                        int code = jsonObject.getInt("code");
                        if (code == 200) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            JSONObject info = data.getJSONObject("video_preview_play_info");
                            JSONArray array = info.getJSONArray("live_transcoding_task_list");
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject itemObject = array.getJSONObject(i);
                                String status = itemObject.getString("status");
                                if ("finished".equals(status)) {
                                    String template_id = itemObject.getString("template_id");
                                    String url = itemObject.getString("url");
                                    videoTransCoding.put(template_id, url);
                                }
                            }


                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {

                }

            }
        });
    }

    @Override
    public void run() {

        getVideo();
    }

    class NextVideo implements Runnable {

        @Override
        public void run() {
            OkHttpClient client = new OkHttpClient();

            JSONObject requestBody = new JSONObject();

            try {
                requestBody.put("path", path);
                requestBody.put("password", "");
                requestBody.put("page", 1);
                requestBody.put("per_page", 30);
                requestBody.put("refresh", false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            SharedPreferences data = getSharedPreferences("data", 0);
            String domain = data.getString("domain", "");
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, requestBody.toString());
            Request request = new Request.Builder()
                    .url(domain + "api/fs/get")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(PlayerActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
                    });

                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {

                            String responseBody = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseBody);
                            int code = jsonObject.getInt("code");
                            if (code == 200) {
                                JSONObject data = jsonObject.getJSONObject("data");
                                String url = data.getString("raw_url");
                                runOnUiThread(() -> {

                                    videoView.setUrl(url);
                                    videoView.start();
                                });
                                videoTransCoding.clear();
                                if (type == 2) {
                                    new Thread(PlayerActivity.this).start();
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        }
    }
}
