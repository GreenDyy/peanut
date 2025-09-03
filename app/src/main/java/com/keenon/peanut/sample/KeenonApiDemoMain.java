/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 * 
 * Sử dụng PeanutSDKManager để quản lý SDK thay vì lặp lại code khởi tạo.
 * PeanutSDKManager cung cấp:
 * - Quản lý trạng thái khởi tạo SDK
 * - Tránh khởi tạo lại SDK
 * - Quản lý lifecycle của SDK
 */
package com.keenon.peanut.sample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.keenon.peanut.sample.base.BaseDemo;
import com.keenon.peanut.sample.chassis.ChassisList;
import com.keenon.peanut.sample.test.TestPerceptionActivity;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.test.TestNavigationActivity;
import com.keenon.peanut.sample.test.TestCameraDetectHumanActivity;
import com.keenon.peanut.sample.test.RobotKeenonActivity;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.VersionInfo;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.common.external.PeanutConfig;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.peanut.sample.util.PeanutSDKManager;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

import static com.keenon.sdk.external.PeanutSDK.SDK_INIT_SUCCESS;

public class KeenonApiDemoMain extends BaseActivity {
  private static final String TAG = KeenonApiDemoMain.class.getSimpleName();
  @BindView(R.id.text_Info)
  TextView text;
  @BindView(R.id.cb_laser)
  CheckBox cbLaser;
  @BindView(R.id.cb_label)
  CheckBox cbLabel;
  @BindView(R.id.listView)
  ListView mListView;

