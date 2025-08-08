package com.keenon.peanut.sample.chassis;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.PrintLnLog;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.constant.ApiConstants;
import com.keenon.sdk.constant.RobotTopic;
// SDK mới sử dụng MotorComponent và ApiCallback thay vì ApiCallback<String> và PeanutSDK
// import com.keenon.sdk.external.ApiCallback<String>;
// import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.robot.ApiCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTouch;

public class MotorDemo extends BaseActivity {

  @BindView(R.id.tv_api_log)
  TextView tvApiLog;
  @BindView(R.id.sv_api_log)
  ScrollView svApiLog;
  boolean manualControlLongClicked, isRunning;
  private StringBuilder sb = new StringBuilder();
  private ScheduledThreadPoolExecutor executor;
  
  // PeanutRuntime.Listener cho SDK mới thay vì subscribe pattern
  private PeanutRuntime.Listener mRuntimeListener = new PeanutRuntime.Listener() {
    @Override
    public void onEvent(int event, Object obj) {
      // Xử lý các event khác nhau
      String eventName = "EVENT_" + event;
      PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, 
        "Runtime Event: " + eventName + " = " + (obj != null ? obj.toString() : "null"));
    }

    @Override
    public void onHealth(Object content) {
      PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "Runtime Health: " + content);
    }

    @Override
    public void onHeartbeat(Object content) {
      PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "Runtime Heartbeat: " + content);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_motor);
    ButterKnife.bind(this);
    setButtonBack();

    initData();
  }

  @Override
  protected void onPause() {
    super.onPause();
    manualControlLongClicked = false;
  }

  @Override
  protected void onDestroy() {
    PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.AUTO);
    // SDK mới sử dụng removeListener thay vì unSubscribe
    PeanutRuntime.getInstance().removeListener(mRuntimeListener);
    super.onDestroy();
  }

  // Note: bottomCallback đã được thay thế bởi PeanutRuntime.Listener.onEvent()
  // Dữ liệu BOTTOM_RAW giờ được nhận qua runtime listener

  private void initData() {
    // step 1: switch work mode to factory mode
    PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.MFG_TEST);
    
    // SDK mới: registerListener thay vì subscribe
    PeanutRuntime.getInstance().registerListener(mRuntimeListener);
    PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "Registered runtime listener for " + RobotTopic.BOTTOM_RAW);

    PeanutSDK.getInstance().motor().getState(new ApiCallback<String>() {
      @Override
      public void onSuccess(String result) {
        PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "motor state success = " + result);
      }

      @Override
      public void onSuccess(String requestId, String result) {
        PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "motor state success, requestId: " + requestId + ", result: " + result);
      }
    });
  }

  // Note: motorCallback đã được thay thế bởi PeanutRuntime.Listener.onEvent()
  // Dữ liệu MOTOR_STATUS giờ được nhận qua runtime listener

  @OnCheckedChanged({
      R.id.sw_motor_enable,
      R.id.sw_motor_hrc,
      R.id.cb_motor_status
  })
  public void onViewChecked(CompoundButton view, boolean isChecked) {
    switch (view.getId()) {
      case R.id.sw_motor_enable:
        if (isChecked) {
          PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_UNLOCK);
        } else {
          PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_LOCK);
        }
        break;
      case R.id.sw_motor_hrc:
        if (isChecked) {
          PeanutSDK.getInstance().motor().hrc(null, true);
        } else {
          PeanutSDK.getInstance().motor().hrc(null, false);
        }
        break;
      case R.id.cb_motor_status:
        if (isChecked) {
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "Enabled motor status monitoring via PeanutRuntime.Listener");
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "Topic: " + RobotTopic.MOTOR_STATUS);
        } else {
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "Disabled motor status monitoring");
        }
        break;
      default:
        break;
    }
  }

  @OnClick({
      R.id.btn_motor_encoder,
      R.id.btn_motor_speed,
      R.id.btn_motor_health})
  public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.btn_motor_encoder:
        PeanutSDK.getInstance().motor().getEncoder(new ApiCallback<String>() {
          @Override
          public void onSuccess(String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor encoder success = " + response);
          }

          @Override
          public void onSuccess(String requestId, String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor encoder success, requestId: " + requestId + ", response: " + response);
          }
        });
        break;
      case R.id.btn_motor_speed:
        PeanutSDK.getInstance().motor().getSpeed(new ApiCallback<String>() {
          @Override
          public void onSuccess(String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor speed success = " + response);
          }

          @Override
          public void onSuccess(String requestId, String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor speed success, requestId: " + requestId + ", response: " + response);
          }
        });
        break;
      case R.id.btn_motor_health:
        PeanutSDK.getInstance().motor().getHealth(new ApiCallback<String>() {
          @Override
          public void onSuccess(String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor health success = " + response);
          }

          @Override
          public void onSuccess(String requestId, String response) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "query motor health success, requestId: " + requestId + ", response: " + response);
          }
        });
        break;
      default:
        break;
    }
  }

  @OnTouch({R.id.btn_move_forward, R.id.btn_move_backward, R.id.btn_turn_right, R.id.btn_turn_left})
  public boolean onViewTouched(View view, MotionEvent event) {
    switch (view.getId()) {
      case R.id.btn_move_forward:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          front();
          manualControlLongClicked = true;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "down info front ");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          executor.shutdownNow();
          manualControlLongClicked = false;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "up info front ");
        }
        break;
      case R.id.btn_move_backward:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          back();
          manualControlLongClicked = true;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "down info back ");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          executor.shutdownNow();
          manualControlLongClicked = false;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "up info back ");
        }
        break;
      case R.id.btn_turn_right:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          right();
          manualControlLongClicked = true;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "down info right ");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          executor.shutdownNow();
          manualControlLongClicked = false;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "up info right ");
        }
        break;
      case R.id.btn_turn_left:
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          left();
          manualControlLongClicked = true;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "down info left ");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
          executor.shutdownNow();
          manualControlLongClicked = false;
          PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "up info left ");
        }
        break;
      default:
        break;
    }
    return true;
  }


  private void front() {
    if (manualControlLongClicked) {
      return;
    }
    executor = new ScheduledThreadPoolExecutor(5);
    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        PeanutSDK.getInstance().motor().manual(new ApiCallback<String>() {
          @Override
          public void onSuccess(String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success = " + result);
          }

          @Override
          public void onSuccess(String requestId, String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success, requestId: " + requestId + ", result: " + result);
          }
        }, ApiConstants.MotorMove.FRONT);
      }
    }, 0, 100, TimeUnit.MILLISECONDS);
  }

  private void back() {
    if (manualControlLongClicked) {
      return;
    }
    executor = new ScheduledThreadPoolExecutor(5);
    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        PeanutSDK.getInstance().motor().manual(new ApiCallback<String>() {
          @Override
          public void onSuccess(String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success = " + result);
          }

          @Override
          public void onSuccess(String requestId, String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success (overload), requestId: " + requestId + ", result: " + result);
          }
        }, ApiConstants.MotorMove.BACK);
      }
    }, 0, 100, TimeUnit.MILLISECONDS);
  }

  private void left() {
    if (manualControlLongClicked) {
      return;
    }
    executor = new ScheduledThreadPoolExecutor(5);
    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        PeanutSDK.getInstance().motor().manual(new ApiCallback<String>() {
          @Override
          public void onSuccess(String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success = " + result);
          }

          @Override
          public void onSuccess(String requestId, String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success (overload), requestId: " + requestId + ", result: " + result);
          }
        }, ApiConstants.MotorMove.LEFT);
      }
    }, 0, 100, TimeUnit.MILLISECONDS);
  }

  private void right() {
    if (manualControlLongClicked) {
      return;
    }
    executor = new ScheduledThreadPoolExecutor(5);
    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        PeanutSDK.getInstance().motor().manual(new ApiCallback<String>() {
          @Override
          public void onSuccess(String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success = " + result);
          }

          @Override
          public void onSuccess(String requestId, String result) {
            PrintLnLog.d(MotorDemo.this, tvApiLog, svApiLog, sb, "manual run front success (overload), requestId: " + requestId + ", result: " + result);
          }
        }, ApiConstants.MotorMove.RIGHT);
      }
    }, 0, 100, TimeUnit.MILLISECONDS);
  }

}
