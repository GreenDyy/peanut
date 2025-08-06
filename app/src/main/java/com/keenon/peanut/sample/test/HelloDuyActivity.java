package com.keenon.peanut.sample.test;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;

import java.io.IOException;
import java.lang.reflect.Type;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.ArrayList;
import java.util.List;

public class HelloDuyActivity extends BaseActivity {

    private Button btnTestApi;
    private ListView listViewUsers;
    private TextView tvStatus;
    private UserAdapter userAdapter;
    private List<User> userList;
    private OkHttpClient okHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_duy);
        
        // Hiển thị Toast "hello Duy"
        Toast.makeText(this, "hello Duy", Toast.LENGTH_LONG).show();
        
        setTitle("Hello Duy Function");
        
        // Hiển thị nút back trên ActionBar
        setButtonBack();
        
        // Khởi tạo views
        btnTestApi = findViewById(R.id.btn_test_api);
        listViewUsers = findViewById(R.id.list_view_users);
        tvStatus = findViewById(R.id.tv_status);
        
        // Khởi tạo adapter
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList);
        listViewUsers.setAdapter(userAdapter);
        
        // Khởi tạo OkHttpClient
        okHttpClient = new OkHttpClient();

        // Set click listener cho ListView items
        listViewUsers.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userAdapter.getItem(position);
            if (selectedUser != null) {
                Gson gson = new Gson();

                // Chuyển đối tượng thành chuỗi JSON
                String userJson = gson.toJson(selectedUser);

                // Log chuỗi JSON ra Logcat
                Log.d("HelloDuyActivity", "User selected: " + userJson);
                Toast.makeText(HelloDuyActivity.this,
                        "User ID: " + selectedUser.getId(),
                        Toast.LENGTH_SHORT).show();
                System.out.println("User selected: " + userJson);
            }
        });

        // Set click listener cho nút Test API
        btnTestApi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear data cũ trước khi gọi API mới
                userList.clear();
                userAdapter.notifyDataSetChanged();
                tvStatus.setText("Đang gọi API...");
                callApiWithOkHttp();
            }
        });
    }
    

    
    private void callApiWithOkHttp() {
        Toast.makeText(this, "Đang gọi API với OkHttp...", Toast.LENGTH_SHORT).show();
        
        String url = "https://jsonplaceholder.typicode.com/users";
        Log.d("OKHTTP_CALL", "Starting OkHttp API call to: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OKHTTP_CALL", "API call failed: " + e.getMessage(), e);
                
                // Cập nhật UI trên main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Lỗi kết nối: " + e.getMessage());
                        Toast.makeText(HelloDuyActivity.this, 
                                "Lỗi kết nối: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("OKHTTP_CALL", "Response received, length: " + responseBody.length() + " characters");
                    Log.d("OKHTTP_CALL", "Response code: " + response.code());
                    
                    // Parse JSON trên background thread
                    try {
                        Gson gson = new Gson();
                        Type userListType = new TypeToken<List<User>>(){}.getType();
                        List<User> users = gson.fromJson(responseBody, userListType);
                        
                        // Cập nhật UI trên main thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleApiSuccess(users);
                            }
                        });
                        
                    } catch (Exception e) {
                        Log.e("OKHTTP_CALL", "Error parsing JSON: " + e.getMessage(), e);
                        
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvStatus.setText("Lỗi parse JSON: " + e.getMessage());
                                Toast.makeText(HelloDuyActivity.this, 
                                        "Lỗi parse JSON: " + e.getMessage(), 
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } else {
                    Log.e("OKHTTP_CALL", "HTTP Error: " + response.code());
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Lỗi HTTP: " + response.code());
                            Toast.makeText(HelloDuyActivity.this, 
                                    "Lỗi HTTP: " + response.code(), 
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
                response.close();
            }
        });
    }
    
    private void handleApiSuccess(List<User> users) {
        if (users != null && !users.isEmpty()) {
            // Update UI
            userList.clear();
            userList.addAll(users);
            userAdapter.notifyDataSetChanged();
            
            // Update status
            tvStatus.setText("Đã tải " + users.size() + " users thành công với OkHttp!");
            
            // Log detailed user information
            Log.d("OKHTTP_CALL", "Total users: " + users.size());
            
            for (int i = 0; i < Math.min(users.size(), 3); i++) { // Log first 3 users
                User user = users.get(i);
                Log.d("OKHTTP_CALL", "User " + (i + 1) + ":");
                Log.d("OKHTTP_CALL", "  - ID: " + user.getId());
                Log.d("OKHTTP_CALL", "  - Name: " + user.getName());
                Log.d("OKHTTP_CALL", "  - Username: " + user.getUsername());
                Log.d("OKHTTP_CALL", "  - Email: " + user.getEmail());
                
                if (user.getAddress() != null) {
                    Log.d("OKHTTP_CALL", "  - Address: " + user.getAddress().getFullAddress());
                }
                
                if (user.getCompany() != null) {
                    Log.d("OKHTTP_CALL", "  - Company: " + user.getCompany().getName());
                }
                
                Log.d("OKHTTP_CALL", "  - Phone: " + user.getPhone());
                Log.d("OKHTTP_CALL", "  - Website: " + user.getWebsite());
            }
            
            Toast.makeText(this, 
                    "OkHttp API call thành công! Đã tải " + users.size() + " users", 
                    Toast.LENGTH_LONG).show();
        } else {
            tvStatus.setText("Không có dữ liệu users");
            Toast.makeText(this, "Không có dữ liệu users", Toast.LENGTH_LONG).show();
        }
    }
}