/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package com.keenon.peanut.sample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.VersionInfo;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.common.external.PeanutConfig;
import com.keenon.sdk.external.PeanutSDK;

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
          new DemoInfo(R.drawable.info, R.string.demo_title_baselist, R.string.demo_desc_baselist, BaseDemo.class),
          new DemoInfo(R.drawable.chassis, R.string.demo_title_chassislist, R.string.demo_desc_chassislist, ChassisList.class)
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
          PeanutRuntime.getInstance().start(new PeanutRuntime.Listener() {
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
    // 申请动态权限
    requestPermission();
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
          PeanutSDK.getInstance().release();
          initSDK(PeanutConstants.REMOTE_LINK_PROXY);
        }
        break;
      case R.id.cb_label:
        if (isChecked) {
          saveToSP(PeanutConstants.LOCAL_LINK_PROXY);
          cbLaser.setChecked(false);
          PeanutSDK.getInstance().release();
          initSDK(PeanutConstants.LOCAL_LINK_PROXY);
        }
        break;
      default:
        break;
    }
  }

  private void initView () {
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
    }else{
      checkBoxLaser.setChecked(false);
      checkBoxLabel.setChecked(true);
    }
  }

  private void initSDK(String ip) {
    PeanutConfig.getConfig()
            .setLinkType(PeanutConstants.REMOTE_LINK_PROXY.equals(ip) ? PeanutConstants.LinkType.COAP : PeanutConstants.LinkType.COM_COAP)
            .setLinkIP(ip)
            .enableLog(true)
            .setLogLevel(Log.DEBUG)
            .setAppId("bcb8ebc7f22345bebb378aead035cfb3")
            .setSecret("nPlQERTP4qJWimTp0+ZXXkM5ND93iEyWpM6eXAGIZ/HQmyEg8zN7x5tGLebwINKLYScXEjg5lhQBvt1QCODovm2gq7dsXAK4pgjBRK2OqQHxl4nvTjq2AX9Or6XrdfFfVgOiHqW0mw+qWGDJc1/EUBg3llLOzMNUiDqwPsXMZYs=")
            .enableUMLog(false);
    PeanutSDK.getInstance().init(this.getApplicationContext(), mErrorListener);
  }

  void onListItemClick(int index) {
    Intent intent;
    intent = new Intent(KeenonApiDemoMain.this, DEMOS[index].demoClass);
    this.startActivity(intent);
  }


  @Override
  protected void onDestroy() {
    PeanutSDK.getInstance().release();
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
    sp.edit().putString("type",type).apply();
  }
  public String getType() {
    SharedPreferences sp = getSharedPreferences("SP",
            Context.MODE_PRIVATE);
    return sp.getString("type", PeanutConstants.REMOTE_LINK_PROXY);
  }

}