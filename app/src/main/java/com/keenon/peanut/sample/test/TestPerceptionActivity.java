package com.keenon.peanut.sample.test;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.keenon.peanut.common.welcome.StaticWelcomeHelper; // Import lớp StaticWelcomeHelper
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.robot.SubscribeRespDispatcher;

public class TestPerceptionActivity extends BaseActivity {
    private Button startDetectButton, stopDetectButton;
    private TextView logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_perception);

        startDetectButton = findViewById(R.id.btn_start_detect);
        stopDetectButton = findViewById(R.id.btn_stop_detect);
        logTextView = findViewById(R.id.tv_log);

        setupListeners();
        
        // Log khởi tạo
        addLog("System", "TestPerceptionActivity đã được khởi tạo");
        addLog("Info", "Sẵn sàng phát hiện đối tượng người");
        addLog("Instruction", "Nhấn 'Bắt đầu phát hiện' để bật chế độ tự động chào");
    }

    private void addLog(String tag, String message) {
        runOnUiThread(() -> {
            logTextView.append("\n" + tag + ": " + message);
        });
    }

    private void setupListeners() {
        startDetectButton.setOnClickListener(v -> {
            addLog("Action", "Bật chế độ phát hiện người...");
            try {
                StaticWelcomeHelper.ins().openMoveDetect();
                addLog("Success", "Đã bật chế độ phát hiện người thành công!");
                addLog("Info", "Robot sẽ tự động chào khi phát hiện người.");
            } catch (Exception e) {
                addLog("Error", "Không thể bật chế độ phát hiện: " + e.getMessage());
            }
        });

        stopDetectButton.setOnClickListener(v -> {
            addLog("Action", "Tắt chế độ phát hiện người...");
            try {
                StaticWelcomeHelper.ins().closeMoveDetect();
                addLog("Success", "Đã tắt chế độ phát hiện người thành công!");
                addLog("Info", "Robot sẽ không còn tự động chào khi phát hiện người.");
            } catch (Exception e) {
                addLog("Error", "Không thể tắt chế độ phát hiện: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Tắt chế độ khi Activity bị hủy để đảm bảo không rò rỉ tài nguyên
        addLog("System", "Đang đóng TestPerceptionActivity...");
        try {
            StaticWelcomeHelper.ins().closeMoveDetect();
            addLog("System", "Đã tắt chế độ phát hiện người");
        } catch (Exception e) {
            addLog("Error", "Lỗi khi tắt chế độ phát hiện: " + e.getMessage());
        }
        addLog("System", "TestPerceptionActivity đã được đóng");
    }
}