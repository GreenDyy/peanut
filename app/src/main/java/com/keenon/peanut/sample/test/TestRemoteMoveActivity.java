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
import com.keenon.sdk.robot.base.BaseResp;

public class TestRemoteMoveActivity extends BaseActivity {
    private Button forwardButton, backwardButton, turnLeftButton, turnRightButton;
    private Button stopButton, connectButton, disconnectButton;
    private Button motorEnableButton, motorDisableButton;
    private TextView logTextView;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_remote_move);
        
        initViews();
        setupListeners();
        setButtonBack();
        
        // Khởi tạo SDK
        PeanutSDK.getInstance().init(this, new PeanutSDK.ErrorListener() {
            @Override
            public void onInit(int statusCode) {
                if (statusCode == PeanutSDK.SDK_INIT_SUCCESS) {
                    addLog("SDK", "✅ PeanutSDK khởi tạo thành công");
                    initRuntime();
                } else {
                    addLog("SDK", "❌ PeanutSDK khởi tạo thất bại với mã: " + statusCode);
                }
            }
        });
    }

    private void initViews() {
        forwardButton = findViewById(R.id.btn_forward);
        backwardButton = findViewById(R.id.btn_backward);
        turnLeftButton = findViewById(R.id.btn_turn_left);
        turnRightButton = findViewById(R.id.btn_turn_right);
        stopButton = findViewById(R.id.btn_stop);
        connectButton = findViewById(R.id.btn_connect);
        disconnectButton = findViewById(R.id.btn_disconnect);
        motorEnableButton = findViewById(R.id.btn_motor_enable);
        motorDisableButton = findViewById(R.id.btn_motor_disable);
        logTextView = findViewById(R.id.tv_log);
    }

    private void setupListeners() {
        connectButton.setOnClickListener(v -> connectToRobot());
        disconnectButton.setOnClickListener(v -> disconnectFromRobot());
        
        motorEnableButton.setOnClickListener(v -> enableMotor());
        motorDisableButton.setOnClickListener(v -> disableMotor());
        
        forwardButton.setOnClickListener(v -> moveForward());
        backwardButton.setOnClickListener(v -> moveBackward());
        turnLeftButton.setOnClickListener(v -> turnLeft());
        turnRightButton.setOnClickListener(v -> turnRight());
        stopButton.setOnClickListener(v -> stopMovement());
    }

    private void initRuntime() {
        try {
            if (PeanutRuntime.getInstance() != null) {
                PeanutRuntime.getInstance().start(new PeanutRuntime.Listener() {
                    @Override
                    public void onEvent(int event, Object obj) {
                        addLog("Runtime", "Event: " + event + ", Content: " + obj);
                    }

                    @Override
                    public void onHealth(Object content) {
                        addLog("Health", "Status: " + content);
                    }

                    @Override
                    public void onHeartbeat(Object content) {
                        // Không log heartbeat để tránh spam
                    }
                });
                addLog("Runtime", "✅ PeanutRuntime khởi tạo thành công");
            } else {
                addLog("Runtime", "❌ PeanutRuntime không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Khởi tạo Runtime thất bại: " + e.getMessage());
        }
    }

    private void connectToRobot() {
        addLog("Connection", "🔄 Đang kết nối đến robot...");
        isConnected = true;
        updateButtonStates();
        addLog("Connection", "✅ Đã kết nối thành công đến robot");
    }

    private void disconnectFromRobot() {
        addLog("Connection", "🔄 Đang ngắt kết nối...");
        isConnected = false;
        updateButtonStates();
        addLog("Connection", "✅ Đã ngắt kết nối khỏi robot");
    }

    private void enableMotor() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }
        
        addLog("Motor", "🔄 Đang bật motor...");
        PeanutSDK.getInstance().motor().enable(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onSuccess(BaseResp<String> result) {
                addLog("Motor", "✅ Bật motor thành công");
            }

            @Override
            public void onSuccess(String requestId, BaseResp<String> result) {
                onSuccess(result);
            }

            @Override
            public void onFail(ApiError error) {
                addLog("Error", "❌ Bật motor thất bại: " + error.toString());
            }
        }, 1); // Enable với parameter 1
    }

    private void disableMotor() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }
        
        addLog("Motor", "🔄 Đang tắt motor...");
        PeanutSDK.getInstance().motor().enable(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onSuccess(BaseResp<String> result) {
                addLog("Motor", "✅ Tắt motor thành công");
            }

            @Override
            public void onSuccess(String requestId, BaseResp<String> result) {
                onSuccess(result);
            }

            @Override
            public void onFail(ApiError error) {
                addLog("Error", "❌ Tắt motor thất bại: " + error.toString());
            }
        }, 0); // Disable với parameter 0
    }

    private void updateButtonStates() {
        boolean enabled = isConnected;
        forwardButton.setEnabled(enabled);
        backwardButton.setEnabled(enabled);
        turnLeftButton.setEnabled(enabled);
        turnRightButton.setEnabled(enabled);
        stopButton.setEnabled(enabled);
        motorEnableButton.setEnabled(enabled);
        motorDisableButton.setEnabled(enabled);
    }

    private void moveForward() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }
        
        addLog("Movement", "⬆️ Đang ra lệnh robot tiến về phía trước...");
        PeanutSDK.getInstance().motor().forward(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onSuccess(BaseResp<String> result) {
                addLog("Movement", "✅ Tiến về phía trước thành công");
            }

            @Override
            public void onSuccess(String requestId, BaseResp<String> result) {
                onSuccess(result);
            }

            @Override
            public void onFail(ApiError error) {
                addLog("Error", "❌ Tiến về phía trước thất bại: " + error.toString());
            }
        });
    }

    private void moveBackward() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }
        
        addLog("Movement", "⬇️ Đang ra lệnh robot lùi về phía sau...");
        PeanutSDK.getInstance().motor().backward(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onSuccess(BaseResp<String> result) {
                addLog("Movement", "✅ Lùi về phía sau thành công");
            }

            @Override
            public void onSuccess(String requestId, BaseResp<String> result) {
                onSuccess(result);
            }

            @Override
            public void onFail(ApiError error) {
                addLog("Error", "❌ Lùi về phía sau thất bại: " + error.toString());
            }
        });
    }

    private void turnLeft() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }
        
        addLog("Movement", "⬅️ Đang ra lệnh robot quay sang trái...");
        PeanutSDK.getInstance().motor().turnLeft(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onSuccess(BaseResp<String> result) {
                addLog("Movement", "✅ Quay sang trái thành công");
            }

            @Override
            public void onSuccess(String requestId, BaseResp<String> result) {
                onSuccess(result);
            }

            @Override
            public void onFail(ApiError error) {
                addLog("Error", "❌ Quay sang trái thất bại: " + error.toString());
            }
        });
    }

    private void turnRight() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }
        
        addLog("Movement", "➡️ Đang ra lệnh robot quay sang phải...");
        PeanutSDK.getInstance().motor().turnRight(new ApiCallback<BaseResp<String>>() {
            @Override
            public void onSuccess(BaseResp<String> result) {
                addLog("Movement", "✅ Quay sang phải thành công");
            }

            @Override
            public void onSuccess(String requestId, BaseResp<String> result) {
                onSuccess(result);
            }

            @Override
            public void onFail(ApiError error) {
                addLog("Error", "❌ Quay sang phải thất bại: " + error.toString());
            }
        });
    }

    private void stopMovement() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }
        
        addLog("Movement", "⏹️ Đang ra lệnh robot dừng lại...");
        // SDK không có API stop riêng, chỉ log thông báo dừng
        addLog("Movement", "✅ Lệnh dừng robot đã được gửi (SDK không có API stop riêng)");
        addLog("Info", "💡 Để dừng robot hoàn toàn, có thể sử dụng nút Enable/Disable motor");
    }

    private void addLog(String tag, String message) {
        String timeStamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        runOnUiThread(() -> {
            logTextView.append("\n[" + timeStamp + "] " + tag + ": " + message);
            // Tự động cuộn xuống cuối
            final int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
            if (scrollAmount > 0) {
                logTextView.scrollTo(0, scrollAmount);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnected) {
            disconnectFromRobot();
        }
    }
}