  private static final DemoInfo[] DEMOS = {
      new DemoInfo(R.drawable.ic_gear_simple, R.string.demo_title_baselist, R.string.demo_desc_baselist,
          BaseDemo.class),
      new DemoInfo(R.drawable.ic_battery_simple, R.string.demo_title_chassislist, R.string.demo_desc_chassislist,
          ChassisList.class),
      new DemoInfo(R.drawable.ic_robot_simple, R.string.demo_title_robot_keenon, R.string.demo_desc_robot_keenon,
          RobotKeenonActivity.class),
      new DemoInfo(R.drawable.ic_nav_simple, R.string.demo_title_test_navigation, R.string.demo_desc_test_navigation,
          TestNavigationActivity.class),
      new DemoInfo(R.drawable.ic_vision_eye, R.string.demo_title_test_perception, R.string.demo_desc_test_perception,
          TestPerceptionActivity.class),
      new DemoInfo(R.drawable.ic_vision_eye, R.string.demo_title_test_camera_detect_human,
          R.string.demo_desc_test_camera_detect_human, TestCameraDetectHumanActivity.class),

  };
  private boolean isPermissionRequested;
  private PeanutSDK.ErrorListener mErrorListener = errorCode -> {
    Log.d(TAG, "onInit:" + errorCode);
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        TextView text = (TextView) findViewById(R.id.text_Info);

        if (errorCode == SDK_INIT_SUCCESS) {
          text.setTextColor(Color.GREEN);
          text.setText(getString(R.string.str_init_text) + errorCode);
          // Sử dụng PeanutSDKManager để khởi tạo Runtime
          PeanutSDKManager.startRuntime(new PeanutRuntime.Listener() {
            @Override
            public void onEvent(int event, Object obj) {
              LogUtils.d(TAG, "onEvent:" + event + ", content: " + obj);
            }

            @Override
            public void onHealth(Object content) {
              LogUtils.d(TAG, "onHealth:" + content);
            }

            @Override
            public void onHeartbeat(Object content) {
              LogUtils.d(TAG, "onHeartbeat:" + content);
            }
          });
        } else {
          text.setTextColor(Color.RED);
          text.setText(getString(R.string.str_init_text) + errorCode);
        }
      }
    });
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    ButterKnife.bind(this);
    initView();
    // Kiểm tra và yêu cầu quyền ngay khi khởi động
    checkAndInitSDK();
  }

  @OnCheckedChanged({
      R.id.cb_laser,
      R.id.cb_label
  })
  public void onViewChecked(CompoundButton view, boolean isChecked) {
    switch (view.getId()) {
      case R.id.cb_laser:
        if (isChecked) {
          saveToSP(PeanutConstants.REMOTE_LINK_PROXY);
          cbLabel.setChecked(false);
          // Sử dụng PeanutSDKManager để giải phóng và khởi tạo lại
          PeanutSDKManager.releaseSDK();
          // Kiểm tra quyền trước khi init SDK
          checkAndInitSDK();
        }
        break;
      case R.id.cb_label:
        if (isChecked) {
          saveToSP(PeanutConstants.LOCAL_LINK_PROXY);
          cbLaser.setChecked(false);
          // Sử dụng PeanutSDKManager để giải phóng và khởi tạo lại
          PeanutSDKManager.releaseSDK();
          // Kiểm tra quyền trước khi init SDK
          checkAndInitSDK();
        }
        break;
      default:
        break;
    }
  }

  private void initView() {
    text.setTextColor(Color.GREEN);
    text.setText(getString(R.string.str_content) + VersionInfo.versionName);
    setTitle(getTitle() + " v" + VersionInfo.versionName);
    mListView.setAdapter(new DemoListAdapter());
    mListView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> arg0, View v, int index, long arg3) {
        onListItemClick(index);
      }
    });
    CheckBox checkBoxLaser = findViewById(R.id.cb_laser);
    CheckBox checkBoxLabel = findViewById(R.id.cb_label);
    if (PeanutConstants.REMOTE_LINK_PROXY.equals(getType())) {
      checkBoxLaser.setChecked(true);
      checkBoxLabel.setChecked(false);
    } else {
      checkBoxLaser.setChecked(false);
      checkBoxLabel.setChecked(true);
    }
  }

  private void checkAndInitSDK() {
    if (Build.VERSION.SDK_INT >= 23) {
      ArrayList<String> permissionsList = new ArrayList<>();
      String[] permissions = {
          Manifest.permission.ACCESS_NETWORK_STATE,
          Manifest.permission.INTERNET,
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.ACCESS_WIFI_STATE,
          Manifest.permission.READ_PHONE_STATE,
      };

      for (String perm : permissions) {
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
          permissionsList.add(perm);
        }
      }

      if (!permissionsList.isEmpty()) {
        String[] strings = new String[permissionsList.size()];
        requestPermissions(permissionsList.toArray(strings), 1000); // Đổi requestCode để dễ quản lý hơn
      } else {
        // Đã có tất cả các quyền, tiến hành khởi tạo SDK
        initSDK(getType());
      }
    } else {
      // Phiên bản Android dưới 6.0, không cần xin quyền động
      initSDK(getType());
    }
  }

  private void initSDK(String ip) {
    // Sử dụng PeanutSDKManager để khởi tạo SDK
    PeanutSDKManager.initializeSDK(this.getApplicationContext(), ip, mErrorListener);
  }

  void onListItemClick(int index) {
    Intent intent;
    intent = new Intent(KeenonApiDemoMain.this, DEMOS[index].demoClass);
    this.startActivity(intent);
  }

  @Override
  protected void onDestroy() {
    // Sử dụng PeanutSDKManager để giải phóng SDK
    PeanutSDKManager.releaseSDK();
    super.onDestroy();
  }

  /**
   * Android6.0之后需要动态申请权限
   */
  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {
      isPermissionRequested = true;
      ArrayList<String> permissionsList = new ArrayList<>();
      String[] permissions = {
          Manifest.permission.ACCESS_NETWORK_STATE,
          Manifest.permission.INTERNET,
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.ACCESS_WIFI_STATE,
          Manifest.permission.READ_PHONE_STATE,
      };

      for (String perm : permissions) {
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
          permissionsList.add(perm);
          // 进入到这里代表没有权限.
        }
      }

      if (!permissionsList.isEmpty()) {
        String[] strings = new String[permissionsList.size()];
        requestPermissions(permissionsList.toArray(strings), 0);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 1000) { // Xử lý kết quả từ request mới
      boolean allGranted = true;
      for (int result : grantResults) {
        if (result != PackageManager.PERMISSION_GRANTED) {
          allGranted = false;
          break;
        }
      }
      if (allGranted) {
        // Đã có quyền, khởi tạo SDK với loại kết nối đã lưu
        initSDK(getType());
      } else {
//        text.setTextColor(Color.RED);
//        text.setText("Thiếu quyền Storage. Nhấp để mở Settings và cấp quyền.");
        text.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            openAppSettings();
          }
        });
        // Yêu cầu lại quyền sau 3 giây
        text.postDelayed(new Runnable() {
          @Override
          public void run() {
            checkAndInitSDK();
          }
        }, 3000);
      }
    }
  }

  private void openAppSettings() {
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    Uri uri = Uri.fromParts("package", getPackageName(), null);
    intent.setData(uri);
    startActivityForResult(intent, 1001);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1001) {
      // Quay lại từ Settings, kiểm tra lại quyền
      checkAndInitSDK();
    }
  }

  private static class DemoInfo {
    private final int image;
    private final int title;
    private final int desc;
    private final Class<? extends Activity> demoClass;

    private DemoInfo(int image, int title, int desc, Class<? extends Activity> demoClass) {
      this.image = image;
      this.title = title;
      this.desc = desc;
      this.demoClass = demoClass;
    }
  }

  private class DemoListAdapter extends BaseAdapter {
    private DemoListAdapter() {
      super();
    }

    @Override
    public View getView(int index, View convertView, ViewGroup parent) {
      if (null == convertView) {
        convertView = View.inflate(KeenonApiDemoMain.this, R.layout.item_demo, null);
      }
      ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
      TextView title = (TextView) convertView.findViewById(R.id.title);
      TextView desc = (TextView) convertView.findViewById(R.id.desc);
      imageView.setBackgroundResource(DEMOS[index].image);
      title.setText(DEMOS[index].title);
      desc.setText(DEMOS[index].desc);
      return convertView;
    }

    @Override
    public int getCount() {
      return DEMOS.length;
    }

    @Override
    public Object getItem(int index) {
      return DEMOS[index];
    }

    @Override
    public long getItemId(int id) {
      return id;
    }
  }

  public void saveToSP(String type) {
    SharedPreferences sp = getSharedPreferences("SP",
        Context.MODE_PRIVATE);
    sp.edit().putString("type", type).apply();
  }

  public String getType() {
    SharedPreferences sp = getSharedPreferences("SP",
        Context.MODE_PRIVATE);
    return sp.getString("type", PeanutConstants.REMOTE_LINK_PROXY);
  }

}