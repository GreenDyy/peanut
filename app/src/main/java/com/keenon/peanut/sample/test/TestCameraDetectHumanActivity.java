package com.keenon.peanut.sample.test;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.keenon.component.autoDetection.bean.DetectionConfigBean;
import com.keenon.component.autoDetection.cons.Constants;
import com.keenon.component.autoDetection.iface.IDetectionActionResultListener;
import com.keenon.component.autoDetection.manager.detection.AutoDetectionManager;
import com.keenon.peanut.sample.R;

public class TestCameraDetectHumanActivity extends Activity {

    private static final String TAG = TestCameraDetectHumanActivity.class.getSimpleName();
    private TextView tvStatus;
    private Button btnStart, btnStop, btnDetect;
    private IDetectionActionResultListener onDetectionResultListener;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_camera_detect_human);

        tvStatus = findViewById(R.id.tv_status);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnDetect = findViewById(R.id.btn_detect);

        // 1. Kiểm tra Camera có tồn tại
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            tvStatus.setText("❌ Thiết bị không có Camera");
            return;
        }

        // 2. Kiểm tra quyền camera
        checkCameraPermission();

        // 3. Button start
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDetection();
            }
        });

        // 4. Button stop
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopDetection();
            }
        });

        // 5. Button detect 1 lần
        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performDetection();
            }
        });
    }

    private void checkCameraPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            tvStatus.setText("Đang yêu cầu quyền Camera...");
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            tvStatus.setText("Đã có quyền Camera, khởi tạo detection...");
            initDetection();
        }
    }

    private void initDetection() {
        try {
            DetectionConfigBean config = new DetectionConfigBean();
            config.setDetectionType(Constants.HardwareProvider.PROVIDER_CAMERA);
            config.setSensorNum(2);
            config.setAutoDetectionSwitch(true);

            AutoDetectionManager.ins().init(this, config);

            initDetectionListener();
            tvStatus.setText("✅ Khởi tạo thành công! Sẵn sàng phát hiện người.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing detection: " + e.getMessage(), e);
            tvStatus.setText("❌ Lỗi khởi tạo: " + e.getMessage());
        }
    }

    private void initDetectionListener() {
        onDetectionResultListener = new IDetectionActionResultListener() {
            @Override
            public void onDetectionResult(String str, int i, int i2) {
                processDetectionResult(str, i, i2);
            }
        };
    }

    private void startDetection() {
        if (onDetectionResultListener == null) {
            tvStatus.setText("❌ Chưa khởi tạo detection! Vui lòng kiểm tra quyền Camera.");
            return;
        }
        try {
            tvStatus.setText("Đang bắt đầu phát hiện...");
            AutoDetectionManager.ins().startActionDetection(onDetectionResultListener);
        } catch (Exception e) {
            Log.e(TAG, "Error starting detection: " + e.getMessage(), e);
            tvStatus.setText("❌ Lỗi bắt đầu phát hiện: " + e.getMessage());
        }
    }

    private void stopDetection() {
        try {
            AutoDetectionManager.ins().stopActionDetection();
            tvStatus.setText("Đã dừng phát hiện");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping detection: " + e.getMessage(), e);
            tvStatus.setText("❌ Lỗi dừng phát hiện: " + e.getMessage());
        }
    }

    private void performDetection() {
        if (onDetectionResultListener == null) {
            tvStatus.setText("❌ Chưa khởi tạo detection! Vui lòng kiểm tra quyền Camera.");
            return;
        }

        try {
            tvStatus.setText("Đang thực hiện phát hiện...");
            AutoDetectionManager.ins().startActionDetection(onDetectionResultListener);

            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        AutoDetectionManager.ins().stopActionDetection();
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping detection in delayed task: " + e.getMessage(), e);
                    }
                }
            }, 3000);

        } catch (Exception e) {
            Log.e(TAG, "Error performing detection: " + e.getMessage(), e);
            tvStatus.setText("❌ Lỗi thực hiện phát hiện: " + e.getMessage());
        }
    }

    private void processDetectionResult(String str, int i, int i2) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kết quả phát hiện:\n");
        sb.append("String: ").append(str).append("\n");
        sb.append("Int 1: ").append(i).append("\n");
        sb.append("Int 2: ").append(i2).append("\n");

        if (i == 1) {
            sb.append("✅ Phát hiện người!\n");
        } else {
            sb.append("❌ Không phát hiện người\n");
        }

        final String status = sb.toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStatus.setText(status);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            AutoDetectionManager.ins().stopActionDetection();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping detection onPause: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            AutoDetectionManager.ins().endDetection();
        } catch (Exception e) {
            Log.e(TAG, "Error ending detection onDestroy: " + e.getMessage(), e);
        }
        onDetectionResultListener = null; // tránh leak memory
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvStatus.setText("✅ Đã cấp quyền Camera, khởi tạo detection...");
                initDetection();
            } else {
                tvStatus.setText("❌ Không cấp quyền Camera, không thể test detection!\nVui lòng vào Settings để cấp quyền.");
            }
        }
    }
}
