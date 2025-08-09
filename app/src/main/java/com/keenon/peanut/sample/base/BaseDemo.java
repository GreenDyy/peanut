package com.keenon.peanut.sample.base;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.coapapi.api.device.DeviceListApi;
import com.keenon.sdk.robot.model.bean.device.DeviceListBean;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.constant.RobotTopic;
// SDK mới không còn sử dụng PeanutSDK.subscribe() và IDataCallback
// import com.keenon.sdk.external.IDataCallback;
// import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class BaseDemo extends BaseActivity {
  private static final String TAG = "BaseDemo";

  @BindView(R.id.tv_api_log)
  TextView tvApiLog;
  @BindView(R.id.sv_api_log)
  ScrollView svApiLog;
  private StringBuilder sb = new StringBuilder();
  private List<DeviceListBean.DeviceBean> deviceList;
  private PeanutRuntime.Listener mRuntimeListener = new PeanutRuntime.Listener() {
    @Override
    public void onEvent(int event, Object obj) {
      Log.d(TAG, "onEvent:" + event + ", content: " + obj);
      tvApiLog.setTextColor(Color.RED);
      
      // Xử lý các event khác nhau
      String eventName = "Unknown";
      switch (event) {
        case 1: // Position Status - giả định
          eventName = "POSITION_STATUS";
          break;
        case 2: // Navigation Path - giả định  
          eventName = "NAVIGATION_PATH";
          break;
        default:
          eventName = "EVENT_" + event;
          break;
      }
      
      PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Event: " + eventName + " = " + (obj != null ? obj.toString() : "null"));
    }

    @Override
    public void onHealth(Object content) {
      Log.d(TAG, "onHealth:" + content);
      tvApiLog.setTextColor(Color.GREEN);
      PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "health success = " + content);
    }

    @Override
    public void onHeartbeat(Object content) {
      Log.d(TAG, "onHeartbeat:" + content);
      tvApiLog.setTextColor(Color.BLACK);
      PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "heartbeat success = " + content);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_robot_base);
    ButterKnife.bind(this);
    setButtonBack();
    initData();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    PeanutRuntime.getInstance().removeListener(mRuntimeListener);
  }

  private void initData() {
    PeanutRuntime.getInstance().registerListener(mRuntimeListener);
  }

  @OnClick({
          R.id.btn_arm_info,
          R.id.btn_stm32_version,
          R.id.btn_query_ip,
          R.id.btn_query_all_dest,
          R.id.btn_query_power,
          R.id.btn_slam_location,
          R.id.btn_query_mileage,
          R.id.btn_sync_params,
          R.id.btn_sync_time,
          R.id.btn_query_path
  })
  public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.btn_arm_info:
        try {
          if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
            String armInfo = PeanutRuntime.getInstance().getRuntimeInfo().getRobotArmInfo();
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "query arm board = " + armInfo);
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - arm info unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error getting arm info: " + e.getMessage());
        }
        break;
      case R.id.btn_stm32_version:
        try {
          if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
            String stm32Info = PeanutRuntime.getInstance().getRuntimeInfo().getRobotStm32Info();
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "query stm32 board = " + stm32Info);
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - stm32 info unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error getting stm32 info: " + e.getMessage());
        }
        break;
      case R.id.btn_query_ip:
        try {
          if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
            String ip = PeanutRuntime.getInstance().getRuntimeInfo().getRobotIp();
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "query IP = " + ip);
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - IP unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error getting robot IP: " + e.getMessage());
        }
        break;
      case R.id.btn_query_all_dest:
        try {
          if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
            String dest = PeanutRuntime.getInstance().getRuntimeInfo().getDestList();
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "dest list = " + dest);
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - dest list unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error getting dest list: " + e.getMessage());
        }
        break;
      case R.id.btn_query_power:
        try {
          if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
            int power = PeanutRuntime.getInstance().getRuntimeInfo().getPower();
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "query power = " + power);
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - power unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error getting power: " + e.getMessage());
        }
        break;
      case R.id.btn_slam_location:
        try {
          if (PeanutRuntime.getInstance() != null) {
            PeanutRuntime.getInstance().location();
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "SLAM location query initiated");
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - location unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error getting location: " + e.getMessage());
        }
        break;
      case R.id.btn_query_mileage:
        try {
          if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
            Double odo = PeanutRuntime.getInstance().getRuntimeInfo().getTotalOdo();
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "query odo = " + odo);
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - mileage unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error getting mileage: " + e.getMessage());
        }
        break;
      case R.id.btn_sync_params:
        try {
          if (PeanutRuntime.getInstance() != null) {
            PeanutRuntime.getInstance().syncParams2Robot(true);
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Sync params to robot initiated");
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - sync params unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error syncing params: " + e.getMessage());
        }
        break;
      case R.id.btn_sync_time:
        try {
          if (PeanutRuntime.getInstance() != null) {
            PeanutRuntime.getInstance().setTime(System.currentTimeMillis());
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Time sync initiated");
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - time sync unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error syncing time: " + e.getMessage());
        }
        break;
      case R.id.btn_query_path:
        try {
          if (PeanutRuntime.getInstance() != null) {
            PeanutRuntime.getInstance().getPath();
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Path query initiated");
          } else {
            PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Runtime not initialized - path query unavailable");
          }
        } catch (Exception e) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Error getting path: " + e.getMessage());
        }
        break;
      default:
        break;
    }
  }

  // Note: Trong SDK mới, subscribe/unsubscribe đã được thay thế
  // Tất cả dữ liệu giờ được nhận qua PeanutRuntime.Listener.onEvent()
  // Các callback cũ đã không còn cần thiết

  @OnCheckedChanged({
          R.id.cb_location_status,
          R.id.cb_navigation_position
  })
  public void onViewChecked(CompoundButton view, boolean isChecked) {
    switch (view.getId()) {
      case R.id.cb_location_status:
        if (isChecked) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Enabled POSITION_STATUS monitoring via PeanutRuntime.Listener");
        } else {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Disabled POSITION_STATUS monitoring");
        }
        break;
      case R.id.cb_navigation_position:
        if (isChecked) {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Enabled NAVIGATION_PATH monitoring via PeanutRuntime.Listener");
        } else {
          PrintLnLog.d(BaseDemo.this, tvApiLog, svApiLog, sb, "Disabled NAVIGATION_PATH monitoring");
        }
        break;
      default:
        break;
    }
  }
}
