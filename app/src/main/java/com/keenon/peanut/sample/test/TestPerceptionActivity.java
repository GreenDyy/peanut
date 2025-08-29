package com.keenon.peanut.sample.test;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.keenon.peanut.common.config.ConfigManager;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.robot.ApiCallback;
import com.keenon.sdk.robot.base.ApiTopic;
import com.keenon.sdk.robot.base.BaseResp;
import com.keenon.sdk.robot.model.bean.sensor.ObjectPerceptionBean;

public class TestPerceptionActivity extends BaseActivity {
    private Button startDetectButton, stopDetectButton;
    private TextView logTextView;
    private Button turnLeftButton;
    private Gson gson = new Gson();
    private boolean hasDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_perception);
        startDetectButton = findViewById(R.id.btn_start_detect);
        stopDetectButton = findViewById(R.id.btn_stop_detect);
        logTextView = findViewById(R.id.tv_log);
        setButtonBack();
        setupListeners();

        PeanutSDK.getInstance().init(this, new PeanutSDK.ErrorListener() {
            @Override
            public void onInit(int statusCode) {
                if (statusCode == PeanutSDK.SDK_INIT_SUCCESS) {
                    initRuntime();
                    addLog("SDK", "✅ PeanutSDK initialized successfully.");
                } else {
                    addLog("SDK", "❌ PeanutSDK initialization failed with status: " + statusCode);
                }
            }
        });
    }

    private void initRuntime() {
        try {
            if (PeanutRuntime.getInstance() != null) {
                // Thêm các bước khởi tạo runtime nếu cần
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

    private void setWelcomeSwitch(boolean isOpen) {
        PeanutSDK.getInstance().runtime().setWelcomeSwitch(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onFail(ApiError apiError) {
                addLog("API_FAIL", "Lỗi khi thay đổi chế độ chào mừng: " + apiError.toString());
            }

            @Override
            public void onSuccess(BaseResp<String> stringBaseResp) {
                addLog("API_RAW_SUCCESS", isOpen ? "Mở " : "Tắt " + "Raw Response: " + gson.toJson(stringBaseResp));
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

                for (int i = 0; i < bean.getObjects().size(); i++) {
                    ObjectPerceptionBean.ObjectsBean obj = bean.getObjects().get(i);
                    addLog("OBJECT_" + (i + 1),
                            "Khoảng cách=" + obj.getDistance() + ", X=" + obj.getX() + ", Y=" + obj.getY());
                }
            } else {
                addLog("DETECTION", "Không có đối tượng nào được phát hiện.");
            }
        } catch (Exception e) {
            addLog("JSON_ERROR", "Không thể parse dữ liệu: " + e.getMessage());
        }
    }

    private void subscribePerceptionEvent() {
        addLog("SUBSCRIBE", "Đang đăng ký lắng nghe sự kiện Object Perception...");

        ApiCallback<String> commonCallback = new ApiCallback<String>() {

            @Override
            public void onSuccess(String requestId, String result) {
                // Handle the case when onSuccess is called with requestId and result
                if (result != null) {
                    handleObjectPerception(result);
                }
            }

            @Override
            public void onFail(ApiError error) {
                addLog("SUBSCRIBE_ERROR", "Error: " + error.toString());
            }

            @Override
            public void onSuccess(String s) {

            }
        };

        PeanutSDK.getInstance().subscribe(ApiTopic.OBJECT_PERCEPTION, 1000, commonCallback);
        PeanutSDK.getInstance().subscribe(ApiTopic.DYNAMIC_OBJECT_PERCEPTION, 1000, commonCallback);
        PeanutSDK.getInstance().subscribe(ApiTopic.NAVI_HUMAN_DETECTION_STATUS, 1000, commonCallback);
    }

    private void unsubscribePerceptionEvent() {
        addLog("UNSUBSCRIBE", "Đang hủy đăng ký lắng nghe sự kiện Object Perception...");
        PeanutSDK.getInstance().unSubscribe(ApiTopic.OBJECT_PERCEPTION, null);
        PeanutSDK.getInstance().unSubscribe(ApiTopic.DYNAMIC_OBJECT_PERCEPTION, null);
        PeanutSDK.getInstance().unSubscribe(ApiTopic.NAVI_HUMAN_DETECTION_STATUS, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribePerceptionEvent();
        setWelcomeSwitch(false);
    }
}
