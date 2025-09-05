package com.keenon.peanut.sample.test;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.peanut.sample.util.PeanutSDKManager;
import com.keenon.sdk.constant.ApiConstants;
import com.keenon.sdk.constant.RobotTopic;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.robot.ApiCallback;
import com.keenon.sdk.robot.ApiProgressCallback;
import com.keenon.sdk.robot.base.BaseResp;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.robot.model.bean.system.IPBean;
import com.keenon.sdk.robot.model.bean.system.OdoBean;
import com.keenon.sdk.robot.model.bean.device.MotorStatusBean;

import com.keenon.common.external.PeanutConfig;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.sdk.coapapi.api.sensor.ObjectPerceptionApi;
import com.keenon.sdk.robot.model.bean.sensor.ObjectPerceptionBean;
import com.keenon.sdk.robot.model.bean.battery.BatteryInfoBean;
import com.keenon.sdk.robot.model.bean.battery.ChargeStatusBean;
import com.keenon.sdk.sensor.headmotor.HeadMotorBean;
import com.keenon.sdk.sensor.headmotor.HeadMotorStateResult;
import com.keenon.sdk.sensor.headmotor.PlayHeadMotorParam;
import com.keenon.sdk.sensor.headmotor.PlayHeadMotorParam2;
import com.keenon.sdk.sensor.headmotor.HeadMotorInterface;
import com.keenon.sdk.sensor.headmotor.SensorHeadMotor;
import com.keenon.sdk.embedded.common.SensorEvent;
import com.keenon.sdk.embedded.common.SensorObserver;
import com.keenon.sdk.embedded.common.Event;
import com.keenon.sdk.embedded.common.Sensor;
import android.util.Log;
import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RobotKeenonActivity extends BaseActivity {

    @BindView(R.id.tv_log)
    TextView tvLog;
    @BindView(R.id.sv_log)
    ScrollView svLog;

    private SpannableStringBuilder logBuilder = new SpannableStringBuilder();
    private boolean isSDKInitialized = false;
    private ObjectPerceptionApi objectPerceptionApi;

    // Runtime listener để nhận events
    private PeanutRuntime.Listener runtimeListener = new PeanutRuntime.Listener() {
        @Override
        public void onEvent(int event, Object obj) {
            addLog("Runtime Event", "Event: " + event + ", Data: " + (obj != null ? obj.toString() : "null"));

            // Kiểm tra nếu là head motor event - có thể events được gửi qua đây
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
        setContentView(R.layout.activity_robot_keenon);
        ButterKnife.bind(this);
        setButtonBack();

        // Khởi tạo APIs
        objectPerceptionApi = new ObjectPerceptionApi();
        // Khởi tạo SDK sử dụng PeanutSDKManager
        // Kiểm tra xem SDK đã được khởi tạo chưa, nếu rồi thì thôi, chưa thì mới init
        if (PeanutSDKManager.isInitialized() && PeanutSDKManager.isSDKAvailable()) {
            addLog("SDK", "ℹ️ PeanutSDK đã được khởi tạo trước đó");
            isSDKInitialized = true; // Gán lại biến khi SDK đã sẵn sàng
            initRuntime();
        } else {
            // Sử dụng PeanutSDKManager để khởi tạo SDK
            PeanutSDKManager.initializeSDK(this,  new PeanutSDK.ErrorListener() {
                @Override
                public void onInit(int statusCode) {
                    switch (statusCode) {
                        case PeanutSDK.SDK_INIT_SUCCESS:
                            addLog("SDK", "✅ PeanutSDK khởi tạo thành công");
                            isSDKInitialized = true; // Gán lại biến khi SDK khởi tạo thành công
                            initRuntime();
                            break;

                        case PeanutSDK.SDK_INITIALIZING:
                            addLog("SDK", "⏳ PeanutSDK đang trong quá trình khởi tạo...");
                            break;

                        default:
                            addLog("SDK", "❌ PeanutSDK khởi tạo thất bại với mã: " + statusCode);
                            break;
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup runtime listener only - DO NOT release SDK
        if (PeanutRuntime.getInstance() != null) {
            PeanutRuntime.getInstance().removeListener(runtimeListener);
        }

        // Cleanup Head Motor - UnMount and clear observers
        try {
            SensorHeadMotor.getInstance().clearObserver();
            SensorHeadMotor.getInstance().unMount();
            addLog("Cleanup", "🤖 Head Motor unmounted and observers cleared");
        } catch (Exception e) {
            addLog("Cleanup", "⚠️ Head Motor cleanup warning: " + e.getMessage());
        }

        // NOTE: Do NOT release SDK here as it's shared across activities
        // SDK should only be released when the entire app is closing
        addLog("Cleanup", "🧹 Activity cleanup completed (SDK preserved)");
    }

    private void initRuntime() {
        try {
            if (PeanutRuntime.getInstance() != null) {
                // KHÔNG gọi start() vì đã được gọi từ KeenonApiDemoMain
                // Chỉ cần add listener để nhận events
                PeanutRuntime.getInstance().registerListener(runtimeListener);
                addLog("Runtime", "✅ Runtime listener added (Runtime already started from Main)");
            } else {
                addLog("Runtime", "❌ PeanutRuntime not available");
            }

            // Head Motor setup - MOUNT sensor trước khi sử dụng
            try {
                SensorHeadMotor.getInstance().mount();
                addLog("Head Motor", "✅ Head Motor mounted successfully");

                // Add observer để nhận events
                SensorHeadMotor.getInstance().addObserver(new com.keenon.sdk.embedded.common.SensorObserver() {
                    @Override
                    public void onUpdate(com.keenon.sdk.embedded.common.Event event,
                            com.keenon.sdk.embedded.common.Sensor sensor) {
                        addLog("Head Motor Event", "🎯 Event: " + event.toString() + " from " + sensor.name());
                    }
                });
                addLog("Head Motor", "✅ Head Motor observer added");
            } catch (Exception e) {
                addLog("Error", "❌ Head Motor mount failed: " + e.getMessage());
            }

            // Head Motor events sẽ được nhận qua cả Runtime Listener VÀ Sensor Observer
            addLog("Head Motor", "✅ Head Motor setup completed");

        } catch (Exception e) {
            addLog("Error", "❌ Runtime initialization failed: " + e.getMessage());
        }
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

            // Set text and scroll
            tvLog.setText(logBuilder);
            svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN));
        });
    }

    private int getCategoryColor(String category) {
        switch (category.toLowerCase()) {
            case "error":
                return Color.RED;
            case "robot info":
            case "components":
            case "initialization":
            case "runtime":
                return Color.GREEN;
            case "battery":
            case "battery info":
            case "battery details":
            case "power level":
            case "charge":
            case "charge status":
            case "auto charge":
            case "charge details":
                return Color.CYAN;
            case "head motor":
            case "head motor event":
            case "movement":
            case "motor":
            case "motor health":
                return Color.MAGENTA;
            case "perception":
            case "objects":
            case "object detection":
            case "location":
                return Color.BLUE;
            case "work mode":
            case "work mode details":
            case "work mode info":
                return 0xFF9C27B0; // Purple
            case "door":
            case "map":
                return 0xFFFFA500; // Orange
            case "debug":
                return Color.GRAY;
            case "cleanup":
                return Color.YELLOW;
            case "health":
            case "heartbeat":
            case "runtime event":
                return 0xFF90EE90; // Light Green
            default:
                return Color.WHITE;
        }
    }

    // =========================== SYSTEM INFO ===========================
    @OnClick(R.id.btn_robot_info)
    public void onRobotInfoClicked() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Please wait for initialization to complete.");
            return;
        }

        try {
            addLog("Robot Info", "🔍 Collecting robot information...");

            // 1. Try to get IP address from runtime
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().runtime() != null) {
                addLog("Robot Info", "📡 Requesting IP address from runtime...");
                PeanutSDK.getInstance().runtime().getIPAddress(new ApiCallback<BaseResp<IPBean>>() {
                    @Override
                    public void onSuccess(BaseResp<IPBean> result) {
                        if (result != null && result.getData() != null) {
                            IPBean ipData = result.getData();
                            addLog("Robot Info", "✅ IP Address: " + ipData.toString());
                        } else {
                            addLog("Robot Info", "⚠️ IP request succeeded but no data received");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<IPBean> result) {
                        addLog("Robot Info", "📡 IP request ID: " + requestId);
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ IP request failed: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Runtime component not available");
            }

            // 2. Get basic SDK info as fallback
            getRobotBasicInfo();

        } catch (Exception e) {
            addLog("Error", "❌ Failed to get robot info: " + e.getMessage());
            addLog("Debug", "Exception type: " + e.getClass().getSimpleName());
        }
    }

    private void getRobotBasicInfo() {
        try {
            addLog("Robot Info", "📋 Basic robot information:");

            // 1. SDK instance info
            if (PeanutSDK.getInstance() != null) {
                addLog("Robot Info", "✅ SDK Instance: " + PeanutSDK.getInstance().getClass().getSimpleName());
            } else {
                addLog("Robot Info", "❌ SDK Instance: Not available");
                return; // No point checking further if SDK is null
            }

            // 2. Runtime info
            if (PeanutRuntime.getInstance() != null) {
                addLog("Robot Info", "✅ Runtime Instance: Available");

                // Get runtime info details safely
                getRuntimeDetails();

            } else {
                addLog("Robot Info", "❌ Runtime not available");
            }

            // 3. SDK components availability
            checkSDKComponents();

            // 4. Additional system info
            getSystemInfo();

        } catch (Exception e) {
            addLog("Error", "❌ Basic info collection failed: " + e.getMessage());
        }
    }

    private void getRuntimeDetails() {
        try {
            if (PeanutRuntime.getInstance().getRuntimeInfo() != null) {
                addLog("Robot Info", "📊 Runtime Details:");

                // Power level
                try {
                    int power = PeanutRuntime.getInstance().getRuntimeInfo().getPower();
                    addLog("Robot Info", "🔋 Power Level: " + power + "%");
                } catch (Exception e) {
                    addLog("Robot Info", "🔋 Power Level: Not accessible");
                }

                // Destination info
                try {
                    String destList = PeanutRuntime.getInstance().getRuntimeInfo().getDestList();
                    addLog("Robot Info",
                            "📍 Destination List: " + (destList != null && !destList.isEmpty() ? destList : "Empty"));
                } catch (Exception e) {
                    addLog("Robot Info", "📍 Destination List: Not accessible - " + e.getMessage());
                }

                // Robot sync status (from decompiled code)
                try {
                    int syncStatus = PeanutRuntime.getInstance().getRuntimeInfo().getSyncStatus();
                    String statusDescription = getSyncStatusDescription(syncStatus);
                    addLog("Robot Info", "🚀 Sync Status: " + syncStatus + " (" + statusDescription + ")");
                } catch (Exception e) {
                    addLog("Robot Info", "🚀 Sync Status: Not accessible - " + e.getMessage());
                }

                // Work mode
                try {
                    int workMode = PeanutRuntime.getInstance().getRuntimeInfo().getWorkMode();
                    addLog("Robot Info", "⚙️ Work Mode: " + workMode);
                } catch (Exception e) {
                    addLog("Robot Info", "⚙️ Work Mode: Not accessible - " + e.getMessage());
                }

                // Motor status
                try {
                    int motorStatus = PeanutRuntime.getInstance().getRuntimeInfo().getMotorStatus();
                    String motorDescription = getMotorStatusDescription(motorStatus);
                    addLog("Robot Info", "🔧 Motor Status: " + motorStatus + " (" + motorDescription + ")");
                } catch (Exception e) {
                    addLog("Robot Info", "🔧 Motor Status: Not accessible - " + e.getMessage());
                }

                // Emergency status
                try {
                    boolean emergencyOpen = PeanutRuntime.getInstance().getRuntimeInfo().isEmergencyOpen();
                    addLog("Robot Info", "🚨 Emergency: " + (emergencyOpen ? "ACTIVE" : "Normal"));
                } catch (Exception e) {
                    addLog("Robot Info", "🚨 Emergency: Not accessible - " + e.getMessage());
                }

                // Robot IP (confirmed exists)
                try {
                    String robotIp = PeanutRuntime.getInstance().getRuntimeInfo().getRobotIp();
                    addLog("Robot Info",
                            "🌐 Robot IP: " + (robotIp != null && !robotIp.isEmpty() ? robotIp : "Not set"));
                } catch (Exception e) {
                    addLog("Robot Info", "🌐 Robot IP: Not accessible - " + e.getMessage());
                }

                // Total ODO (confirmed exists)
                try {
                    Double totalOdo = PeanutRuntime.getInstance().getRuntimeInfo().getTotalOdo();
                    addLog("Robot Info", "🛣️ Total ODO: " + (totalOdo != null ? totalOdo + " units" : "Not set"));
                } catch (Exception e) {
                    addLog("Robot Info", "🛣️ Total ODO: Not accessible - " + e.getMessage());
                }

                // Robot Arm Info (newly discovered)
                try {
                    String robotArmInfo = PeanutRuntime.getInstance().getRuntimeInfo().getRobotArmInfo();
                    addLog("Robot Info", "🦾 Robot Arm: "
                            + (robotArmInfo != null && !robotArmInfo.isEmpty() ? robotArmInfo : "Not set"));
                } catch (Exception e) {
                    addLog("Robot Info", "🦾 Robot Arm: Not accessible - " + e.getMessage());
                }

                // Robot Properties (newly discovered)
                try {
                    String robotProperties = PeanutRuntime.getInstance().getRuntimeInfo().getRobotProperties();
                    addLog("Robot Info", "⚙️ Properties: "
                            + (robotProperties != null && !robotProperties.isEmpty() ? robotProperties : "Not set"));
                } catch (Exception e) {
                    addLog("Robot Info", "⚙️ Properties: Not accessible - " + e.getMessage());
                }

                // STM32 Info (newly discovered)
                try {
                    String stm32Info = PeanutRuntime.getInstance().getRuntimeInfo().getRobotStm32Info();
                    addLog("Robot Info",
                            "🔧 STM32: " + (stm32Info != null && !stm32Info.isEmpty() ? stm32Info : "Not set"));
                } catch (Exception e) {
                    addLog("Robot Info", "🔧 STM32: Not accessible - " + e.getMessage());
                }

                // Emergency Enable status (newly discovered)
                try {
                    boolean emergencyEnabled = PeanutRuntime.getInstance().getRuntimeInfo().isEmergencyEnable();
                    addLog("Robot Info", "🛡️ Emergency Enable: " + (emergencyEnabled ? "YES" : "NO"));
                } catch (Exception e) {
                    addLog("Robot Info", "🛡️ Emergency Enable: Not accessible - " + e.getMessage());
                }

            } else {
                addLog("Robot Info", "⚠️ Runtime info object not available");
            }
        } catch (Exception e) {
            addLog("Robot Info", "❌ Runtime details error: " + e.getMessage());
        }
    }

    private String getSyncStatusDescription(int status) {
        // From decompiled code: robot reboot status tracking
        switch (status) {
            case -1:
                return "Not initialized";
            case 0:
                return "Startup";
            case 1:
                return "Initializing";
            case 2:
                return "Ready";
            default:
                return "Unknown (" + status + ")";
        }
    }

    private String getMotorStatusDescription(int status) {
        // Motor status values (need to verify with actual API)
        switch (status) {
            case -1:
                return "Not initialized";
            case 0:
                return "Disabled";
            case 1:
                return "Enabled";
            case 2:
                return "Error";
            default:
                return "Unknown (" + status + ")";
        }
    }

    private void getSystemInfo() {
        try {
            addLog("Robot Info", "🔧 System Information:");

            // App info
            addLog("Robot Info", "📱 Package: " + getPackageName());

            // Thread info
            addLog("Robot Info", "🧵 Main Thread: "
                    + (android.os.Looper.myLooper() == android.os.Looper.getMainLooper() ? "Yes" : "No"));

            // Memory info
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024; // Convert to MB
            long totalMemory = runtime.totalMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            addLog("Robot Info",
                    "💾 Memory: " + (totalMemory - freeMemory) + "/" + totalMemory + " MB (Max: " + maxMemory + " MB)");

        } catch (Exception e) {
            addLog("Robot Info", "❌ System info error: " + e.getMessage());
        }
    }

    private void checkSDKComponents() {
        addLog("Robot Info", "🔧 SDK Components Status:");

        try {
            if (PeanutSDK.getInstance().battery() != null) {
                addLog("Components", "✅ Battery component available");
            } else {
                addLog("Components", "❌ Battery component not available");
            }
        } catch (Exception e) {
            addLog("Components", "❌ Battery component error: " + e.getMessage());
        }

        try {
            if (PeanutSDK.getInstance().motor() != null) {
                addLog("Components", "✅ Motor component available");
            } else {
                addLog("Components", "❌ Motor component not available");
            }
        } catch (Exception e) {
            addLog("Components", "❌ Motor component error: " + e.getMessage());
        }

        try {
            if (PeanutSDK.getInstance().door() != null) {
                addLog("Components", "✅ Door component available");
            } else {
                addLog("Components", "❌ Door component not available");
            }
        } catch (Exception e) {
            addLog("Components", "❌ Door component error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_battery_info)
    public void onBatteryInfoClicked() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Please wait for initialization to complete.");
            return;
        }

        try {
            addLog("Battery", "🔋 Getting battery information...");

            // Sử dụng BatteryComponent theo decompiled code
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().batteryInfo(new ApiCallback<BaseResp<BatteryInfoBean>>() {
                    @Override
                    public void onSuccess(BaseResp<BatteryInfoBean> result) {
                        if (result != null && result.getData() != null) {
                            BatteryInfoBean battery = result.getData();
                            addLog("Battery Info", "✅ Battery: " + battery.toString());
                            // Thêm thông tin chi tiết nếu có
                            try {
                                addLog("Battery Details", "🔋 Battery class: " + battery.getClass().getSimpleName());
                            } catch (Exception e) {
                                addLog("Battery Details", "🔋 Battery data received");
                            }
                        } else {
                            addLog("Battery Info", "⚠️ No battery data received");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<BatteryInfoBean> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Battery info failed: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Battery component not available");
            }

            // Cũng lấy power từ runtime như backup
            if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
                int powerLevel = PeanutRuntime.getInstance().getRuntimeInfo().getPower();
                addLog("Power Level", "🔋 Runtime Power: " + powerLevel + "%");
            }

        } catch (Exception e) {
            addLog("Error", "❌ Failed to get battery info: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_location_info)
    public void onLocationInfoClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().runtime() != null) {
                PeanutSDK.getInstance().runtime().getRobotPosition(
                        new ApiCallback<BaseResp<com.keenon.sdk.robot.model.bean.map.PositionInfoBean>>() {
                            @Override
                            public void onSuccess(
                                    BaseResp<com.keenon.sdk.robot.model.bean.map.PositionInfoBean> result) {
                                if (result != null && result.getData() != null) {
                                    addLog("Location", "Position: " + result.getData().toString());
                                } else {
                                    addLog("Location", "No position data");
                                }
                            }

                            @Override
                            public void onSuccess(String requestId,
                                    BaseResp<com.keenon.sdk.robot.model.bean.map.PositionInfoBean> result) {
                                onSuccess(result);
                            }

                            @Override
                            public void onFail(ApiError error) {
                                addLog("Error", "Failed to get position: " + error.toString());
                            }
                        });
            } else {
                addLog("Error", "Runtime not initialized");
            }
        } catch (Exception e) {
            addLog("Error", "Failed to get location: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_work_mode)
    public void onWorkModeClicked() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Please wait for initialization to complete.");
            return;
        }

        try {
            addLog("Work Mode", "🔍 Checking current work mode...");

            if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
                int workMode = PeanutRuntime.getInstance().getRuntimeInfo().getWorkMode();
                addLog("Work Mode", "✅ Current Work Mode: " + workMode);

            } else {
                addLog("Error", "❌ Runtime or RuntimeInfo not available");
            }

        } catch (Exception e) {
            addLog("Error", "❌ Failed to get work mode: " + e.getMessage());
            addLog("Debug", "Exception type: " + e.getClass().getSimpleName());
        }
    }

    // =========================== NAVIGATION ===========================
    @OnClick(R.id.btn_move_forward)
    public void onMoveForwardClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().forward(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Movement", "Forward: " + (result != null ? result.toString() : "Success"));
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Forward failed: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "Motor component not available");
            }
        } catch (Exception e) {
            addLog("Error", "Forward movement error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_move_backward)
    public void onMoveBackwardClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().backward(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Movement", "Backward: " + (result != null ? result.toString() : "Success"));
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Backward failed: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "Motor component not available");
            }
        } catch (Exception e) {
            addLog("Error", "Backward movement error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_turn_left)
    public void onTurnLeftClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().turnLeft(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Movement", "Turn Left: " + (result != null ? result.toString() : "Success"));
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Turn left failed: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "Motor component not available");
            }
        } catch (Exception e) {
            addLog("Error", "Turn left error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_turn_right)
    public void onTurnRightClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().turnRight(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Movement", "Turn Right: " + (result != null ? result.toString() : "Success"));
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Turn right failed: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "Motor component not available");
            }
        } catch (Exception e) {
            addLog("Error", "Turn right error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_stop_move)
    public void onStopMoveClicked() {
        addLog("Movement", "Stop movement - no specific stop API found");
        // SDK không có API stop riêng, cần kiểm tra thêm
    }

    // Loại bỏ các phương thức cũ không dùng API thực tế

    @OnClick(R.id.btn_navigate_to)
    public void onNavigateToClicked() {
        // Example navigation to a point
        addLog("🎯 Navigation", "Navigate to destination feature - implement with coordinates");
        // TODO: Implement actual navigation with PeanutSDK.getInstance().navigation()
    }

    @OnClick(R.id.btn_set_destination)
    public void onSetDestinationClicked() {
        try {
            if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
                String destList = PeanutRuntime.getInstance().getRuntimeInfo().getDestList();
                addLog("📍 Destinations", "Available: " + destList);
            } else {
                addLog("❌ Destinations", "Runtime not initialized");
            }
        } catch (Exception e) {
            addLog("❌ Error", "Failed to get destinations: " + e.getMessage());
        }
    }

    // =========================== VOICE & AUDIO ===========================
    @OnClick(R.id.btn_speak_text)
    public void onSpeakTextClicked() {
        // TTS component không có trong PeanutSDK.getInstance()
        addLog("Voice", "TTS not available in current SDK version");
    }

    @OnClick(R.id.btn_start_listen)
    public void onStartListenClicked() {
        // ASR component không có trong PeanutSDK.getInstance()
        addLog("Voice", "ASR not available in current SDK version");
    }

    @OnClick(R.id.btn_stop_listen)
    public void onStopListenClicked() {
        // ASR component không có trong PeanutSDK.getInstance()
        addLog("Voice", "ASR stop not available in current SDK version");
    }

    // =========================== PERCEPTION ===========================
    @OnClick(R.id.btn_face_detection)
    public void onFaceDetectionClicked() {
        // Perception component không có trong PeanutSDK.getInstance()
        addLog("Perception", "Face detection not available in current SDK version");
    }

    @OnClick(R.id.btn_object_detection)
    public void onObjectDetectionClicked() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Please wait for initialization to complete.");
            return;
        }

        try {
            addLog("Perception", "🎯 Starting object detection...");

            // Sử dụng ObjectPerceptionApi đúng theo screenshot
            objectPerceptionApi.send(new ApiCallback<BaseResp<ObjectPerceptionBean>>() {
                @Override
                public void onSuccess(BaseResp<ObjectPerceptionBean> result) {
                    if (result != null && result.getData() != null) {
                        ObjectPerceptionBean data = result.getData();
                        addLog("Object Detection", "✅ Object detected: " + data.toString());

                        // Log chi tiết về objects
                        if (data.getObjects() != null && !data.getObjects().isEmpty()) {
                            addLog("Objects", "📋 Found " + data.getObjects().size() + " objects");
                            for (int i = 0; i < data.getObjects().size(); i++) {
                                addLog("Object " + (i + 1), "📍 " + data.getObjects().get(i).toString());
                            }
                        } else {
                            addLog("Objects", "📡 No objects detected");
                        }
                    } else {
                        addLog("Object Detection", "📡 Scanning for objects...");
                    }
                }

                @Override
                public void onSuccess(String requestId, BaseResp<ObjectPerceptionBean> result) {
                    onSuccess(result);
                }

                @Override
                public void onFail(ApiError error) {
                    addLog("Error", "❌ Object detection failed: " + error.toString());
                }
            });

        } catch (Exception e) {
            addLog("Error", "❌ Object detection error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_distance_sensor)
    public void onDistanceSensorClicked() {
        // Sensor component không có trong PeanutSDK.getInstance()
        addLog("Sensor", "Distance sensor not available in current SDK version");
    }

    // =========================== HARDWARE CONTROL ===========================
    @OnClick(R.id.btn_door_open)
    public void onDoorOpenClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().door() != null) {
                PeanutSDK.getInstance().door().open(new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        addLog("Door", "Opened: " + result);
                    }

                    @Override
                    public void onSuccess(String requestId, String result) {
                        addLog("Door", "Opened (ID: " + requestId + "): " + result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Door open failed: " + error.toString());
                    }
                }, 1); // Door ID 1
                addLog("Door", "Opening door...");
            } else {
                addLog("Error", "Door service not available");
            }
        } catch (Exception e) {
            addLog("Error", "Door open error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_door_close)
    public void onDoorCloseClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().door() != null) {
                PeanutSDK.getInstance().door().close(new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        addLog("Door", "Closed: " + result);
                    }

                    @Override
                    public void onSuccess(String requestId, String result) {
                        addLog("Door", "Closed (ID: " + requestId + "): " + result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Door close failed: " + error.toString());
                    }
                }, 1); // Door ID 1
                addLog("Door", "Closing door...");
            } else {
                addLog("Error", "Door service not available");
            }
        } catch (Exception e) {
            addLog("Error", "Door close error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_light_control)
    public void onLightControlClicked() {
        // Light component không có trong PeanutSDK.getInstance()
        addLog("Light", "Light control not available in current SDK version");
    }

    // =========================== CHARGER ===========================
    @OnClick(R.id.btn_go_charge)
    public void onGoChargeClicked() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Please wait for initialization to complete.");
            return;
        }

        try {
            addLog("Charge", "🔌 Starting auto charge...");

            // Sử dụng BatteryComponent.autoCharge(int i, ApiCallback) theo decompiled code
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().autoCharge(1, new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        if (result != null && result.getData() != null) {
                            addLog("Auto Charge", "✅ Auto charge response: " + result.getData());
                        } else {
                            addLog("Auto Charge", "✅ Auto charge initiated");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Auto charge failed: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Battery component not available");
            }

        } catch (Exception e) {
            addLog("Error", "❌ Auto charge error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_charge_status)
    public void onChargeStatusClicked() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Please wait for initialization to complete.");
            return;
        }

        try {
            addLog("Charge Status", "🔍 Checking charge match times...");

            // Sử dụng
            // BatteryComponent.getChargeMatches(ApiCallback<BaseResp<ChargeStatusBean>>)
            // theo decompiled code
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().getChargeMatches(new ApiCallback<BaseResp<ChargeStatusBean>>() {
                    @Override
                    public void onSuccess(BaseResp<ChargeStatusBean> result) {
                        if (result != null && result.getData() != null) {
                            ChargeStatusBean status = result.getData();
                            addLog("Charge Status", "✅ Charge status: " + status.toString());
                            try {
                                addLog("Charge Details", "🔋 Status class: " + status.getClass().getSimpleName());
                            } catch (Exception e) {
                                addLog("Charge Details", "🔋 Charge status data received");
                            }
                        } else {
                            addLog("Charge Status", "⚠️ No charge status data");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<ChargeStatusBean> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Charge status failed: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Battery component not available");
            }

        } catch (Exception e) {
            addLog("Error", "❌ Charge status error: " + e.getMessage());
        }
    }

    // =========================== MAP ===========================
    @OnClick(R.id.btn_map_download)
    public void onMapDownloadClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().map() != null) {
                addLog("Map", "Map component available - implement specific map APIs");
            } else {
                addLog("Error", "Map service not available");
            }
        } catch (Exception e) {
            addLog("Error", "Map download error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_map_upload)
    public void onMapUploadClicked() {
        addLog("Map", "Map upload - implement specific map APIs");
    }

    @OnClick(R.id.btn_start_mapping)
    public void onStartMappingClicked() {
        addLog("Map", "Start mapping - implement specific map APIs");
    }

    // =========================== MOTOR CONTROL ===========================
    @OnClick(R.id.btn_motor_enable)
    public void onMotorEnableClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().enable(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Motor", "Enabled: " + (result != null ? result.toString() : "Success"));
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Motor enable failed: " + error.toString());
                    }
                }, 1); // Enable with parameter 1
                addLog("Motor", "Enabling motors...");
            } else {
                addLog("Error", "Motor service not available");
            }
        } catch (Exception e) {
            addLog("Error", "Motor enable error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_motor_disable)
    public void onMotorDisableClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().enable(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Motor", "Disabled: " + (result != null ? result.toString() : "Success"));
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Motor disable failed: " + error.toString());
                    }
                }, 0); // Disable with parameter 0
                addLog("Motor", "Disabling motors...");
            } else {
                addLog("Error", "Motor service not available");
            }
        } catch (Exception e) {
            addLog("Error", "Motor disable error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_motor_health)
    public void onMotorHealthClicked() {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().getStatus(new ApiCallback<BaseResp<MotorStatusBean>>() {
                    @Override
                    public void onSuccess(BaseResp<MotorStatusBean> result) {
                        if (result != null && result.getData() != null) {
                            addLog("Motor Health", "Status: " + result.getData().toString());
                        } else {
                            addLog("Motor Health", "No status data");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<MotorStatusBean> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "Motor health failed: " + error.toString());
                    }
                });
                addLog("Motor Health", "Checking motor health...");
            } else {
                addLog("Error", "Motor service not available");
            }
        } catch (Exception e) {
            addLog("Error", "Motor health error: " + e.getMessage());
        }
    }

    // =========================== HEAD MOTOR HELPER ===========================
    private boolean checkHeadMotorReady() {
        if (!isSDKInitialized) {
            addLog("Error", "❌ SDK not initialized. Please wait for initialization to complete.");
            return false;
        }

        try {
            if (SensorHeadMotor.getInstance() == null) {
                addLog("Error", "❌ Head Motor sensor not available");
                return false;
            }
            return true;
        } catch (Exception e) {
            addLog("Error", "❌ Head Motor check failed: " + e.getMessage());
            return false;
        }
    }

    // =========================== HEAD MOTOR CONTROL ===========================
    @OnClick(R.id.btn_head_up)
    public void onHeadUpClicked() {
        if (!checkHeadMotorReady())
            return;

        try {
            addLog("Head Motor", "🔄 Moving head up...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PA);
            addLog("Head Motor", "✅ Moving head up (PA - Thẳng đứng) - Command sent");
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head Motor native library not found: " + e.getMessage());
        } catch (Exception e) {
            addLog("Error", "❌ Head up error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_down)
    public void onHeadDownClicked() {
        if (!checkHeadMotorReady())
            return;

        try {
            addLog("Head Motor", "🔄 Moving head down...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PB);
            addLog("Head Motor", "✅ Moving head down (PB - Nghiêng xuống) - Command sent");
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head Motor native library not found: " + e.getMessage());
        } catch (Exception e) {
            addLog("Error", "❌ Head down error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_left)
    public void onHeadLeftClicked() {
        if (!checkHeadMotorReady())
            return;

        try {
            addLog("Head Motor", "🔄 Turning head left...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PH);
            addLog("Head Motor", "✅ Turning head left (PH - Quay trái) - Command sent");
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head Motor native library not found: " + e.getMessage());
        } catch (Exception e) {
            addLog("Error", "❌ Head left error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_right)
    public void onHeadRightClicked() {
        if (!checkHeadMotorReady())
            return;

        try {
            addLog("Head Motor", "🔄 Turning head right...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PI);
            addLog("Head Motor", "✅ Turning head right (PI - Quay phải) - Command sent");
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head Motor native library not found: " + e.getMessage());
        } catch (Exception e) {
            addLog("Error", "❌ Head right error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_center)
    public void onHeadCenterClicked() {
        if (!checkHeadMotorReady())
            return;

        try {
            addLog("Head Motor", "🔄 Centering head position...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.RESET);
            addLog("Head Motor", "✅ Centering head position (RESET - Hiệu chuẩn) - Command sent");
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head Motor native library not found: " + e.getMessage());
        } catch (Exception e) {
            addLog("Error", "❌ Head center error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_nod)
    public void onHeadNodClicked() {
        if (!checkHeadMotorReady())
            return;

        try {
            addLog("Head Motor", "🔄 Performing nod gesture...");
            SensorHeadMotor.getInstance().onControlHeadMotorPlay(HeadMotorInterface.MotorAction.PC);
            addLog("Head Motor", "✅ Performing nod gesture (PC - Gật đầu) - Command sent");
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head Motor native library not found: " + e.getMessage());
        } catch (Exception e) {
            addLog("Error", "❌ Head nod error: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_head_stop)
    public void onHeadStopClicked() {
        if (!checkHeadMotorReady())
            return;

        try {
            addLog("Head Motor", "🔍 Getting head motor state...");
            SensorHeadMotor.getInstance().getHeadMotorState();
            addLog("Head Motor", "✅ Head motor state request sent - check events for result");
        } catch (UnsatisfiedLinkError e) {
            addLog("Error", "❌ Head Motor native library not found: " + e.getMessage());
        } catch (Exception e) {
            addLog("Error", "❌ Head stop error: " + e.getMessage());
        }
    }
}
