package com.keenon.peanut.sample.test;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class ObjectPerceptionTestActivity extends BaseActivity {
    private static final String TAG = "ObjectPerceptionTest";
    private static final String TOPIC_OBJECT_PERCEPTION = "action/topic/object_perception";

    @BindView(R.id.tv_api_log)
    TextView tvApiLog;
    @BindView(R.id.sv_api_log)
    ScrollView svApiLog;
    @BindView(R.id.cb_object_perception)
    CheckBox cbObjectPerception;

    private StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_perception_test);
        ButterKnife.bind(this);
        setButtonBack();
        setTitle("Object Perception Test");
        
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unsubscribe topic khi destroy
        try {
            PeanutSDK.getInstance().unSubscribe(TOPIC_OBJECT_PERCEPTION, objectPerceptionCallback);
            Log.d(TAG, "Unsubscribed from " + TOPIC_OBJECT_PERCEPTION);
        } catch (Exception e) {
            Log.e(TAG, "Error unsubscribing: " + e.getMessage());
        }
    }

    private void initData() {
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "=== OBJECT PERCEPTION TEST ACTIVITY ===");
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Topic: " + TOPIC_OBJECT_PERCEPTION);
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Sẵn sàng để test subscribe/unsubscribe");
        Log.d(TAG, "ObjectPerceptionTestActivity initialized");
    }

    // Callback cho Object Perception topic
    IDataCallback objectPerceptionCallback = new IDataCallback() {
        @Override
        public void success(String result) {
            String logMsg = "🎯 OBJECT PERCEPTION SUCCESS: " + result;
            Log.d(TAG, logMsg);
            tvApiLog.setTextColor(Color.GREEN);
            PrintLnLog.d(ObjectPerceptionTestActivity.this, tvApiLog, svApiLog, sb, logMsg);
            
            // Log thêm thông tin chi tiết
            Log.d(TAG, "Raw data length: " + (result != null ? result.length() : "null"));
            Log.d(TAG, "Timestamp: " + System.currentTimeMillis());
        }

        @Override
        public void error(ApiError error) {
            String logMsg = "❌ OBJECT PERCEPTION ERROR: " + (error != null ? error.toString() : "Unknown error");
            Log.e(TAG, logMsg);
            tvApiLog.setTextColor(Color.RED);
            PrintLnLog.d(ObjectPerceptionTestActivity.this, tvApiLog, svApiLog, sb, logMsg);
            
            // Log thêm thông tin lỗi
            if (error != null) {
                Log.e(TAG, "Error code: " + error.getCode());
                Log.e(TAG, "Full error details: " + error.toString());
            }
        }
    };

    @OnCheckedChanged(R.id.cb_object_perception)
    public void onObjectPerceptionChecked(CompoundButton view, boolean isChecked) {
        try {
            if (isChecked) {
                // Subscribe
                PeanutSDK.getInstance().subscribe(TOPIC_OBJECT_PERCEPTION, objectPerceptionCallback);
                String logMsg = "✅ SUBSCRIBED to " + TOPIC_OBJECT_PERCEPTION;
                Log.d(TAG, logMsg);
                tvApiLog.setTextColor(Color.BLUE);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, logMsg);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Đang lắng nghe dữ liệu từ Object Perception...");
            } else {
                // Unsubscribe
                PeanutSDK.getInstance().unSubscribe(TOPIC_OBJECT_PERCEPTION, objectPerceptionCallback);
                String logMsg = "❌ UNSUBSCRIBED from " + TOPIC_OBJECT_PERCEPTION;
                Log.d(TAG, logMsg);
                tvApiLog.setTextColor(Color.GRAY);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, logMsg);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Đã dừng lắng nghe Object Perception");
            }
        } catch (Exception e) {
            String errorMsg = "🚨 EXCEPTION: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            tvApiLog.setTextColor(Color.RED);
            PrintLnLog.d(this, tvApiLog, svApiLog, sb, errorMsg);
            
            // Reset checkbox nếu có lỗi
            cbObjectPerception.setChecked(false);
        }
    }
}
