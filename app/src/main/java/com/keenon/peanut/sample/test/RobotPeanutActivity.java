package com.keenon.peanut.sample.test;

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
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.constant.ApiConstants;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class RobotPeanutActivity extends BaseActivity {
    private static final String TAG = "RobotPeanutActivity";

    @BindView(R.id.tv_api_log)
    TextView tvApiLog;
    @BindView(R.id.sv_api_log)
    ScrollView svApiLog;
    
    private StringBuilder sb = new StringBuilder();
    private PeanutRuntime.Listener mRuntimeListener = new PeanutRuntime.Listener() {
        @Override
        public void onEvent(int event, Object obj) {
            Log.d(TAG, "onEvent:" + event + ", content: " + obj);
            tvApiLog.setTextColor(Color.RED);
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Runtime Event = " + event);
        }

        @Override
        public void onHealth(Object content) {
            Log.d(TAG, "onHealth:" + content);
            tvApiLog.setTextColor(Color.GREEN);
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Runtime Health = " + content);
        }

        @Override
        public void onHeartbeat(Object content) {
            Log.d(TAG, "onHeartbeat:" + content);
            tvApiLog.setTextColor(Color.BLUE);
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Runtime Heartbeat = " + content);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_peanut);
        ButterKnife.bind(this);
        setButtonBack();
        setTitle("Robot Peanut - Tất cả API");
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PeanutRuntime.getInstance().removeListener(mRuntimeListener);
        // Unsubscribe tất cả topics
        PeanutSDK.getInstance().unSubscribe(TopicName.POSITION_STATUS, positionCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.NAVIGATION_PATH, navigationCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.MOTOR_STATUS, motorCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, bottomCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.DOOR_SWITCH_STATUS, doorCallback);
        PeanutSDK.getInstance().unSubscribe(TopicName.CHARGE_MATCH_TIMES, chargeCallback);
    }

    private void initData() {
        PeanutRuntime.getInstance().registerListener(mRuntimeListener);
        
        // Kiểm tra trạng thái ban đầu
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "=== KIỂM TRA TRẠNG THÁI BAN ĐẦU ===");
        checkConnectionStatus();
    }
    
    private void checkConnectionStatus() {
        String ip = PeanutRuntime.getInstance().getRuntimeInfo().getRobotIp();
        String armInfo = PeanutRuntime.getInstance().getRuntimeInfo().getRobotArmInfo();
        int power = PeanutRuntime.getInstance().getRuntimeInfo().getPower();
        
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "IP: " + (ip != null ? ip : "NULL"));
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "ARM Info: " + (armInfo != null ? armInfo : "NULL"));
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Power: " + power);
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "=== HƯỚNG DẪN ===");
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "1. Đảm bảo robot đã được kết nối");
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "2. Nhấn 'Start Runtime' để khởi động");
        PrintLnLog.d(this, tvApiLog, svApiLog, sb, "3. Đợi vài giây rồi thử lại 'Get IP'");
    }

    // ==================== PEANUT RUNTIME API ====================
    
    @OnClick({
            R.id.btn_runtime_start,
            R.id.btn_runtime_location,
            R.id.btn_runtime_get_path,
            R.id.btn_runtime_sync_params,
            R.id.btn_runtime_set_time,
            R.id.btn_runtime_set_work_mode_auto,
            R.id.btn_runtime_set_work_mode_mfg,
            R.id.btn_runtime_get_arm_info,
            R.id.btn_runtime_get_stm32_info,
            R.id.btn_runtime_get_ip,
            R.id.btn_runtime_get_dest_list,
            R.id.btn_runtime_get_power,
            R.id.btn_runtime_get_mileage,
            R.id.btn_check_status
    })
    public void onRuntimeClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_runtime_start:
                PeanutRuntime.getInstance().start(mRuntimeListener);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Start");
                break;
            case R.id.btn_runtime_location:
                PeanutRuntime.getInstance().location();
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Location");
                break;
            case R.id.btn_runtime_get_path:
                // PeanutRuntime.getInstance().getPath(); // Method not available
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Get Path - Method not available");
                break;
            case R.id.btn_runtime_sync_params:
                // PeanutRuntime.getInstance().syncParams2Robot(true); // Method not available
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Sync Params - Method not available");
                break;
            case R.id.btn_runtime_set_time:
                // PeanutRuntime.getInstance().setTime(System.currentTimeMillis()); // Method not available
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Set Time - Method not available");
                break;
            case R.id.btn_runtime_set_work_mode_auto:
                PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.AUTO);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Set Work Mode: AUTO");
                break;
            case R.id.btn_runtime_set_work_mode_mfg:
                PeanutRuntime.getInstance().setWorkMode(ApiConstants.WorkMode.MFG_TEST);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Set Work Mode: MFG_TEST");
                break;
            case R.id.btn_runtime_get_arm_info:
                String armInfo = PeanutRuntime.getInstance().getRuntimeInfo().getRobotArmInfo();
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Arm Info = " + armInfo);
                break;
            case R.id.btn_runtime_get_stm32_info:
                String stm32Info = PeanutRuntime.getInstance().getRuntimeInfo().getRobotStm32Info();
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime STM32 Info = " + stm32Info);
                break;
            case R.id.btn_runtime_get_ip:
                String ip = PeanutRuntime.getInstance().getRuntimeInfo().getRobotIp();
                if (ip != null && !ip.isEmpty()) {
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime IP = " + ip);
                } else {
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime IP = NULL (Robot chưa kết nối hoặc chưa start)");
                }
                break;
            case R.id.btn_runtime_get_dest_list:
                String destList = PeanutRuntime.getInstance().getRuntimeInfo().getDestList();
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Dest List = " + destList);
                break;
            case R.id.btn_runtime_get_power:
                int power = PeanutRuntime.getInstance().getRuntimeInfo().getPower();
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Power = " + power);
                break;
            case R.id.btn_runtime_get_mileage:
                Double mileage = PeanutRuntime.getInstance().getRuntimeInfo().getTotalOdo();
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Runtime Mileage = " + mileage);
                break;
            case R.id.btn_check_status:
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "=== KIỂM TRA TRẠNG THÁI HIỆN TẠI ===");
                checkConnectionStatus();
                break;
        }
    }

    // ==================== PEANUT SDK API ====================
    
    // Position Status Callback
    IDataCallback positionCallback = new IDataCallback() {
        @Override
        public void success(String result) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Position Status = " + result);
        }

        @Override
        public void error(ApiError error) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Position Status Error = " + error.toString());
        }
    };

    // Navigation Path Callback
    IDataCallback navigationCallback = new IDataCallback() {
        @Override
        public void success(String result) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Navigation Path = " + result);
        }

        @Override
        public void error(ApiError error) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Navigation Path Error = " + error.toString());
        }
    };

    // Motor Status Callback
    IDataCallback motorCallback = new IDataCallback() {
        @Override
        public void success(String result) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Motor Status = " + result);
        }

        @Override
        public void error(ApiError error) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Motor Status Error = " + error.toString());
        }
    };

    // Bottom Raw Callback
    IDataCallback bottomCallback = new IDataCallback() {
        @Override
        public void success(String result) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Bottom Raw = " + result);
        }

        @Override
        public void error(ApiError error) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Bottom Raw Error = " + error.toString());
        }
    };

    // Door Switch Callback
    IDataCallback doorCallback = new IDataCallback() {
        @Override
        public void success(String result) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Door Switch Status = " + result);
        }

        @Override
        public void error(ApiError error) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Door Switch Error = " + error.toString());
        }
    };

    // Charge Match Callback
    IDataCallback chargeCallback = new IDataCallback() {
        @Override
        public void success(String result) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Charge Match = " + result);
        }

        @Override
        public void error(ApiError error) {
            PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "Charge Match Error = " + error.toString());
        }
    };

    @OnClick({
            R.id.btn_sdk_motor_enable,
            R.id.btn_sdk_motor_disable,
            R.id.btn_sdk_motor_hrc_enable,
            R.id.btn_sdk_motor_hrc_disable,
            R.id.btn_sdk_motor_get_state,
            R.id.btn_sdk_motor_get_encoder,
            R.id.btn_sdk_motor_get_speed,
            R.id.btn_sdk_motor_get_health,
            R.id.btn_sdk_door_open_1,
            R.id.btn_sdk_door_close_1,
            R.id.btn_sdk_door_open_2,
            R.id.btn_sdk_door_close_2,
            R.id.btn_sdk_door_open_3,
            R.id.btn_sdk_door_close_3,
            R.id.btn_sdk_door_open_4,
            R.id.btn_sdk_door_close_4
    })
    public void onSDKClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_sdk_motor_enable:
                PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_UNLOCK);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "SDK Motor Enable");
                break;
            case R.id.btn_sdk_motor_disable:
                PeanutSDK.getInstance().motor().enable(null, ApiConstants.MOTOR_ENABLE_LOCK);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "SDK Motor Disable");
                break;
            case R.id.btn_sdk_motor_hrc_enable:
                PeanutSDK.getInstance().motor().hrc(null, true);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "SDK Motor HRC Enable");
                break;
            case R.id.btn_sdk_motor_hrc_disable:
                PeanutSDK.getInstance().motor().hrc(null, false);
                PrintLnLog.d(this, tvApiLog, svApiLog, sb, "SDK Motor HRC Disable");
                break;
            case R.id.btn_sdk_motor_get_state:
                PeanutSDK.getInstance().motor().getState(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Motor State = " + result);
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Motor State Error = " + error.toString());
                    }
                });
                break;
            case R.id.btn_sdk_motor_get_encoder:
                PeanutSDK.getInstance().motor().getEncoder(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Motor Encoder = " + result);
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Motor Encoder Error = " + error.toString());
                    }
                });
                break;
            case R.id.btn_sdk_motor_get_speed:
                PeanutSDK.getInstance().motor().getSpeed(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Motor Speed = " + result);
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Motor Speed Error = " + error.toString());
                    }
                });
                break;
            case R.id.btn_sdk_motor_get_health:
                PeanutSDK.getInstance().motor().getHealth(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Motor Health = " + result);
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Motor Health Error = " + error.toString());
                    }
                });
                break;
            case R.id.btn_sdk_door_open_1:
                PeanutSDK.getInstance().door().open(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 1 Open Success");
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 1 Open Error = " + error.toString());
                    }
                }, 1);
                break;
            case R.id.btn_sdk_door_close_1:
                PeanutSDK.getInstance().door().close(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 1 Close Success");
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 1 Close Error = " + error.toString());
                    }
                }, 1);
                break;
            case R.id.btn_sdk_door_open_2:
                PeanutSDK.getInstance().door().open(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 2 Open Success");
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 2 Open Error = " + error.toString());
                    }
                }, 2);
                break;
            case R.id.btn_sdk_door_close_2:
                PeanutSDK.getInstance().door().close(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 2 Close Success");
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 2 Close Error = " + error.toString());
                    }
                }, 2);
                break;
            case R.id.btn_sdk_door_open_3:
                PeanutSDK.getInstance().door().open(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 3 Open Success");
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 3 Open Error = " + error.toString());
                    }
                }, 3);
                break;
            case R.id.btn_sdk_door_close_3:
                PeanutSDK.getInstance().door().close(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 3 Close Success");
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 3 Close Error = " + error.toString());
                    }
                }, 3);
                break;
            case R.id.btn_sdk_door_open_4:
                PeanutSDK.getInstance().door().open(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 4 Open Success");
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 4 Open Error = " + error.toString());
                    }
                }, 4);
                break;
            case R.id.btn_sdk_door_close_4:
                PeanutSDK.getInstance().door().close(new IDataCallback() {
                    @Override
                    public void success(String result) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 4 Close Success");
                    }

                    @Override
                    public void error(ApiError error) {
                        PrintLnLog.d(RobotPeanutActivity.this, tvApiLog, svApiLog, sb, "SDK Door 4 Close Error = " + error.toString());
                    }
                }, 4);
                break;
        }
    }

    // ==================== SUBSCRIBE/UNSUBSCRIBE ====================
    
    @OnCheckedChanged({
            R.id.cb_position_status,
            R.id.cb_navigation_path,
            R.id.cb_motor_status,
            R.id.cb_bottom_raw,
            R.id.cb_door_switch,
            R.id.cb_charge_match
    })
    public void onSubscribeChecked(CompoundButton view, boolean isChecked) {
        switch (view.getId()) {
            case R.id.cb_position_status:
                if (isChecked) {
                    PeanutSDK.getInstance().subscribe(TopicName.POSITION_STATUS, positionCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Subscribe Position Status");
                } else {
                    PeanutSDK.getInstance().unSubscribe(TopicName.POSITION_STATUS, positionCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Unsubscribe Position Status");
                }
                break;
            case R.id.cb_navigation_path:
                if (isChecked) {
                    PeanutSDK.getInstance().subscribe(TopicName.NAVIGATION_PATH, navigationCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Subscribe Navigation Path");
                } else {
                    PeanutSDK.getInstance().unSubscribe(TopicName.NAVIGATION_PATH, navigationCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Unsubscribe Navigation Path");
                }
                break;
            case R.id.cb_motor_status:
                if (isChecked) {
                    PeanutSDK.getInstance().subscribe(TopicName.MOTOR_STATUS, motorCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Subscribe Motor Status");
                } else {
                    PeanutSDK.getInstance().unSubscribe(TopicName.MOTOR_STATUS, motorCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Unsubscribe Motor Status");
                }
                break;
            case R.id.cb_bottom_raw:
                if (isChecked) {
                    PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, bottomCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Subscribe Bottom Raw");
                } else {
                    PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, bottomCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Unsubscribe Bottom Raw");
                }
                break;
            case R.id.cb_door_switch:
                if (isChecked) {
                    PeanutSDK.getInstance().subscribe(TopicName.DOOR_SWITCH_STATUS, doorCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Subscribe Door Switch Status");
                } else {
                    PeanutSDK.getInstance().unSubscribe(TopicName.DOOR_SWITCH_STATUS, doorCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Unsubscribe Door Switch Status");
                }
                break;
            case R.id.cb_charge_match:
                if (isChecked) {
                    PeanutSDK.getInstance().subscribe(TopicName.CHARGE_MATCH_TIMES, chargeCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Subscribe Charge Match");
                } else {
                    PeanutSDK.getInstance().unSubscribe(TopicName.CHARGE_MATCH_TIMES, chargeCallback);
                    PrintLnLog.d(this, tvApiLog, svApiLog, sb, "Unsubscribe Charge Match");
                }
                break;
        }
    }
} 