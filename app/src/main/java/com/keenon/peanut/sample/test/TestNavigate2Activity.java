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
import com.keenon.peanut.common.resouorce.resourceBean.Resource;
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
    private Button btnLoadMapWithSDK;
    private Button btnCheckMapStatus;
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
        setupClickListeners();
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        logMessage("TestNavigate2Activity khởi tạo thành công");
        logMessage("Sử dụng PeanutSDK.getInstance().navigation() API");
        
        // Khởi tạo SDK và Navigation
        initializeSDKAndNavigation();
    }
    
    private void initializeSDKAndNavigation() {
        logMessage("=== KHỞI TẠO SDK VÀ NAVIGATION ===");
        
        // Khởi tạo SDK
        PeanutSDKManager.initializeSDK(this, new PeanutSDK.ErrorListener() {
            @Override
            public void onInit(int statusCode) {
                mainHandler.post(() -> {
                    if (statusCode == PeanutSDK.SDK_INIT_SUCCESS) {
                        logMessage("✅ PeanutSDK khởi tạo thành công!");
                        initNavigation();
                        
                        // Kiểm tra map status sau khi SDK init
                        checkMapStatus();
                    } else {
                        logMessage("❌ PeanutSDK khởi tạo thất bại với mã: " + statusCode);
                        Toast.makeText(TestNavigate2Activity.this, "SDK init thất bại: " + statusCode, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
    
    private void initViews() {
        etTargetId = findViewById(R.id.et_target_id);
        etApproximateTime = findViewById(R.id.et_approximate_time);
        btnLoadMap = findViewById(R.id.btn_load_map);
        btnLoadMapWithSDK = findViewById(R.id.btn_load_map_with_sdk);
        btnCheckMapStatus = findViewById(R.id.btn_check_map_status);
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
        
        // Set button text for new buttons
        if (btnLoadMapWithSDK != null) {
            btnLoadMapWithSDK.setText("Load Map (SDK)");
        }
        if (btnCheckMapStatus != null) {
            btnCheckMapStatus.setText("Check Map Status");
        }
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
        if (btnLoadMapWithSDK != null) {
            btnLoadMapWithSDK.setOnClickListener(v -> loadMapWithSDK());
        }
        if (btnCheckMapStatus != null) {
            btnCheckMapStatus.setOnClickListener(v -> checkMapStatus());
        }
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
        
        // Tạo header thông tin tổng quan
        TextView headerInfo = new TextView(this);
        headerInfo.setText("📊 THÔNG TIN MAP TARGETS:");
        headerInfo.setTextSize(16);
        headerInfo.setTextColor(0xFF2196F3);
        headerInfo.setPadding(0, 0, 0, 16);
        layoutTargets.addView(headerInfo);
        
        // Hiển thị tổng số targets
        TextView totalInfo = new TextView(this);
        totalInfo.setText("Tổng số targets: " + navigationPointBean.getCount());
        totalInfo.setTextSize(14);
        totalInfo.setTextColor(0xFF666666);
        totalInfo.setPadding(0, 0, 0, 8);
        layoutTargets.addView(totalInfo);
        
        // Hiển thị các điểm đích từ list (Targets thông thường)
        if (navigationPointBean.getList() != null && !navigationPointBean.getList().isEmpty()) {
            logMessage("🗺️ Hiển thị " + navigationPointBean.getList().size() + " điểm đích thông thường:");
            
            TextView sectionHeader = new TextView(this);
            sectionHeader.setText("🎯 TARGETS THÔNG THƯỜNG (" + navigationPointBean.getList().size() + "):");
            sectionHeader.setTextSize(14);
            sectionHeader.setTextColor(0xFF4CAF50);
            sectionHeader.setPadding(0, 16, 0, 8);
            layoutTargets.addView(sectionHeader);
            
            for (Integer targetId : navigationPointBean.getList()) {
                Button btnTarget = new Button(this);
                btnTarget.setText("🎯 Target ID: " + targetId);
                btnTarget.setBackgroundColor(0xFF4CAF50);
                btnTarget.setTextColor(0xFFFFFFFF);
                btnTarget.setPadding(16, 8, 16, 8);
                btnTarget.setOnClickListener(v -> {
                    etTargetId.setText(String.valueOf(targetId));
                    logMessage("✅ Đã chọn điểm đích: ID=" + targetId + " (Target thông thường)");
                    Toast.makeText(this, "Chọn Target ID: " + targetId, Toast.LENGTH_SHORT).show();
                });
                layoutTargets.addView(btnTarget);
            }
        }
        
        // Hiển thị các điểm sạc từ chargers
        if (navigationPointBean.getChargers() != null && !navigationPointBean.getChargers().isEmpty()) {
            logMessage("🔋 Hiển thị " + navigationPointBean.getChargers().size() + " điểm sạc:");
            
            TextView sectionHeader = new TextView(this);
            sectionHeader.setText("🔋 ĐIỂM SẠC (" + navigationPointBean.getChargers().size() + "):");
            sectionHeader.setTextSize(14);
            sectionHeader.setTextColor(0xFF2196F3);
            sectionHeader.setPadding(0, 16, 0, 8);
            layoutTargets.addView(sectionHeader);
            
            for (Integer chargerId : navigationPointBean.getChargers()) {
                Button btnCharger = new Button(this);
                btnCharger.setText("🔋 Charger ID: " + chargerId);
                btnCharger.setBackgroundColor(0xFF2196F3);
                btnCharger.setTextColor(0xFFFFFFFF);
                btnCharger.setPadding(16, 8, 16, 8);
                btnCharger.setOnClickListener(v -> {
                    etTargetId.setText(String.valueOf(chargerId));
                    logMessage("✅ Đã chọn điểm sạc: ID=" + chargerId + " (Charger)");
                    Toast.makeText(this, "Chọn Charger ID: " + chargerId, Toast.LENGTH_SHORT).show();
                });
                layoutTargets.addView(btnCharger);
            }
        }
        
        // Hiển thị các điểm thang máy từ elevators
        if (navigationPointBean.getElevators() != null && !navigationPointBean.getElevators().isEmpty()) {
            logMessage("🛗 Hiển thị " + navigationPointBean.getElevators().size() + " điểm thang máy:");
            
            TextView sectionHeader = new TextView(this);
            sectionHeader.setText("🛗 THANG MÁY (" + navigationPointBean.getElevators().size() + "):");
            sectionHeader.setTextSize(14);
            sectionHeader.setTextColor(0xFF9C27B0);
            sectionHeader.setPadding(0, 16, 0, 8);
            layoutTargets.addView(sectionHeader);
            
            for (Integer elevatorId : navigationPointBean.getElevators()) {
                Button btnElevator = new Button(this);
                btnElevator.setText("🛗 Elevator ID: " + elevatorId);
                btnElevator.setBackgroundColor(0xFF9C27B0);
                btnElevator.setTextColor(0xFFFFFFFF);
                btnElevator.setPadding(16, 8, 16, 8);
                btnElevator.setOnClickListener(v -> {
                    etTargetId.setText(String.valueOf(elevatorId));
                    logMessage("✅ Đã chọn điểm thang máy: ID=" + elevatorId + " (Elevator)");
                    Toast.makeText(this, "Chọn Elevator ID: " + elevatorId, Toast.LENGTH_SHORT).show();
                });
                layoutTargets.addView(btnElevator);
            }
        }
        
        // Hiển thị các điểm gốc từ origins
        if (navigationPointBean.getOrigins() != null && !navigationPointBean.getOrigins().isEmpty()) {
            logMessage("🏠 Hiển thị " + navigationPointBean.getOrigins().size() + " điểm gốc:");
            
            TextView sectionHeader = new TextView(this);
            sectionHeader.setText("🏠 ĐIỂM GỐC (" + navigationPointBean.getOrigins().size() + "):");
            sectionHeader.setTextSize(14);
            sectionHeader.setTextColor(0xFFFF9800);
            sectionHeader.setPadding(0, 16, 0, 8);
            layoutTargets.addView(sectionHeader);
            
            for (Integer originId : navigationPointBean.getOrigins()) {
                Button btnOrigin = new Button(this);
                btnOrigin.setText("🏠 Origin ID: " + originId);
                btnOrigin.setBackgroundColor(0xFFFF9800);
                btnOrigin.setTextColor(0xFFFFFFFF);
                btnOrigin.setPadding(16, 8, 16, 8);
                btnOrigin.setOnClickListener(v -> {
                    etTargetId.setText(String.valueOf(originId));
                    logMessage("✅ Đã chọn điểm gốc: ID=" + originId + " (Origin)");
                    Toast.makeText(this, "Chọn Origin ID: " + originId, Toast.LENGTH_SHORT).show();
                });
                layoutTargets.addView(btnOrigin);
            }
        }
        
        // Hiển thị thông tin tổng kết
        TextView summaryInfo = new TextView(this);
        int totalLocations = (navigationPointBean.getList() != null ? navigationPointBean.getList().size() : 0) +
                           (navigationPointBean.getChargers() != null ? navigationPointBean.getChargers().size() : 0) +
                           (navigationPointBean.getElevators() != null ? navigationPointBean.getElevators().size() : 0) +
                           (navigationPointBean.getOrigins() != null ? navigationPointBean.getOrigins().size() : 0);
        
        summaryInfo.setText("📋 TỔNG KẾT: " + totalLocations + " locations có sẵn trong map");
        summaryInfo.setTextSize(12);
        summaryInfo.setTextColor(0xFF666666);
        summaryInfo.setPadding(0, 16, 0, 8);
        layoutTargets.addView(summaryInfo);
        
        logMessage("✅ Đã hiển thị " + totalLocations + " locations trong map");
    }
    
    private void loadMapWithSDK() {
        logMessage("=== BẮT ĐẦU LOAD MAP VỚI PEANUTSDKMANAGER ===");
        
        try {
            if (!PeanutSDKManager.isInitialized()) {
                logMessage("❌ PeanutSDK chưa được khởi tạo!");
                Toast.makeText(this, "PeanutSDK chưa được khởi tạo", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Kiểm tra xem có cần load map không
            boolean shouldLoad = PeanutSDKManager.shouldLoadMap();
            logMessage("Có cần load map: " + shouldLoad);
            
            if (!shouldLoad) {
                logMessage("ℹ️ Không cần load map cho machine type này");
                Toast.makeText(this, "Không cần load map cho machine type này", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Kiểm tra và load map tự động
            PeanutSDKManager.checkAndLoadMap(this, new PeanutSDKManager.MapLoadListener() {
                @Override
                public void onMapLoadStart() {
                    mainHandler.post(() -> {
                        logMessage("🔄 Bắt đầu load map...");
                        Toast.makeText(TestNavigate2Activity.this, "Bắt đầu load map...", Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onMapLoadSuccess() {
                    mainHandler.post(() -> {
                        logMessage("✅ Map load thành công!");
                        Toast.makeText(TestNavigate2Activity.this, "Map load thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Sau khi load map thành công, load lại targets
                        loadMapTargets();
                    });
                }
                
                @Override
                public void onMapLoadError(String error) {
                    mainHandler.post(() -> {
                        logMessage("❌ Map load lỗi: " + error);
                        Toast.makeText(TestNavigate2Activity.this, "Map load lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
                
                @Override
                public void onMapLoadProgress(int progress) {
                    mainHandler.post(() -> {
                        logMessage("📊 Map load progress: " + progress + "%");
                    });
                }
            });
            
        } catch (Exception e) {
            logMessage("Exception khi load map với SDK: " + e.getMessage());
            Log.e(TAG, "Error loading map with SDK", e);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkMapStatus() {
        logMessage("=== KIỂM TRA TRẠNG THÁI MAP ===");
        
        try {
            if (!PeanutSDKManager.isInitialized()) {
                logMessage("❌ PeanutSDK chưa được khởi tạo!");
                return;
            }
            
            // Kiểm tra trạng thái map loading
            boolean isMapLoading = PeanutSDKManager.isMapLoading();
            logMessage("Map đang loading: " + isMapLoading);
            
            // Kiểm tra xem có cần load map không
            boolean shouldLoad = PeanutSDKManager.shouldLoadMap();
            logMessage("Có cần load map: " + shouldLoad);
            
            // Hiển thị thông tin tổng hợp
            String statusInfo = "Map Status:\n" +
                    "- Đang loading: " + isMapLoading + "\n" +
                    "- Cần load: " + shouldLoad + "\n" +
                    "- SDK initialized: " + PeanutSDKManager.isInitialized();
            
            logMessage(statusInfo);
            Toast.makeText(this, "Map Status: " + (isMapLoading ? "Loading" : "Ready"), Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            logMessage("Exception khi check map status: " + e.getMessage());
            Log.e(TAG, "Error checking map status", e);
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
        
        // Cleanup map loading nếu đang loading
        if (PeanutSDKManager.isMapLoading()) {
            logMessage("Dừng map loading trước khi destroy...");
            PeanutSDKManager.stopMapLoading();
        }
        
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        logMessage("TestNavigate2Activity đã destroy");
    }
}