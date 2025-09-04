package com.keenon.peanut.sample.test;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PeanutSDKManager;
import com.keenon.sdk.component.NavigationComponent;
import com.keenon.sdk.robot.ApiCallback;
import com.keenon.sdk.robot.base.BaseResp;
import com.keenon.sdk.robot.model.bean.navigation.NavigationPointBean;
import com.keenon.sdk.robot.model.bean.navigation.NavigationStatusBean;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

public class TestNavigate2Activity extends BaseActivity {
    private static final String TAG = "TestNavigate2Activity";
    
    private EditText etTargetId;
    private EditText etApproximateTime;
    private Button btnLoadMap;
    private Button btnNavigate;
    private Button btnStopNavigation;
    private Button btnPauseNavigation;
    private Button btnResumeNavigation;
    private Button btnGetNavigationStatus;
    private TextView tvLog;
    private ScrollView scrollView;
    private LinearLayout layoutTargets;
    
    private NavigationComponent navigationComponent;
    private NavigationPointBean navigationPointBean;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_navigate2);
        
        initViews();
        initNavigation();
        setupClickListeners();
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        logMessage("TestNavigate2Activity khởi tạo thành công");
        logMessage("Sử dụng PeanutSDK.getInstance().navigation() API");
    }
    
    private void initViews() {
        etTargetId = findViewById(R.id.et_target_id);
        etApproximateTime = findViewById(R.id.et_approximate_time);
        btnLoadMap = findViewById(R.id.btn_load_map);
        btnNavigate = findViewById(R.id.btn_navigate);
        btnStopNavigation = findViewById(R.id.btn_stop_navigation);
        btnPauseNavigation = findViewById(R.id.btn_pause_navigation);
        btnResumeNavigation = findViewById(R.id.btn_resume_navigation);
        btnGetNavigationStatus = findViewById(R.id.btn_get_navigation_status);
        tvLog = findViewById(R.id.tv_log);
        scrollView = findViewById(R.id.scroll_view);
        layoutTargets = findViewById(R.id.layout_targets);
        
        // Set default values
        etTargetId.setText("6");
        etApproximateTime.setText("1");
    }
    
    private void initNavigation() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                navigationComponent = PeanutSDK.getInstance().navigation();
                logMessage("PeanutSDK NavigationComponent khởi tạo thành công");
            } else {
                logMessage("PeanutSDK hoặc Navigation component chưa được khởi tạo");
            }
        } catch (Exception e) {
            logMessage("Lỗi khởi tạo NavigationComponent: " + e.getMessage());
            Log.e(TAG, "Error initializing NavigationComponent", e);
        }
    }
    
    private void setupClickListeners() {
        btnLoadMap.setOnClickListener(v -> loadMapTargets());
        btnNavigate.setOnClickListener(v -> startNavigation());
        btnStopNavigation.setOnClickListener(v -> stopNavigation());
        btnPauseNavigation.setOnClickListener(v -> pauseNavigation());
        btnResumeNavigation.setOnClickListener(v -> resumeNavigation());
        btnGetNavigationStatus.setOnClickListener(v -> getNavigationStatus());
    }
    
    private void loadMapTargets() {
        logMessage("=== BẮT ĐẦU LOAD MAP TARGETS ===");
        
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                // Sử dụng PeanutSDK API để lấy tất cả các điểm đích
                PeanutSDK.getInstance().navigation().getAllTargets(new ApiCallback<BaseResp<NavigationPointBean>>() {
                    @Override
                    public void onSuccess(BaseResp<NavigationPointBean> response) {
                        mainHandler.post(() -> {
                            if (response != null && response.getCode() == 0 && response.getData() != null) {
                                // NavigationPointBean chứa danh sách các điểm đích
                                navigationPointBean = response.getData();
                                logMessage("Load map thành công!");
                                logMessage("Count: " + navigationPointBean.getCount());
                                logMessage("List: " + navigationPointBean.getList());
                                logMessage("Chargers: " + navigationPointBean.getChargers());
                                logMessage("Elevators: " + navigationPointBean.getElevators());
                                logMessage("Origins: " + navigationPointBean.getOrigins());
                                
                                // Hiển thị danh sách các điểm đích
                                displayTargets();
                            } else {
                                logMessage("Load map thất bại: " + (response != null ? response.getMsg() : "Response null"));
                            }
                        });
                    }
                    
                    @Override
                    public void onSuccess(String requestId, BaseResp<NavigationPointBean> result) {
                        onSuccess(result);
                    }
                    
                    @Override
                    public void onFail(ApiError error) {
                        mainHandler.post(() -> {
                            logMessage("Lỗi load map - Code: " + error.getCode() + ", Message: " + error.getMsg());
                        });
                    }
                });
            } else {
                logMessage("PeanutSDK hoặc Navigation component chưa được khởi tạo");
            }
            
        } catch (Exception e) {
            logMessage("Exception khi load map: " + e.getMessage());
            Log.e(TAG, "Error loading map targets", e);
        }
    }
    
    private void displayTargets() {
        layoutTargets.removeAllViews();
        
        if (navigationPointBean == null) {
            logMessage("NavigationPointBean is null");
            return;
        }
        
        // Hiển thị các điểm đích từ list
        if (navigationPointBean.getList() != null && !navigationPointBean.getList().isEmpty()) {
            logMessage("Hiển thị " + navigationPointBean.getList().size() + " điểm đích từ list:");
            for (Integer targetId : navigationPointBean.getList()) {
                Button btnTarget = new Button(this);
                btnTarget.setText("Target ID: " + targetId);
                btnTarget.setOnClickListener(v -> {
                    etTargetId.setText(String.valueOf(targetId));
                    logMessage("Đã chọn điểm đích: ID=" + targetId);
                });
                layoutTargets.addView(btnTarget);
            }
        }
        
        // Hiển thị các điểm sạc từ chargers
        if (navigationPointBean.getChargers() != null && !navigationPointBean.getChargers().isEmpty()) {
            logMessage("Hiển thị " + navigationPointBean.getChargers().size() + " điểm sạc:");
            for (Integer chargerId : navigationPointBean.getChargers()) {
                Button btnCharger = new Button(this);
                btnCharger.setText("Charger ID: " + chargerId);
                btnCharger.setBackgroundColor(0xFF4CAF50); // Màu xanh cho charger
                btnCharger.setOnClickListener(v -> {
                    etTargetId.setText(String.valueOf(chargerId));
                    logMessage("Đã chọn điểm sạc: ID=" + chargerId);
                });
                layoutTargets.addView(btnCharger);
            }
        }
        
        // Hiển thị các điểm thang máy từ elevators
        if (navigationPointBean.getElevators() != null && !navigationPointBean.getElevators().isEmpty()) {
            logMessage("Hiển thị " + navigationPointBean.getElevators().size() + " điểm thang máy:");
            for (Integer elevatorId : navigationPointBean.getElevators()) {
                Button btnElevator = new Button(this);
                btnElevator.setText("Elevator ID: " + elevatorId);
                btnElevator.setBackgroundColor(0xFF9C27B0); // Màu tím cho elevator
                btnElevator.setOnClickListener(v -> {
                    etTargetId.setText(String.valueOf(elevatorId));
                    logMessage("Đã chọn điểm thang máy: ID=" + elevatorId);
                });
                layoutTargets.addView(btnElevator);
            }
        }
        
        // Hiển thị các điểm gốc từ origins
        if (navigationPointBean.getOrigins() != null && !navigationPointBean.getOrigins().isEmpty()) {
            logMessage("Hiển thị " + navigationPointBean.getOrigins().size() + " điểm gốc:");
            for (Integer originId : navigationPointBean.getOrigins()) {
                Button btnOrigin = new Button(this);
                btnOrigin.setText("Origin ID: " + originId);
                btnOrigin.setBackgroundColor(0xFFFF9800); // Màu cam cho origin
                btnOrigin.setOnClickListener(v -> {
                    etTargetId.setText(String.valueOf(originId));
                    logMessage("Đã chọn điểm gốc: ID=" + originId);
                });
                layoutTargets.addView(btnOrigin);
            }
        }
    }
    
    private void startNavigation() {
        String targetIdStr = etTargetId.getText().toString().trim();
        String approximateTimeStr = etApproximateTime.getText().toString().trim();
        
        if (targetIdStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Target ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int targetId = Integer.parseInt(targetIdStr);
            int approximateTime = approximateTimeStr.isEmpty() ? 1 : Integer.parseInt(approximateTimeStr);
            
            logMessage("=== BẮT ĐẦU NAVIGATION ===");
            logMessage("Target ID: " + targetId);
            logMessage("Approximate Time: " + approximateTime);
            
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                // Sử dụng PeanutSDK API để navigation
                PeanutSDK.getInstance().navigation().setTarget(targetId, approximateTime, new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> response) {
                        mainHandler.post(() -> {
                            if (response != null && response.getCode() == 0) {
                                logMessage("Navigation bắt đầu thành công!");
                                logMessage("Response: " + response.getData());
                            } else {
                                logMessage("Navigation thất bại: " + (response != null ? response.getMsg() : "Response null"));
                            }
                        });
                    }
                    
                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }
                    
                    @Override
                    public void onFail(ApiError error) {
                        mainHandler.post(() -> {
                            logMessage("Lỗi navigation - Code: " + error.getCode() + ", Message: " + error.getMsg());
                        });
                    }
                });
            } else {
                logMessage("PeanutSDK hoặc Navigation component chưa được khởi tạo");
            }
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Target ID phải là số nguyên", Toast.LENGTH_SHORT).show();
            logMessage("Lỗi format số: " + e.getMessage());
        } catch (Exception e) {
            logMessage("Exception khi start navigation: " + e.getMessage());
            Log.e(TAG, "Error starting navigation", e);
        }
    }
    
    private void stopNavigation() {
        logMessage("=== DỪNG NAVIGATION ===");
        
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().stop(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> response) {
                        mainHandler.post(() -> {
                            if (response != null && response.getCode() == 0) {
                                logMessage("Navigation đã dừng thành công!");
                                logMessage("Response: " + response.getData());
                            } else {
                                logMessage("Dừng navigation thất bại: " + (response != null ? response.getMsg() : "Response null"));
                            }
                        });
                    }
                    
                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }
                    
                    @Override
                    public void onFail(ApiError error) {
                        mainHandler.post(() -> {
                            logMessage("Lỗi dừng navigation - Code: " + error.getCode() + ", Message: " + error.getMsg());
                        });
                    }
                });
            } else {
                logMessage("PeanutSDK hoặc Navigation component chưa được khởi tạo");
            }
            
        } catch (Exception e) {
            logMessage("Exception khi stop navigation: " + e.getMessage());
            Log.e(TAG, "Error stopping navigation", e);
        }
    }
    
    private void pauseNavigation() {
        logMessage("=== TẠM DỪNG NAVIGATION ===");
        
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().pause(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> response) {
                        mainHandler.post(() -> {
                            if (response != null && response.getCode() == 0) {
                                logMessage("Navigation đã tạm dừng thành công!");
                                logMessage("Response: " + response.getData());
                            } else {
                                logMessage("Tạm dừng navigation thất bại: " + (response != null ? response.getMsg() : "Response null"));
                            }
                        });
                    }
                    
                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }
                    
                    @Override
                    public void onFail(ApiError error) {
                        mainHandler.post(() -> {
                            logMessage("Lỗi tạm dừng navigation - Code: " + error.getCode() + ", Message: " + error.getMsg());
                        });
                    }
                });
            } else {
                logMessage("PeanutSDK hoặc Navigation component chưa được khởi tạo");
            }
            
        } catch (Exception e) {
            logMessage("Exception khi pause navigation: " + e.getMessage());
            Log.e(TAG, "Error pausing navigation", e);
        }
    }
    
    private void resumeNavigation() {
        logMessage("=== TIẾP TỤC NAVIGATION ===");
        
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().resume(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> response) {
                        mainHandler.post(() -> {
                            if (response != null && response.getCode() == 0) {
                                logMessage("Navigation đã tiếp tục thành công!");
                                logMessage("Response: " + response.getData());
                            } else {
                                logMessage("Tiếp tục navigation thất bại: " + (response != null ? response.getMsg() : "Response null"));
                            }
                        });
                    }
                    
                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }
                    
                    @Override
                    public void onFail(ApiError error) {
                        mainHandler.post(() -> {
                            logMessage("Lỗi tiếp tục navigation - Code: " + error.getCode() + ", Message: " + error.getMsg());
                        });
                    }
                });
            } else {
                logMessage("PeanutSDK hoặc Navigation component chưa được khởi tạo");
            }
            
        } catch (Exception e) {
            logMessage("Exception khi resume navigation: " + e.getMessage());
            Log.e(TAG, "Error resuming navigation", e);
        }
    }
    
    private void getNavigationStatus() {
        logMessage("=== LẤY TRẠNG THÁI NAVIGATION ===");
        
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().getStatus(new ApiCallback<BaseResp<NavigationStatusBean>>() {
                    @Override
                    public void onSuccess(BaseResp<NavigationStatusBean> response) {
                        mainHandler.post(() -> {
                            if (response != null && response.getCode() == 0 && response.getData() != null) {
                                NavigationStatusBean status = response.getData();
                                logMessage("Lấy trạng thái navigation thành công!");
                                logMessage("Status: " + status.toString());
                            } else {
                                logMessage("Lấy trạng thái navigation thất bại: " + (response != null ? response.getMsg() : "Response null"));
                            }
                        });
                    }
                    
                    @Override
                    public void onSuccess(String requestId, BaseResp<NavigationStatusBean> result) {
                        onSuccess(result);
                    }
                    
                    @Override
                    public void onFail(ApiError error) {
                        mainHandler.post(() -> {
                            logMessage("Lỗi lấy trạng thái navigation - Code: " + error.getCode() + ", Message: " + error.getMsg());
                        });
                    }
                });
            } else {
                logMessage("PeanutSDK hoặc Navigation component chưa được khởi tạo");
            }
            
        } catch (Exception e) {
            logMessage("Exception khi get navigation status: " + e.getMessage());
            Log.e(TAG, "Error getting navigation status", e);
        }
    }
    
    private void logMessage(String message) {
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";
        
        runOnUiThread(() -> {
            tvLog.append(logEntry);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
        
        Log.i(TAG, message);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}