package com.keenon.peanut.sample.test;

import android.os.AsyncTask;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HelloDuyActivity extends BaseActivity {

    private Button btnTestApi;
    private ListView listViewUsers;
    private TextView tvStatus;
    private UserAdapter userAdapter;
    private List<User> userList;

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

        // Set click listener cho nút Test API
        btnTestApi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear data cũ trước khi gọi API mới
                userList.clear();
                userAdapter.notifyDataSetChanged();
                tvStatus.setText("Đang gọi API...");
                new ApiCallTask().execute();
            }
        });
    }
    

    
    private class ApiCallTask extends AsyncTask<Void, Void, String> {
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(HelloDuyActivity.this, "Đang gọi API...", Toast.LENGTH_SHORT).show();
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                Log.d("API_CALL", "Starting API call to: https://jsonplaceholder.typicode.com/users");
                
                URL url = new URL("https://jsonplaceholder.typicode.com/users");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                Log.d("API_CALL", "Connecting to API...");
                int responseCode = connection.getResponseCode();
                Log.d("API_CALL", "Response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("API_CALL", "Reading response...");
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String result = response.toString();
                    Log.d("API_CALL", "Response length: " + result.length() + " characters");
                    return result;
                } else {
                    Log.e("API_CALL", "HTTP Error: " + responseCode);
                    return "Lỗi HTTP: " + responseCode;
                }
            } catch (Exception e) {
                Log.e("API_CALL", "Exception during API call: " + e.getMessage(), e);
                return "Lỗi: " + e.getMessage();
            }
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            
            try {
                // Log raw response
                Log.d("API_CALL", "Raw response: " + result);
                
                // Parse JSON response using Gson
                Gson gson = new Gson();
                Type userListType = new TypeToken<List<User>>(){}.getType();
                List<User> users = gson.fromJson(result, userListType);
                
                if (users != null && !users.isEmpty()) {
                    // Update UI
                    userList.clear();
                    userList.addAll(users);
                    userAdapter.notifyDataSetChanged();
                    
                    // Update status
                    tvStatus.setText("Đã tải " + users.size() + " users thành công!");
                    
                    // Log detailed user information
                    Log.d("API_CALL", "Total users: " + users.size());
                    
                    for (int i = 0; i < Math.min(users.size(), 3); i++) { // Log first 3 users
                        User user = users.get(i);
                        Log.d("API_CALL", "User " + (i + 1) + ":");
                        Log.d("API_CALL", "  - ID: " + user.getId());
                        Log.d("API_CALL", "  - Name: " + user.getName());
                        Log.d("API_CALL", "  - Username: " + user.getUsername());
                        Log.d("API_CALL", "  - Email: " + user.getEmail());
                        
                        if (user.getAddress() != null) {
                            Log.d("API_CALL", "  - Address: " + user.getAddress().getFullAddress());
                        }
                        
                        if (user.getCompany() != null) {
                            Log.d("API_CALL", "  - Company: " + user.getCompany().getName());
                        }
                        
                        Log.d("API_CALL", "  - Phone: " + user.getPhone());
                        Log.d("API_CALL", "  - Website: " + user.getWebsite());
                    }
                    
                    Toast.makeText(HelloDuyActivity.this, 
                            "API call thành công! Đã tải " + users.size() + " users", 
                            Toast.LENGTH_LONG).show();
                } else {
                    tvStatus.setText("Không có dữ liệu users");
                    Toast.makeText(HelloDuyActivity.this, "Không có dữ liệu users", Toast.LENGTH_LONG).show();
                }
                
            } catch (Exception e) {
                Log.e("API_CALL", "Error parsing JSON: " + e.getMessage(), e);
                tvStatus.setText("Lỗi: " + e.getMessage());
                Toast.makeText(HelloDuyActivity.this, "Lỗi parse JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}