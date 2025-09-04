package com.keenon.peanut.sample.test;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.sensor.headmotor.SensorHeadMotor;
import com.keenon.sdk.sensor.headmotor.HeadMotorInterface;
import com.keenon.sdk.sensor.headmotor.HeadMotorBean;
import com.keenon.sdk.embedded.common.PeanutSensors;
import com.keenon.sdk.embedded.common.Sensor;
import com.keenon.sdk.embedded.common.SensorObserver;
import com.keenon.sdk.embedded.common.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HeadMotorControlActivity extends BaseActivity {
    private static final String TAG = HeadMotorControlActivity.class.getSimpleName();

    // UI Components
    @BindView(R.id.tv_sdk_status)
    TextView tvSdkStatus;
    @BindView(R.id.tv_head_motor_status)
    TextView tvHeadMotorStatus;
    @BindView(R.id.tv_topic_manager_status)
    TextView tvTopicManagerStatus;
    @BindView(R.id.tv_sensor_status)
    TextView tvSensorStatus;
    @BindView(R.id.tv_log)
    TextView tvLog;
    @BindView(R.id.sv_log)
    ScrollView svLog;
    @BindView(R.id.sv_main_content)
    ScrollView svMainContent;

    // Data
    private SpannableStringBuilder logBuilder = new SpannableStringBuilder();
    private boolean isSDKInitialized = false;
    private boolean isHeadMotorReady = false;
    private boolean isTopicManagerReady = false;
    private boolean isSensorReady = false;

    // Runtime listener để nhận events
    private PeanutRuntime.Listener runtimeListener = new PeanutRuntime.Listener() {
        @Override
        public void onEvent(int event, Object obj) {
            addLog("Runtime Event", "Event: " + event + ", Data: " + (obj != null ? obj.toString() : "null"));

            // Kiểm tra nếu là head motor event
            if (obj instanceof HeadMotorBean) {
                HeadMotorBean headData = (HeadMotorBean) obj;
                addLog("Head Motor Event", "🎯 Head Motor Bean - X: " + headData.getAngleX() +
                        ", Y: " + headData.getAngleY() + ", State: " + headData.getState());
            }

            // Kiểm tra nếu obj là String chứa head motor data
            if (obj instanceof String) {
                String data = (String) obj;
                if (data.contains("head") || data.contains("motor")) {
                    addLog("Head Motor String Event", "📝 Possible head motor data: " + data);
                }
            }
        }

        @Override
        public void onHealth(Object content) {
            addLog("Health", "Status: " + content);
        }

        @Override
        public void onHeartbeat(Object content) {
            addLog("Heartbeat", "Data: " + content);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_motor_control);
        ButterKnife.bind(this);
        setButtonBack();

        // Khởi tạo hệ thống
        initializeSystem();
        
        // Thiết lập scroll cho log panel
        setupScrollView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup runtime listener
        if (PeanutRuntime.getInstance() != null) {
            PeanutRuntime.getInstance().removeListener(runtimeListener);
        }

        // Cleanup Head Motor
        try {
            SensorHeadMotor.getInstance().clearObserver();
            SensorHeadMotor.getInstance().unMount();
            addLog("Cleanup", "🤖 Head Motor unmounted and observers cleared");
        } catch (Exception e) {
            addLog("Cleanup", "⚠️ Head Motor cleanup warning: " + e.getMessage());
        }

        addLog("Cleanup", "🧹 Activity cleanup completed");
    }

    private void initializeSystem() {
        addLog("System", "🚀 Initializing Head Motor Control System...");

        // Kiểm tra SDK status
        checkSDKStatus();
        
        // Kiểm tra Head Motor status
        checkHeadMotorStatus();
        
        // Kiểm tra Topic Manager status
        checkTopicManagerStatus();
        
        // Kiểm tra Sensor status
        checkSensorStatus();

        // Khởi tạo Runtime listener
        initRuntimeListener();

        addLog("System", "✅ System initialization completed");
    }

    private void setupScrollView() {
        // Thiết lập main content scroll (toàn bộ màn hình)
        svMainContent.setVerticalScrollBarEnabled(true);
        svMainContent.setHorizontalScrollBarEnabled(false);
        svMainContent.setSmoothScrollingEnabled(true);
        svMainContent.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        
        // Thiết lập log panel scroll (cố định ở dưới)
        svLog.setVerticalScrollBarEnabled(true);
        svLog.setHorizontalScrollBarEnabled(false);
        svLog.setSmoothScrollingEnabled(true);
        
        // Thiết lập scroll listener cho main content
        svMainContent.getViewTreeObserver().addOnScrollChangedListener(() -> {
            // Có thể thêm logic xử lý scroll events ở đây nếu cần
        });
        
        // Thiết lập scroll listener cho log panel
        svLog.getViewTreeObserver().addOnScrollChangedListener(() -> {
            // Có thể thêm logic xử lý scroll events ở đây nếu cần
        });
        
        addLog("System", "🎯 Scroll system initialized - Main content scrollable, Log panel fixed");
    }

    private void checkSDKStatus() {
        try {
            if (PeanutSDK.getInstance() != null) {
                isSDKInitialized = true;
                updateStatusUI(tvSdkStatus, "SDK: ✅", Color.GREEN);
                addLog("SDK", "✅ PeanutSDK available");
            } else {
                isSDKInitialized = false;
                updateStatusUI(tvSdkStatus, "SDK: ❌", Color.RED);
                addLog("SDK", "❌ PeanutSDK not available");
            }
        } catch (Exception e) {
            isSDKInitialized = false;
            updateStatusUI(tvSdkStatus, "SDK: ❌", Color.RED);
            addLog("SDK", "❌ SDK check failed: " + e.getMessage());
        }
    }

    private void checkHeadMotorStatus() {
        try {
            if (SensorHeadMotor.getInstance() != null) {
                isHeadMotorReady = true;
                updateStatusUI(tvHeadMotorStatus, "Head Motor: ✅", Color.GREEN);
                addLog("Head Motor", "✅ SensorHeadMotor available");
            } else {
                isHeadMotorReady = false;
                updateStatusUI(tvHeadMotorStatus, "Head Motor: ❌", Color.RED);
                addLog("Head Motor", "❌ SensorHeadMotor not available");
            }
        } catch (Exception e) {
            isHeadMotorReady = false;
            updateStatusUI(tvHeadMotorStatus, "Head Motor: ❌", Color.RED);
            addLog("Head Motor", "❌ Head Motor check failed: " + e.getMessage());
        }
    }

    private void checkTopicManagerStatus() {
        try {
            // Kiểm tra TopicManager availability
            isTopicManagerReady = true;
            updateStatusUI(tvTopicManagerStatus, "Topic Manager: ✅", Color.GREEN);
            addLog("Topic Manager", "✅ TopicManager available");
        } catch (Exception e) {
            isTopicManagerReady = false;
            updateStatusUI(tvTopicManagerStatus, "Topic Manager: ❌", Color.RED);
            addLog("Topic Manager", "❌ TopicManager check failed: " + e.getMessage());
        }
    }

    private void checkSensorStatus() {
        try {
            if (PeanutSensors.getInstance() != null) {
                isSensorReady = true;
                updateStatusUI(tvSensorStatus, "Sensor: ✅", Color.GREEN);
                addLog("Sensor", "✅ PeanutSensors available");
            } else {
                isSensorReady = false;
                updateStatusUI(tvSensorStatus, "Sensor: ❌", Color.RED);
                addLog("Sensor", "❌ PeanutSensors not available");
            }
        } catch (Exception e) {
            isSensorReady = false;
            updateStatusUI(tvSensorStatus, "Sensor: ❌", Color.RED);
            addLog("Sensor", "❌ Sensor check failed: " + e.getMessage());
        }
    }

    private void updateStatusUI(TextView textView, String text, int color) {
        runOnUiThread(() -> {
            textView.setText(text);
            textView.setTextColor(color);
        });
    }

    private void initRuntimeListener() {
        try {
            if (PeanutRuntime.getInstance() != null) {
                PeanutRuntime.getInstance().registerListener(runtimeListener);
                addLog("Runtime", "✅ Runtime listener registered");
            } else {
                addLog("Runtime", "❌ PeanutRuntime not available");
            }
        } catch (Exception e) {
            addLog("Runtime", "❌ Runtime listener registration failed: " + e.getMessage());
        }
    }

    // =========================== HEAD MOTOR CONTROL METHODS ===========================

    @OnClick(R.id.btn_head_up)
    public void onHeadUpClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Moving head up...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PA);
            addLog("Head Motor", "✅ Moving head up (PA - Thẳng đứng) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head up error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_down)
    public void onHeadDownClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Moving head down...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PB);
            addLog("Head Motor", "✅ Moving head down (PB - Nghiêng xuống) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head down error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_left)
    public void onHeadLeftClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Turning head left...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PH);
            addLog("Head Motor", "✅ Turning head left (PH - Quay trái) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head left error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_right)
    public void onHeadRightClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Turning head right...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PI);
            addLog("Head Motor", "✅ Turning head right (PI - Quay phải) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head right error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_center)
    public void onHeadCenterClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Centering head position...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.RESET);
            addLog("Head Motor", "✅ Centering head position (RESET - Hiệu chuẩn) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head center error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_nod)
    public void onHeadNodClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Performing nod gesture...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PC);
            addLog("Head Motor", "✅ Performing nod gesture (PC - Gật đầu) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head nod error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_shake)
    public void onHeadShakeClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Performing shake gesture...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PD);
            addLog("Head Motor", "✅ Performing shake gesture (PD - Lắc đầu) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head shake error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_look_around)
    public void onHeadLookAroundClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Looking around...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PE);
            addLog("Head Motor", "✅ Looking around (PE - Nhìn xung quanh) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head look around error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_turn)
    public void onHeadTurnClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Turning head...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PF);
            addLog("Head Motor", "✅ Turning head (PF - Quay) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head turn error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_tilt2)
    public void onHeadTilt2Clicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Tilting head (Tilt 2)...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PG);
            addLog("Head Motor", "✅ Tilting head (PG - Nghiêng 2) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head tilt2 error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_face_tracking)
    public void onHeadFaceTrackingClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Starting face tracking...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PJ);
            addLog("Head Motor", "✅ Face tracking started (PJ - Theo dõi khuôn mặt) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head face tracking error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_startup)
    public void onHeadStartupClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔄 Starting head motor startup sequence...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PZ);
            addLog("Head Motor", "✅ Startup sequence initiated (PZ - Khởi động) - Command sent");
        } catch (Exception e) {
            addLog("Error", "❌ Head startup error: " + e.getMessage());
        }
    }

    // =========================== CONTROL PANEL METHODS ===========================

    @OnClick(R.id.btn_get_state)
    public void onGetStateClicked() {
        if (!checkSystemReady()) return;
        
        try {
            addLog("Head Motor", "🔍 Getting head motor state...");
            SensorHeadMotor.getInstance().getHeadMotorState();
            addLog("Head Motor", "✅ Head motor state request sent - check events for result");
        } catch (Exception e) {
            addLog("Error", "❌ Get state error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_check_status)
    public void onCheckStatusClicked() {
        addLog("System", "🔍 Checking system status...");
        
        // Recheck all statuses
        checkSDKStatus();
        checkHeadMotorStatus();
        checkTopicManagerStatus();
        checkSensorStatus();
        
        addLog("System", "✅ Status check completed");
    }

    @OnClick(R.id.btn_reinit_head_motor)
    public void onReinitHeadMotorClicked() {
        try {
            addLog("Head Motor", "🔄 Reinitializing head motor...");
            
            // Cleanup existing head motor
            try {
                SensorHeadMotor.getInstance().release();
                addLog("Head Motor", "✅ Existing head motor released");
            } catch (Exception e) {
                addLog("Head Motor", "⚠️ Release warning: " + e.getMessage());
            }

            // Reinitialize
            checkHeadMotorStatus();
            addLog("Head Motor", "✅ Head motor reinitialization completed");
            
        } catch (Exception e) {
            addLog("Error", "❌ Head motor reinit error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_reinit_topic_manager)
    public void onReinitTopicManagerClicked() {
        try {
            addLog("Topic Manager", "🔄 Reinitializing TopicManager...");
            
            // Reinitialize TopicManager
            checkTopicManagerStatus();
            addLog("Topic Manager", "✅ TopicManager reinitialization completed");
            
        } catch (Exception e) {
            addLog("Error", "❌ TopicManager reinit error: " + e.getMessage());
        }
    }

    // =========================== DEBUG TOOLS METHODS ===========================

    @OnClick(R.id.btn_debug_sensors)
    public void onDebugSensorsClicked() {
        addLog("Debug", "🔧 Debugging sensors...");
        
        try {
            // Debug SDK status
            boolean sdkAvailable = PeanutSDK.getInstance() != null;
            addLog("Debug", "📱 SDK Available: " + sdkAvailable);
            
            // Debug runtime status
            boolean runtimeAvailable = PeanutRuntime.getInstance() != null;
            addLog("Debug", "🔄 Runtime Available: " + runtimeAvailable);
            
            // Debug head motor
            boolean headMotorAvailable = false;
            try {
                SensorHeadMotor.getInstance();
                headMotorAvailable = true;
            } catch (Exception e) {
                addLog("Debug", "⚠️ Head Motor not available: " + e.getMessage());
            }
            addLog("Debug", "🤖 Head Motor Available: " + headMotorAvailable);
            
            // Debug PeanutSensors
            boolean peanutSensorsAvailable = false;
            try {
                PeanutSensors.getInstance();
                peanutSensorsAvailable = true;
            } catch (Exception e) {
                addLog("Debug", "⚠️ PeanutSensors not available: " + e.getMessage());
            }
            addLog("Debug", "📡 PeanutSensors Available: " + peanutSensorsAvailable);
            
            addLog("Debug", "✅ Sensor debug completed");
            
        } catch (Exception e) {
            addLog("Error", "❌ Debug sensors error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_force_init)
    public void onForceInitClicked() {
        try {
            addLog("Force Init", "💪 Force initializing HeadMotor...");
            
            // Force khởi tạo lại headMotor
            try {
                SensorHeadMotor.getInstance();
                addLog("Force Init", "✅ HeadMotor force initialized successfully");
                
                // Update status
                checkHeadMotorStatus();
                
            } catch (Exception e) {
                addLog("Error", "❌ Failed to force initialize HeadMotor: " + e.getMessage());
            }
            
        } catch (Exception e) {
            addLog("Error", "❌ Force init error: " + e.getMessage());
        }
    }

    // =========================== UTILITY METHODS ===========================

    @OnClick(R.id.btn_clear_log)
    public void onClearLogClicked() {
        logBuilder.clear();
        tvLog.setText("🤖 Log cleared\n");
        addLog("System", "🗑️ Log cleared");
    }

    private boolean checkSystemReady() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Please wait for initialization to complete.");
            return false;
        }
        
        if (!isHeadMotorReady) {
            addLog("Error", "❌ Head Motor not ready. Please check system status.");
            return false;
        }
        
        return true;
    }

    private void addLog(String category, String message) {
        runOnUiThread(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = String.format("[%s] %s: %s\n", timestamp, category, message);

            // Get color for this category
            int color = getCategoryColor(category);

            // Create colored text
            int startPos = logBuilder.length();
            logBuilder.append(logEntry);
            int endPos = logBuilder.length();

            // Apply color to this specific log entry
            logBuilder.setSpan(new ForegroundColorSpan(color), startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set text và scroll xuống dưới cùng
            tvLog.setText(logBuilder);
            
            // Scroll xuống dưới cùng với animation mượt mà
            svLog.post(() -> {
                svLog.fullScroll(View.FOCUS_DOWN);
                // Hoặc sử dụng smooth scroll:
                // svLog.smoothScrollTo(0, tvLog.getHeight());
            });
        });
    }

    private int getCategoryColor(String category) {
        switch (category.toLowerCase()) {
            case "error":
                return Color.RED;
            case "system":
            case "head motor":
                return Color.GREEN;
            case "debug":
                return Color.CYAN;
            case "force init":
                return Color.MAGENTA;
            case "runtime":
            case "runtime event":
                return Color.YELLOW;
            case "cleanup":
                return 0xFFFFA500; // Orange
            default:
                return Color.WHITE;
        }
    }
}
