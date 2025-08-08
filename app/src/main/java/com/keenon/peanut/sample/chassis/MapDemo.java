package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
// SDK mới sử dụng MapComponent thay vì MapManager
// import com.keenon.sdk.sensor.map.MapManager;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.robot.ApiProgressCallback;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MapDemo extends BaseActivity {

    private static final String TAG = MapDemo.class.getSimpleName();
    @BindView(R.id.tv_api_log)
    TextView tvApiLog;
    @BindView(R.id.sv_api_log)
    ScrollView svApiLog;
    private StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        setButtonBack();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // SDK mới không cần release MapComponent vì nó được quản lý bởi PeanutSDK
    }

    private void initData() {
        // SDK mới: MapComponent sử dụng ApiProgressCallback thay vì MapListen
        PrintLnLog.d(MapDemo.this, tvApiLog, svApiLog, sb, "MapDemo initialized with new SDK MapComponent");
    }

    @OnClick({R.id.btn_import, R.id.btn_export})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_import:
                // SDK mới: sử dụng MapComponent.download() thay vì MapManager.onImportToRos()
                PeanutSDK.getInstance().map().download(new ApiProgressCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        PrintLnLog.d(MapDemo.this, tvApiLog, svApiLog, sb, "Map download success: " + result);
                    }
                    
                    @Override
                    public void onProgress(float progress) {
                        PrintLnLog.d(MapDemo.this, tvApiLog, svApiLog, sb, "Map download progress: " + (progress * 100) + "%");
                    }
                    
                    @Override
                    public void onSuccess(String requestId, String result) {
                        PrintLnLog.d(MapDemo.this, tvApiLog, svApiLog, sb, "Map download success, requestId: " + requestId + ", result: " + result);
                    }
                });
                break;
            case R.id.btn_export:
                // SDK mới: sử dụng MapComponent.upload() thay vì MapManager.onExportToAndroid()
                PeanutSDK.getInstance().map().upload(new ApiProgressCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        PrintLnLog.d(MapDemo.this, tvApiLog, svApiLog, sb, "Map upload success: " + result);
                    }
                    
                    @Override
                    public void onProgress(float progress) {
                        PrintLnLog.d(MapDemo.this, tvApiLog, svApiLog, sb, "Map upload progress: " + (progress * 100) + "%");
                    }
                    
                    @Override
                    public void onSuccess(String requestId, String result) {
                        PrintLnLog.d(MapDemo.this, tvApiLog, svApiLog, sb, "Map upload success, requestId: " + requestId + ", result: " + result);
                    }
                }, null); // byte[] data parameter - có thể null cho default behavior
                break;
        }
    }

}
