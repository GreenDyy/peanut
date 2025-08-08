package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.component.charger.PeanutCharger;
import com.keenon.sdk.component.charger.common.Charger;
import com.keenon.sdk.component.charger.common.ChargerInfo;
import com.keenon.sdk.constant.RobotTopic;
// SDK mới không còn sử dụng PeanutSDK.subscribe() và IDataCallback
// import com.keenon.sdk.external.IDataCallback;
// import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class ChargerDemo extends BaseActivity {
  private static final String TAG = "ChargerDemo";

  @BindView(R.id.tv_api_log)
  TextView tvApiLog;
  @BindView(R.id.sv_api_log)
  ScrollView svApiLog;
  PeanutCharger mPeanutCharger;
  private StringBuilder sb = new StringBuilder();

  //充电回调 - SDK mới sử dụng Charger.Listener thay vì subscribe pattern
  Charger.Listener listener=new Charger.Listener() {
    @Override
    public void onChargerInfoChanged(int event, ChargerInfo chargerInfo) {
      Log.d(TAG, "event = " + event +
              " Power = " + chargerInfo.getPower() + " ChargeEvent = " + chargerInfo.getEvent());
      
      // Log chi tiết cho UI
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, 
        "Charger Info - Event: " + event + ", Power: " + chargerInfo.getPower() + 
        "%, ChargeEvent: " + chargerInfo.getEvent());
    }

    @Override
    public void onChargerStatusChanged(int status) {
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "Charger Status = " + status);
      
      // Thông tin này tương đương với CHARGE_MATCH_TIMES trong subscribe pattern cũ
      if (status > 0) {
        PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "Charge Match Times: " + status);
      }
    }

    @Override
    public void onError(int errorCode) {
      PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "Charger Error Code = " + errorCode);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_charge);
    ButterKnife.bind(this);
    setButtonBack();
    initData();
  }

  private void initData() {
    //初始化
    mPeanutCharger = new PeanutCharger.Builder()
        .setListener(listener)
        .build();
    mPeanutCharger.execute();
  }
  // Note: Trong SDK mới, charge match data được nhận qua Charger.Listener
  // thay vì sử dụng subscribe pattern
  @OnCheckedChanged({
      R.id.cb_auto_charge_match
  })
  public void onViewChecked(CompoundButton view, boolean isChecked) {
    switch (view.getId()) {
      case R.id.cb_auto_charge_match:
        if (isChecked) {
          PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "Enabled charge match monitoring via Charger.Listener");
          PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "Topic: " + RobotTopic.CHARGE_MATCH_TIMES);
        } else {
          PrintLnLog.d(ChargerDemo.this, tvApiLog, svApiLog, sb, "Disabled charge match monitoring");
        }
        break;
      default:
        break;
    }
  }

  @OnClick({R.id.btn_auto_charge, R.id.btn_manual_charge, R.id.btn_stop_charge,R.id.btn_adapter_charge})
  public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.btn_manual_charge:
        LogUtils.i(TAG, "Action :" + "btn_manual_charge");
        mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_MANUAL);

        break;
      case R.id.btn_auto_charge:
        mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_AUTO);
        break;
      case R.id.btn_stop_charge:
        mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_STOP);
        break;
      case R.id.btn_adapter_charge:
        mPeanutCharger.performAction(PeanutCharger.CHARGE_ACTION_ADAPTER);
        break;
      default:
        break;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (mPeanutCharger != null) {
      mPeanutCharger.release();
    }
  }
}
