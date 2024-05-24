package com.example.tv;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tv.ui.PlayerActivity;
import com.example.tv.ui.anim.MyItemAnimator;
import com.example.tv.ui.theme.DividerItemDecoration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import github.jasonhancn.tvcursor.TvCursorActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements Runnable {

    private String domain = "http://192.168.3.114:5678/";
    private String url = "api/fs/list";

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<JSONObject> itemList = new ArrayList<>();
    String name = "";
    private View pb;
    private int selectedPosition = -1;
    private String cacheJson;
    private boolean isScrolling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerview);
        pb = findViewById(R.id.pb);
        name = getIntent().getStringExtra("name");
        if (TextUtils.isEmpty(name)) {
            name = "/";
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration());
        adapter = new ItemAdapter(itemList);
        recyclerView.setAdapter(adapter);
        recyclerView.requestFocus();

//        showCursor();
        SharedPreferences data = getSharedPreferences("data", 0);
        domain = data.getString("domain", "");
        if (TextUtils.isEmpty(domain)) {
            showDialog();
        } else {
            url = domain + url;
            cacheJson = data.getString(name,"");
            if(!TextUtils.isEmpty(cacheJson)){
                try {
                    setList(new JSONObject(cacheJson));
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
            new Thread(this).start();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                showDialog();
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入IP地址者域名");
        builder.setMessage("域名为https的请输入以https开头的地址");
        final EditText input = new EditText(this);
        input.setHint("输入IP地址或域名");
        builder.setView(input);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取输入框内容
                String inputText = input.getText().toString();
                if (!TextUtils.isEmpty(inputText)) {
                    if (!inputText.startsWith("http")) {
                        inputText = "http://" + inputText;

                    }
                    if (!inputText.endsWith("/")) {
                        inputText = inputText + "/";
                    }
                    SharedPreferences data = getSharedPreferences("data", 0);
                    data.edit().putString("domain", inputText).commit();
                    domain = inputText;
                    url = domain + url;
                    new Thread(MainActivity.this).start();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

// 显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void selectPreviousItem() {
        if (selectedPosition > 0) {
            int previousSelectedPosition = selectedPosition;
            selectedPosition--;
            recyclerView.getAdapter().notifyItemChanged(previousSelectedPosition);
            recyclerView.getAdapter().notifyItemChanged(selectedPosition);
        }
    }


    private void sendPostRequest() {

        System.out.println(url);
        OkHttpClient client = new OkHttpClient();

        JSONObject requestBody = new JSONObject();
        String path = "";
        if ("".equals(name)) {
            path = "/";
        } else {
            path = name;
        }
        try {
            requestBody.put("path", path);
            requestBody.put("password", "");
            requestBody.put("page", 1);
            requestBody.put("per_page", 13000);
            requestBody.put("refresh", false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, requestBody.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
                });

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        System.out.println(responseBody);

                        JSONObject jsonObject = new JSONObject(responseBody);
                        int code = jsonObject.getInt("code");
                        String message = jsonObject.getString("message");
                        if (code == 200) {
                            if(!responseBody.equals(cacheJson)){
                                SharedPreferences data = getSharedPreferences("data", 0);
                                data.edit().putString(name, responseBody).commit();
                                setList(jsonObject);
                            }

                        } else {

                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                pb.setVisibility(View.GONE);
                            });
                            finish();

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            pb.setVisibility(View.GONE);
                        });
                        finish();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setList(JSONObject jsonObject) throws JSONException {
        JSONArray contentArray = jsonObject.getJSONObject("data").getJSONArray("content");

        runOnUiThread(() -> {
            itemList.clear();
            try {
                for (int i = 0; i < contentArray.length(); i++) {
                    JSONObject itemObject = contentArray.getJSONObject(i);
                    itemList.add(itemObject);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            adapter.notifyDataSetChanged();
            pb.setVisibility(View.GONE);
        });
    }


    // RecyclerView 适配器
    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

        private List<JSONObject> itemList;

        public ItemAdapter(List<JSONObject> itemList) {
            this.itemList = itemList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject item = itemList.get(position);
            try {
                holder.nameTextView.setText(item.getString("name"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (position == selectedPosition) {
                holder.itemView.setBackgroundColor(Color.BLUE);
            } else {
                holder.itemView.setBackgroundColor(Color.WHITE);
            }
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView nameTextView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
//                        Toast.makeText(MainActivity.this,"on focusChanged",Toast.LENGTH_SHORT).show();
                        if (b) {
                            view.setBackgroundColor(Color.parseColor("#4d7AFF"));
                        } else {
                            view.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        }
                    }
                });
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    try {
                        final JSONObject item = itemList.get(position);
                        boolean isDir = item.getBoolean("is_dir");
                        if (isDir) {
                            Intent intent = new Intent(MainActivity.this, MainActivity.class);
                            intent.putExtra("name", MainActivity.this.name + "/" + item.getString("name"));
                            startActivity(intent);
                        } else {
                            int type = item.getInt("type");
                            if (type == 2) {
                                runOnUiThread(() -> {
                                    pb.setVisibility(View.VISIBLE);
                                });
                                new Thread(() -> {
                                    try {
                                        String path = MainActivity.this.name + "/" + item.getString("name");
                                        getVideo(path, item.getString("name"));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        sendPostRequest();
    }

    private void getVideo(String path, String name) {
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
                    Toast.makeText(MainActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
                    runOnUiThread(() -> {
                        pb.setVisibility(View.GONE);
                    });
                });

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {

                        String responseBody = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseBody);
                        int code = jsonObject.getInt("code");
                        String message = jsonObject.getString("message");
                        if (code == 200) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            String url = data.getString("raw_url");
                            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                            intent.putExtra("url", url);
                            intent.putExtra("path",path);
                            intent.putExtra("name", name);
                            startActivity(intent);
                        } else {

                            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
                            finish();

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
                }
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                });
            }
        });
    }

}
