package com.keenon.peanut.sample.test;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.robot.ApiCallback;
import com.keenon.sdk.robot.SubscribeRespDispatcher;
import com.keenon.sdk.robot.base.BaseResp;
import com.keenon.sdk.robot.model.bean.sensor.ObjectPerceptionBean;
import com.google.gson.Gson;
//animr
import com.keenon.peanut.common.config.ConfigManager;

public class TestPerceptionActivity extends BaseActivity {
    private Button startDetectButton, stopDetectButton;
    private TextView logTextView;
    private Button turnLeftButton;
    private Gson gson = new Gson();
    private boolean hasDetected = false; // Biến cờ để kiểm soát việc quay
    private ApiCallback<String> perceptionCallback;

    // Đối tượng lắng nghe sự kiện từ robot
    private SubscribeRespDispatcher dispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_perception);
        startDetectButton = findViewById(R.id.btn_start_detect);
        stopDetectButton = findViewById(R.id.btn_stop_detect);
        logTextView = findViewById(R.id.tv_log);
        turnLeftButton = findViewById(R.id.btn_turn_left);
        turnLeftButton.setOnClickListener(v -> {
            addLog("ACTION", "⬅️ Đang ra lệnh robot quay sang trái...");
            handleTurnLeft();
        });

        setButtonBack();
        setupListeners();
        setupSubscribeDispatcher();

        // Bước 1: Khởi tạo PeanutSDK
        PeanutSDK.getInstance().init(this, new PeanutSDK.ErrorListener() {
            @Override
            public void onInit(int statusCode) {
                // Kiểm tra trạng thái khởi tạo
                if (statusCode == PeanutSDK.SDK_INIT_SUCCESS) {
                    // Bước 2: Khởi tạo các thành phần khác sau khi SDK đã sẵn sàng
                     initRuntime();
                    addLog("SDK", "✅ PeanutSDK initialized successfully.");
                } else {
                    addLog("SDK", "❌ PeanutSDK initialization failed with status: " + statusCode);
                }
            }
        });
    }

    // Phương thức bạn đã cung cấp, có thể đặt ở đây
    private void initRuntime() {
        try {
            if (PeanutRuntime.getInstance() != null) {
                // ... (Các dòng code khởi tạo Runtime, Head Motor, v.v.)
            } else {
                addLog("Runtime", "❌ PeanutRuntime not available");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Runtime initialization failed: " + e.getMessage());
        }
    }

    private void addLog(String tag, String message) {
        String timeStamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        runOnUiThread(() -> {
            logTextView.append("\n[" + timeStamp + "] " + tag + ": " + message);
        });
    }

    private void handleTurnLeft() {
        PeanutSDK.getInstance().motor().turnLeft(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onSuccess(BaseResp<String> result) {
                addLog("Movement", "Turn Left: " + (result != null ? result.toString() : "Success"));
            }

            @Override
            public void onSuccess(String requestId, BaseResp<String> result) {
                onSuccess(result); // reuse callback
            }

            @Override
            public void onFail(ApiError error) {
                addLog("Error", "Turn left failed: " + error.toString());
            }
        });
    }

    private void setupListeners() {
        startDetectButton.setOnClickListener(v -> {
            addLog("ACTION", "Đang bật chế độ phát hiện người...");
            setWelcomeSwitch(true);
        });

        stopDetectButton.setOnClickListener(v -> {
            addLog("ACTION", "Đang tắt chế độ phát hiện người...");
            setWelcomeSwitch(false);
        });
    }

    // Gửi lệnh bật/tắt chế độ chào mừng tới robot
    private void setWelcomeSwitch(boolean isOpen) {
        PeanutSDK.getInstance().runtime().setWelcomeSwitch(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onFail(ApiError apiError) {
                addLog("API_FAIL",  "Lỗi khi thay đổi chế độ chào mừng: " + apiError.toString());
            }

            @Override
            public void onSuccess(BaseResp<String> stringBaseResp) {
                addLog("API_RAW_SUCCESS",isOpen ? "Mở " : "Tắt "+ "Raw Response: " + gson.toJson(stringBaseResp));
                if (isOpen) {
                    subscribePerceptionEvent();
                } else {
                    unsubscribePerceptionEvent();
                }
            }

            @Override
            public void onSuccess(String topic, BaseResp<String> result) {
                addLog("API_SUCCESS", (isOpen ? "BẬT" : "TẮT") + " chế độ chào mừng thành công! topic=" + topic);
                if (isOpen) {
                    subscribePerceptionEvent();
                } else {
                    unsubscribePerceptionEvent();
                }
            }

            public void onFail(String topic, ApiError error) {
                addLog("API_TOPIC_FAIL", "Topic=" + topic + " | Error: " + error.toString());
            }
        }, isOpen);
    }

    private void handleObjectPerception(String data) {
        try {
            addLog("RAW_DATA", "Nhận dữ liệu từ sensor: " + data);
            ObjectPerceptionBean bean = gson.fromJson(data, ObjectPerceptionBean.class);

            if (bean != null && bean.getObjects() != null && !bean.getObjects().isEmpty()) {
                addLog("DETECTION", "Phát hiện " + bean.getObjects().size() + " đối tượng.");

                // Lặp qua các đối tượng đã phát hiện
                for (int i = 0; i < bean.getObjects().size(); i++) {
                    ObjectPerceptionBean.ObjectsBean obj = bean.getObjects().get(i);
                    addLog("OBJECT_" + (i + 1),
                            "Khoảng cách=" + obj.getDistance() + ", X=" + obj.getX() + ", Y=" + obj.getY());
                }

                // --- Bổ sung logic tại đây để ra lệnh robot quay phải ---
                if (!hasDetected) {
                    handleTurnRight();
                    hasDetected = true; // Đặt cờ để chỉ quay một lần
                }

            } else {
                addLog("DETECTION", "Không có đối tượng nào được phát hiện.");
                // Reset cờ khi không còn phát hiện đối tượng nữa
                hasDetected = false;
            }
        } catch (Exception e) {
            addLog("JSON_ERROR", "Không thể parse dữ liệu: " + e.getMessage());
        }
    }

    private void handleTurnRight() {
        addLog("ACTION", "➡️ Đang ra lệnh robot quay sang phải...");
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
    }

    // Đăng ký lắng nghe sự kiện phát hiện đối tượng
    private void subscribePerceptionEvent() {
        addLog("SUBSCRIBE", "Đang đăng ký lắng nghe sự kiện Object Perception...");
        // Hàm subscribe() để đăng ký lắng nghe các sự kiện từ robot
        PeanutSDK.getInstance().subscribe("sensor.objectPerception", 1000, null);
    }

    // Hủy đăng ký lắng nghe
    private void unsubscribePerceptionEvent() {
        addLog("UNSUBSCRIBE", "Đang hủy đăng ký lắng nghe sự kiện Object Perception...");
        PeanutSDK.getInstance().unSubscribe("sensor.objectPerception", null);
    }

    // Cài đặt bộ xử lý sự kiện
    private void setupSubscribeDispatcher() {
        addLog("setupSubscribeDispatcher", "Đang call setupSubscribeDispatcher");

        dispatcher = new SubscribeRespDispatcher() {
            @Override
            public void notifyApiError(String topic, ApiError error) {
                if ("sensor.objectPerception".equals(topic)) {
                    addLog("SUBSCRIBE_ERROR", "Lỗi lắng nghe " + topic + ": " + error.toString());
                }
            }

            @Override
            public void notifyApiSuccess(String topic, String data) {
                if ("sensor.objectPerception".equals(topic)) {
                    // Xử lý dữ liệu từ sensor tại đây
                    handleObjectPerception(data);
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký và tắt chế độ khi Activity bị hủy để tránh rò rỉ tài nguyên
        unsubscribePerceptionEvent();
        setWelcomeSwitch(false);
    }
}