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
import com.keenon.peanut.sample.test.TestNavigate2Activity;
import com.keenon.peanut.sample.test.TestCameraDetectHumanActivity;
import com.keenon.peanut.sample.test.RobotKeenonActivity;
import com.keenon.peanut.sample.test.HeadMotorControlActivity;
import com.keenon.peanut.sample.test.MqttDemoActivity;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.VersionInfo;
import com.keenon.peanut.sample.util.MqttHandler;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.peanut.sample.util.PeanutSDKManager;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class KeenonApiDemoMain extends BaseActivity {

  private static final String TAG = KeenonApiDemoMain.class.getSimpleName();

  private static final String BROKER_URL = "ssl://emqx.naiscorp.com:8883";
  private static final String CLIENT_ID = "a8faad3e-44fe-41d6-8084-d0ad7ce6dcd9";

  private static final String USER_NAME = "robot01";
  private static final String PASSWORD = "E9SvBhWXK6ZL0z89";
  private String topic = "robot/signal/a8faad3e-44fe-41d6-8084-d0ad7ce6dcd9";

  private MqttHandler mqttHandler;
  private StringBuilder messageLog = new StringBuilder();

  @BindView(R.id.text_Info) TextView text;
  @BindView(R.id.cb_laser) CheckBox cbLaser;
  @BindView(R.id.cb_label) CheckBox cbLabel;
  @BindView(R.id.listView) ListView mListView;

  private static final DemoInfo[] DEMOS = {
          new DemoInfo(R.drawable.ic_gear_simple, R.string.demo_title_baselist, R.string.demo_desc_baselist, BaseDemo.class),
          new DemoInfo(R.drawable.ic_battery_simple, R.string.demo_title_chassislist, R.string.demo_desc_chassislist, ChassisList.class),
          new DemoInfo(R.drawable.ic_robot_simple, R.string.demo_title_robot_keenon, R.string.demo_desc_robot_keenon, RobotKeenonActivity.class),
          new DemoInfo(R.drawable.ic_robot_simple, R.string.demo_title_head_motor_control, R.string.demo_desc_head_motor_control, HeadMotorControlActivity.class),
          new DemoInfo(R.drawable.ic_nav_simple, R.string.demo_title_test_navigation, R.string.demo_desc_test_navigation, TestNavigationActivity.class),
          new DemoInfo(R.drawable.ic_nav_simple, R.string.demo_title_test_navigate2, R.string.demo_desc_test_navigate2, TestNavigate2Activity.class),
          new DemoInfo(R.drawable.ic_vision_eye, R.string.demo_title_test_perception, R.string.demo_desc_test_perception, TestPerceptionActivity.class),
          new DemoInfo(R.drawable.ic_vision_eye, R.string.demo_title_test_camera_detect_human, R.string.demo_desc_test_camera_detect_human, TestCameraDetectHumanActivity.class),
          new DemoInfo(R.drawable.ic_gear_simple, R.string.demo_title_mqtt_demo, R.string.demo_desc_mqtt_demo, MqttDemoActivity.class)
  };

  public static String getAndroidId(Context context) {
    return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    ButterKnife.bind(this);

    initView();
    checkAndInitSDK();
    initMqtt();
  }

  private void initView() {
    text.setTextColor(Color.GREEN);
    text.setText("App Version: " + VersionInfo.versionName);
    setTitle(getTitle() + " v" + VersionInfo.versionName);
    mListView.setAdapter(new DemoListAdapter());
    mListView.setOnItemClickListener((parent, view, position, id) -> onListItemClick(position));

    cbLaser.setChecked(PeanutConstants.REMOTE_LINK_PROXY.equals(getType()));
    cbLabel.setChecked(PeanutConstants.LOCAL_LINK_PROXY.equals(getType()));
  }

  @OnCheckedChanged({R.id.cb_laser, R.id.cb_label})
  public void onViewChecked(CompoundButton view, boolean isChecked) {
    if (view.getId() == R.id.cb_laser && isChecked) {
      saveToSP(PeanutConstants.REMOTE_LINK_PROXY);
      cbLabel.setChecked(false);
      PeanutSDKManager.releaseSDK();
      checkAndInitSDK();
    } else if (view.getId() == R.id.cb_label && isChecked) {
      saveToSP(PeanutConstants.LOCAL_LINK_PROXY);
      cbLaser.setChecked(false);
      PeanutSDKManager.releaseSDK();
      checkAndInitSDK();
    }
  }

  private void initMqtt() {
    mqttHandler = new MqttHandler();

    // Callback khi connect thành công hoặc thất bại
    IMqttActionListener mqttActionListener = new IMqttActionListener() {
      @Override
      public void onSuccess(IMqttToken asyncActionToken) {
        addLog("✅ MQTT connect success");
        // Subcribe ngay sau khi connect thành công
        try {
          mqttHandler.subscribe(topic); // QoS = 1
          addLog("📡 Subscribed to topic: " + topic);
          Log.d(TAG, "📡 Subscribed to topic: " + topic);
        } catch (Exception e) {
          addLog("❌ Subscribe failed: " + e.getMessage());
          Log.d(TAG, "❌ Subscribe failed: " + e.getMessage());
        }
      }

      @Override
      public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        Log.d(TAG,"❌ MQTT connect failed: " + (exception != null ? exception.getMessage() : "Unknown"));
      }
    };

    // Callback xử lý message và connection lost
    MqttCallback mqttCallback = new MqttCallback() {
      @Override
      public void connectionLost(Throwable cause) {
//        addLog("🚨 MQTT connection lost: " + (cause != null ? cause.getMessage() : "Unknown"));
        Log.d(TAG, "🚨 MQTT connection lost");
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
//        addLog("📩 Message arrived [" + topic + "]: " + payload);
        Log.d(TAG, "📩 Message arrived [" + topic + "]: " + payload);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG,"✅ Message delivered");
      }
    };
    String CLIENT_ID = getAndroidId(this);
    Log.d(TAG,"CLIENT_ID: "+ CLIENT_ID);
    // Kết nối MQTT
    mqttHandler.connect(BROKER_URL, CLIENT_ID, USER_NAME, PASSWORD, mqttActionListener, mqttCallback);
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
              Manifest.permission.READ_PHONE_STATE
      };
      for (String perm : permissions) {
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
          permissionsList.add(perm);
        }
      }
      if (!permissionsList.isEmpty()) {
        requestPermissions(permissionsList.toArray(new String[0]), 1000);
      } else {
        initSDK();
      }
    } else {
      initSDK();
    }
  }

  private void initSDK() {
    PeanutSDKManager.initializeSDK(getApplicationContext(), errorCode -> {
      if (errorCode == com.keenon.sdk.external.PeanutSDK.SDK_INIT_SUCCESS) {
        addLog("✅ SDK Init Success");
      } else {
        addLog("❌ SDK Init Failed: " + errorCode);
      }
    });
  }

  private void addLog(String message) {
    String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
    String logEntry = "[" + timestamp + "] " + message + "\n";
    messageLog.append(logEntry);

    runOnUiThread(() -> text.setText(messageLog.toString()));
  }

  void onListItemClick(int index) {
    Intent intent = new Intent(KeenonApiDemoMain.this, DEMOS[index].demoClass);
    startActivity(intent);
  }

  private static class DemoInfo {
    final int image;
    final int title;
    final int desc;
    final Class<? extends Activity> demoClass;

    DemoInfo(int image, int title, int desc, Class<? extends Activity> demoClass) {
      this.image = image;
      this.title = title;
      this.desc = desc;
      this.demoClass = demoClass;
    }
  }

  private class DemoListAdapter extends BaseAdapter {
    @Override
    public int getCount() {
      return DEMOS.length;
    }

    @Override
    public Object getItem(int position) {
      return DEMOS[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = View.inflate(KeenonApiDemoMain.this, R.layout.item_demo, null);
      }
      ImageView imageView = convertView.findViewById(R.id.image);
      TextView title = convertView.findViewById(R.id.title);
      TextView desc = convertView.findViewById(R.id.desc);
      imageView.setBackgroundResource(DEMOS[position].image);
      title.setText(DEMOS[position].title);
      desc.setText(DEMOS[position].desc);
      return convertView;
    }
  }

  public void saveToSP(String type) {
    SharedPreferences sp = getSharedPreferences("SP", Context.MODE_PRIVATE);
    sp.edit().putString("type", type).apply();
  }

  public String getType() {
    SharedPreferences sp = getSharedPreferences("SP", Context.MODE_PRIVATE);
    return sp.getString("type", PeanutConstants.REMOTE_LINK_PROXY);
  }

  @Override
  protected void onDestroy() {
    if (mqttHandler != null && mqttHandler.isConnected()) {
      mqttHandler.disconnect();
    }
    PeanutSDKManager.releaseSDK();
    super.onDestroy();
  }
}
