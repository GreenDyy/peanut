package com.keenon.peanut.sample.test;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.robot.ApiCallback;
import com.keenon.sdk.robot.base.BaseResp;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.coapapi.api.sensor.DynamicObjectApi;
import com.keenon.sdk.robot.model.bean.sensor.DynamicObjectBean;
import com.keenon.sdk.robot.base.ApiTopic;
import com.keenon.sdk.sensor.headmotor.HeadMotorInterface;
import com.keenon.sdk.sensor.headmotor.SensorHeadMotor;
import com.keenon.sdk.component.runtime.PeanutRuntime;

import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class ObjectPerceptionTestActivity extends BaseActivity {

    private static final String TAG = "ObjectPerceptionTest";
    private static final double PERSON_DETECTION_THRESHOLD = 2.0; // Khoảng cách 2m để phát hiện người
    private static final long GREETING_COOLDOWN = 5000; // 5 giây cooldown giữa các lần chào

    @BindView(R.id.tv_api_log)
    TextView tvApiLog;
    @BindView(R.id.sv_api_log)
    ScrollView svApiLog;
    @BindView(R.id.cb_object_perception)
    CheckBox cbObjectPerception;

    private StringBuilder logBuilder = new StringBuilder();
    private boolean isObjectPerceptionSubscribed = false;
    private DynamicObjectApi dynamicObjectApi;
    private boolean autoGreetingEnabled = true;
    private long lastGreetingTime = 0;
    private boolean isSDKInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_perception_test);
        ButterKnife.bind(this);
        setButtonBack();
        cbObjectPerception.setText("Enabled Perception");
        
        // Khởi tạo DynamicObjectApi (sẽ chỉ dùng khi SDK ready)
        
        addLog("System", "🤖 Smart Greeting Robot initialized");
        addLog("Info", "📏 Detection threshold: " + PERSON_DETECTION_THRESHOLD + "m");
        addLog("Info", "⏱️ Greeting cooldown: " + (GREETING_COOLDOWN/1000) + "s");
        addLog("Status", "✅ Auto greeting: " + (autoGreetingEnabled ? "ENABLED" : "DISABLED"));
        
        // Kiểm tra SDK status
        checkSDKStatus();
    }

    @OnCheckedChanged(R.id.cb_object_perception)
    public void onObjectPerceptionToggle(boolean isChecked) {
        if (isChecked) {
            subscribeObjectPerception();
        } else {
            unsubscribeObjectPerception();
        }
    }

    /**
     * Kiểm tra trạng thái SDK và khởi tạo nếu cần thiết
     */
    private void checkSDKStatus() {
        try {
            if (PeanutSDK.getInstance() != null) {
                addLog("SDK", "✅ PeanutSDK instance available");
                
                // Kiểm tra PeanutRuntime
                if (PeanutRuntime.getInstance() != null) {
                    addLog("Runtime", "✅ PeanutRuntime available");
                    
                    // Khởi tạo DynamicObjectApi khi SDK ready
                    initializeDynamicObjectApi();
                } else {
                    addLog("Runtime", "⚠️ PeanutRuntime not available");
                    addLog("Warning", "🔧 Please ensure app is launched from main launcher first");
                    // Vẫn cho phép thử sử dụng DynamicObjectApi
                    initializeDynamicObjectApi();
                }
            } else {
                addLog("SDK", "❌ PeanutSDK not initialized");
                addLog("Error", "🔧 Please launch app from main launcher to initialize SDK");
            }
        } catch (Exception e) {
            addLog("Error", "❌ SDK check failed: " + e.getMessage());
        }
    }

    /**
     * Khởi tạo DynamicObjectApi một cách an toàn
     */
    private void initializeDynamicObjectApi() {
        try {
            dynamicObjectApi = new DynamicObjectApi();
            addLog("API", "✅ DynamicObjectApi initialized");
            isSDKInitialized = true;  // Chỉ set true khi DynamicObjectApi thành công
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Native library not found: " + e.getMessage());
            addLog("Error", "🚨 This app needs to run on actual robot hardware");
            isSDKInitialized = false;
            cbObjectPerception.setEnabled(false);
        } catch (Exception e) {
            addLog("Error", "❌ Failed to initialize DynamicObjectApi: " + e.getMessage());
            isSDKInitialized = false;
        }
    }

    private void subscribeObjectPerception() {
        // Kiểm tra SDK trước khi sử dụng
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not ready. Please check initialization logs above.");
            cbObjectPerception.setChecked(false);
            return;
        }

        if (dynamicObjectApi == null) {
            addLog("Error", "❌ DynamicObjectApi not available. Please try restarting app.");
            cbObjectPerception.setChecked(false);
            return;
        }

        try {
            addLog("Subscribe", "🔄 Starting person detection system...");
            
            // Sử dụng DynamicObjectApi.observe() theo decompiled code
            dynamicObjectApi.observe(new ApiCallback<BaseResp<String>>() {
                @Override
                public void onSuccess(BaseResp<String> result) {
                    if (result != null && result.getData() != null) {
                        try {
                            // Parse JSON response
                            JSONObject json = new JSONObject(result.getData());
                            DynamicObjectBean data = new DynamicObjectBean(json.optDouble("distance"));
                            data.setExist(json.optBoolean("exist"));
                            data.setLeft(json.optBoolean("left"));
                            data.setMiddle(json.optBoolean("middle"));
                            data.setRight(json.optBoolean("right"));
                            
                            // Kiểm tra có đối tượng và trong phạm vi phát hiện người
                            if (data.getExist() && data.getDistance() <= PERSON_DETECTION_THRESHOLD) {
                                addLog("Detection", "👤 Person detected at " + data.getDistance() + "m");
                                
                                StringBuilder positions = new StringBuilder("📍 Position: ");
                                if (data.getLeft()) positions.append("LEFT ");
                                if (data.getMiddle()) positions.append("MIDDLE ");
                                if (data.getRight()) positions.append("RIGHT");
                                addLog("Position", positions.toString());
                                
                                // Thực hiện chào hỏi nếu đủ điều kiện
                                performGreeting(data);
                                
                            } else if (data.getExist()) {
                                addLog("Detection", "📊 Object detected but too far: " + data.getDistance() + "m");
                            } else {
                                addLog("Scanning", "📡 Scanning for people...");
                            }
                            
                        } catch (Exception e) {
                            addLog("Error", "❌ Failed to parse detection data: " + e.getMessage());
                        }
                    } else {
                        addLog("Scanning", "📡 Person detection active...");
                    }
                }

                @Override
                public void onSuccess(String requestId, BaseResp<String> result) {
                    onSuccess(result);
                }

                @Override
                public void onFail(ApiError error) {
                    addLog("Error", "❌ Person detection failed: " + error.toString());
                }
            });
            
            isObjectPerceptionSubscribed = true;
            addLog("Subscribe", "✅ Person detection system active");

        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Native library error: " + e.getMessage());
            addLog("Error", "🚨 Please run on actual robot hardware with proper libraries");
            cbObjectPerception.setChecked(false);
            isSDKInitialized = false;
        } catch (Exception e) {
            addLog("Error", "❌ Failed to start person detection: " + e.getMessage());
            cbObjectPerception.setChecked(false);
        }
    }

    private void unsubscribeObjectPerception() {
        try {
            addLog("Unsubscribe", "🔄 Stopping person detection system...");
            
            // Sử dụng DynamicObjectApi.cancel() theo decompiled code
            if (dynamicObjectApi != null) {
                dynamicObjectApi.cancel();
                isObjectPerceptionSubscribed = false;
                addLog("Unsubscribe", "✅ Person detection system stopped");
            }
            
        } catch (Exception e) {
            addLog("Error", "❌ Failed to stop person detection: " + e.getMessage());
        }
    }

    /**
     * Thực hiện chào hỏi khi phát hiện người
     */
    private void performGreeting(DynamicObjectBean detectionData) {
        if (!autoGreetingEnabled) {
            addLog("Greeting", "🚫 Auto greeting disabled");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGreetingTime < GREETING_COOLDOWN) {
            addLog("Greeting", "⏳ Greeting cooldown active (" + 
                   ((GREETING_COOLDOWN - (currentTime - lastGreetingTime)) / 1000) + "s remaining)");
            return;
        }

        try {
            addLog("Greeting", "🤖 Performing greeting gesture...");
            
            // Thực hiện gật đầu chào
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PC);
            
            lastGreetingTime = currentTime;
            addLog("Greeting", "✅ Nod gesture completed! Welcome human friend! 👋");
            addLog("Greeting", "🎯 Detected at: " + detectionData.getDistance() + "m");
            
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head motor native library not found: " + e.getMessage());
            addLog("Error", "🚨 Robot hardware not available for head movement");
        } catch (Exception e) {
            addLog("Error", "❌ Greeting gesture failed: " + e.getMessage());
        }
    }

    /**
     * Toggle auto greeting on/off
     */
    public void toggleAutoGreeting() {
        autoGreetingEnabled = !autoGreetingEnabled;
        addLog("Setting", "🎛️ Auto greeting: " + (autoGreetingEnabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Test gật đầu thủ công
     */
    private void performManualNod() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Cannot perform manual nod.");
            return;
        }

        try {
            addLog("Manual", "🎯 Manual nod test...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PC);
            addLog("Manual", "✅ Manual nod completed!");
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head motor native library not found: " + e.getMessage());
            addLog("Error", "🚨 Robot hardware not available");
        } catch (Exception e) {
            addLog("Error", "❌ Manual nod failed: " + e.getMessage());
        }
    }

    private void addLog(String category, String message) {
        runOnUiThread(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
            String logEntry = String.format("[%s] %s: %s\n", timestamp, category, message);
            logBuilder.append(logEntry);
            tvApiLog.setText(logBuilder.toString());
            svApiLog.post(() -> svApiLog.fullScroll(ScrollView.FOCUS_DOWN));

            // Màu sắc cho log theo category
            switch (category.toLowerCase()) {
                case "error":
                    tvApiLog.setTextColor(Color.RED);
                    break;
                case "subscribe":
                case "detection":
                case "greeting":
                case "sdk":
                case "runtime":
                case "api":
                    tvApiLog.setTextColor(Color.GREEN);
                    break;
                case "unsubscribe":
                case "manual":
                case "warning":
                    tvApiLog.setTextColor(Color.YELLOW);
                    break;
                case "scanning":
                    tvApiLog.setTextColor(Color.CYAN);
                    break;
                case "position":
                case "info":
                case "status":
                case "setting":
                    tvApiLog.setTextColor(Color.MAGENTA);
                    break;
                default:
                    tvApiLog.setTextColor(Color.WHITE);
                    break;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check SDK status when returning to this activity
        if (!isSDKInitialized) {
            addLog("Resume", "🔄 Rechecking SDK status...");
            checkSDKStatus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isObjectPerceptionSubscribed) {
            unsubscribeObjectPerception();
        }
        addLog("System", "🛑 Smart Greeting Robot activity stopped (SDK preserved)");
        // NOTE: Do NOT release SDK - it's shared across activities
    }
}
