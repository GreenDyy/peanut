package com.naiscorp.vietjet.keenon;

import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import androidx.annotation.Nullable;

import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.sensor.headmotor.SensorHeadMotor;
import com.keenon.sdk.sensor.headmotor.HeadMotorInterface;
import com.keenon.sdk.robot.compat.IRobotApiCompat;
import com.keenon.sdk.sensor.headmotor.HeadMotorBean;
import com.keenon.sdk.sensor.headmotor.HeadMotorStateResult;

import com.keenon.common.external.PeanutConfig;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.robot.ApiCallback;
import com.keenon.sdk.robot.base.BaseResp;
import com.keenon.sdk.robot.base.ApiTopic;
import com.keenon.sdk.robot.model.bean.system.IPBean;
import com.keenon.sdk.robot.model.bean.device.MotorStatusBean;
import com.keenon.sdk.robot.model.bean.battery.BatteryInfoBean;
import com.keenon.sdk.robot.model.bean.battery.ChargeStatusBean;
import com.keenon.sdk.robot.model.bean.sensor.ObjectPerceptionBean;
import com.keenon.sdk.constant.ApiConstants;

// Thêm imports cho sensors
import com.keenon.sdk.embedded.common.PeanutSensors;
import com.keenon.sdk.embedded.common.Sensor;
import com.keenon.sdk.embedded.common.SensorObserver;
import com.keenon.sdk.embedded.common.Event;
import com.keenon.sdk.sensor.keyevnet.SensorKeyEvent;
import com.keenon.sdk.sensor.era.SensorEarKeyEvent;

import static com.keenon.sdk.external.PeanutSDK.SDK_INIT_SUCCESS;

public class KeenonSdkModule extends ReactContextBaseJavaModule {
    private static final String TAG = KeenonSdkModule.class.getSimpleName();
    private final Context appContext;
    private boolean isPerceptionSubscribed = false;
    // Không cần biến headMotor nữa, sẽ sử dụng SensorHeadMotor.getInstance() trực
    // tiếp

    // TopicManager và SensorObserver
    private TopicManager topicManager;
    private MySensorObserver mySensorObserver;

    public KeenonSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.appContext = reactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "KeenonBridge";
    }

    // --- SDK Init ---
    @ReactMethod
    public void initSdk(String appId, String secret, Promise promise) {
        try {
            PeanutSDK.ErrorListener mErrorListener = errorCode -> {
                Log.d(TAG, "SDK Init callback received with errorCode: " + errorCode);
                if (errorCode == SDK_INIT_SUCCESS) {
                    Log.i(TAG, "SDK Initialization successful");
                    try {
                        PeanutRuntime.getInstance().start(runtimeListener);
                        Log.d(TAG, "PeanutRuntime started successfully");

                        // Initialize head motor trước - đảm bảo khởi tạo trước TopicManager
                        try {
                            SensorHeadMotor.getInstance();
                            Log.d(TAG, "HeadMotor initialized successfully");
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to initialize HeadMotor: " + e.getMessage());
                        }

                        // Initialize TopicManager và SensorObserver sau
                        initializeTopicManager();
                        Log.d(TAG, "TopicManager initialized successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start PeanutRuntime: " + e.getMessage());
                    }
                    WritableMap map = Arguments.createMap();
                    map.putString("message", "SDK initialized successfully");
                    map.putInt("errorCode", errorCode);
                    Log.d(TAG, "Resolving promise with success");
                    promise.resolve(map);
                } else {
                    Log.e(TAG, "SDK Initialization failed with errorCode: " + errorCode);
                    promise.reject("INIT_ERROR", "Error code: " + errorCode);
                }
            };

            PeanutConfig.getConfig()
                    .setLinkType(PeanutConstants.LinkType.COAP)
                    .setLinkCOM(PeanutConstants.COM1)
                    .setLinkIP(PeanutConstants.REMOTE_LINK_PROXY)
                    .setLinkPort(5683)
                    .enableLog(true)
                    .setLogLevel(Log.DEBUG)
                    .setAppId(appId)
                    .setSecret(secret);
            Log.d(TAG, "PeanutConfig configured. Initializing PeanutSDK...");

            try {
                PeanutSDK.getInstance().init(appContext, mErrorListener);
                Log.d(TAG, "PeanutSDK.init() called successfully");
            } catch (Exception e) {
                Log.e(TAG, "Exception during PeanutSDK.init(): " + e.getMessage());
                promise.reject("INIT_EXCEPTION", "Exception during SDK init: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in initSdk: " + e.getMessage());
            promise.reject("INIT_EXCEPTION", "Exception in initSdk: " + e.getMessage());
        }
    }

    // --- SDK Release ---
    @ReactMethod
    public void releaseSdk(Promise promise) {
        try {
            // Unsubscribe from perception events before releasing
            if (isPerceptionSubscribed) {
                unsubscribePerceptionEvent();
            }

            // Cleanup TopicManager
            cleanupTopicManager();

            // Release head motor
            try {
                SensorHeadMotor.getInstance().release();
                Log.d(TAG, "HeadMotor released successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to release HeadMotor: " + e.getMessage());
            }

            PeanutSDK.getInstance().release();
            WritableMap map = Arguments.createMap();
            map.putString("message", "SDK released successfully");
            promise.resolve(map);
        } catch (Exception e) {
            promise.reject("RELEASE_ERROR", e.getMessage());
        }
    }

    // --- TopicManager Initialization ---
    private void initializeTopicManager() {
        try {
            Log.d(TAG, "Initializing TopicManager...");

            // Tạo TopicManager
            topicManager = new TopicManager();
            Log.d(TAG, "TopicManager created");

            // Tạo MySensorObserver
            mySensorObserver = new MySensorObserver(this);
            Log.d(TAG, "MySensorObserver created");

            // Gắn SensorObserver vào TopicManager
            topicManager.addSensorObserver(mySensorObserver);
            Log.d(TAG, "SensorObserver added to TopicManager");

            // Subscribe topics với SensorHeadMotor.getInstance()
            topicManager.subscribeTopics(SensorHeadMotor.getInstance());
            Log.d(TAG, "Topics subscribed successfully");

            Log.d(TAG, "TopicManager initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TopicManager: " + e.getMessage());
            throw e;
        }
    }

    // --- TopicManager Cleanup ---
    private void cleanupTopicManager() {
        try {
            Log.d(TAG, "Cleaning up TopicManager...");

            if (topicManager != null) {
                // Remove sensor observer
                if (mySensorObserver != null) {
                    topicManager.removeSensorObserver(mySensorObserver);
                    Log.d(TAG, "SensorObserver removed from TopicManager");
                }

                // Unsubscribe topics
                topicManager.unsubscribeTopics();
                Log.d(TAG, "Topics unsubscribed");

                topicManager = null;
                mySensorObserver = null;
            }

            Log.d(TAG, "TopicManager cleaned up successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup TopicManager: " + e.getMessage());
        }
    }

    // --- Battery ---
    @ReactMethod
    public void getBatteryStatus(Promise promise) {
        try {
            PeanutRuntime runtime = PeanutRuntime.getInstance();
            WritableMap map = Arguments.createMap();

            if (runtime != null && runtime.getRuntimeInfo() != null) {
                map.putInt("batteryLevel", runtime.getRuntimeInfo().getPower());
            } else {
                map.putInt("batteryLevel", 0);
            }
            map.putBoolean("isCharging", false);
            promise.resolve(map);
        } catch (Exception e) {
            promise.reject("BATTERY_ERROR", e.getMessage());
        }
    }

    // --- Runtime Info ---
    @ReactMethod
    public void getRuntimeInfo(Promise promise) {
        try {
            PeanutRuntime runtime = PeanutRuntime.getInstance();
            if (runtime != null && runtime.getRuntimeInfo() != null) {
                WritableMap map = Arguments.createMap();
                map.putInt("workMode", runtime.getRuntimeInfo().getWorkMode());
                map.putInt("power", runtime.getRuntimeInfo().getPower());
                map.putString("robotIp", runtime.getRuntimeInfo().getRobotIp());
                promise.resolve(map);
            } else {
                promise.reject("RUNTIME_ERROR", "Runtime info not available");
            }
        } catch (Exception e) {
            promise.reject("RUNTIME_ERROR", e.getMessage());
        }
    }

    // --- Robot Info ---
    @ReactMethod
    public void getRobotInfo(Promise promise) {
        try {
            WritableMap map = Arguments.createMap();

            if (PeanutSDK.getInstance() != null) {
                map.putBoolean("sdkAvailable", true);

                // Get IP address
                if (PeanutSDK.getInstance().runtime() != null) {
                    PeanutSDK.getInstance().runtime().getIPAddress(new ApiCallback<BaseResp<IPBean>>() {
                        @Override
                        public void onSuccess(BaseResp<IPBean> result) {
                            if (result != null && result.getData() != null) {
                                WritableMap ipMap = Arguments.createMap();
                                ipMap.putString("ip", result.getData().toString());
                                sendEvent("onRobotInfo", ipMap);
                            }
                        }

                        @Override
                        public void onSuccess(String requestId, BaseResp<IPBean> result) {
                            onSuccess(result);
                        }

                        @Override
                        public void onFail(ApiError error) {
                            WritableMap errorMap = Arguments.createMap();
                            errorMap.putString("error", "IP request failed: " + error.toString());
                            sendEvent("onRobotInfoError", errorMap);
                        }
                    });
                }

                // Get runtime details
                if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
                    map.putInt("power", PeanutRuntime.getInstance().getRuntimeInfo().getPower());
                    map.putString("robotIp", PeanutRuntime.getInstance().getRuntimeInfo().getRobotIp());
                    map.putInt("workMode", PeanutRuntime.getInstance().getRuntimeInfo().getWorkMode());
                    map.putString("destList", PeanutRuntime.getInstance().getRuntimeInfo().getDestList());
                }
            } else {
                map.putBoolean("sdkAvailable", false);
            }

            promise.resolve(map);
        } catch (Exception e) {
            promise.reject("ROBOT_INFO_ERROR", e.getMessage());
        }
    }

    // --- Battery Info ---
    @ReactMethod
    public void getBatteryInfo(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().batteryInfo(new ApiCallback<BaseResp<BatteryInfoBean>>() {
                    @Override
                    public void onSuccess(BaseResp<BatteryInfoBean> result) {
                        if (result != null && result.getData() != null) {
                            WritableMap map = Arguments.createMap();
                            map.putString("batteryInfo", result.getData().toString());
                            map.putInt("code", result.getCode());
                            if (result.getMsg() != null) {
                                map.putString("msg", result.getMsg());
                            }
                            promise.resolve(map);
                        } else {
                            promise.reject("BATTERY_INFO_ERROR", "No battery data received");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<BatteryInfoBean> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("BATTERY_INFO_ERROR", "Battery info failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("BATTERY_INFO_ERROR", "Battery component not available");
            }
        } catch (Exception e) {
            promise.reject("BATTERY_INFO_ERROR", e.getMessage());
        }
    }

    // --- Charge Status ---
    @ReactMethod
    public void getChargeStatus(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().getChargeMatches(new ApiCallback<BaseResp<ChargeStatusBean>>() {
                    @Override
                    public void onSuccess(BaseResp<ChargeStatusBean> result) {
                        if (result != null && result.getData() != null) {
                            WritableMap map = Arguments.createMap();
                            map.putString("chargeStatus", result.getData().toString());
                            map.putInt("code", result.getCode());
                            if (result.getMsg() != null) {
                                map.putString("msg", result.getMsg());
                            }
                            promise.resolve(map);
                        } else {
                            promise.reject("CHARGE_STATUS_ERROR", "No charge status data");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<ChargeStatusBean> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("CHARGE_STATUS_ERROR", "Charge status failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("CHARGE_STATUS_ERROR", "Battery component not available");
            }
        } catch (Exception e) {
            promise.reject("CHARGE_STATUS_ERROR", e.getMessage());
        }
    }

    // --- Auto Charge ---
    @ReactMethod
    public void startAutoCharge(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().autoCharge(1, new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        if (result != null && result.getData() != null) {
                            WritableMap map = Arguments.createMap();
                            map.putString("response", result.getData());
                            map.putInt("code", result.getCode());
                            if (result.getMsg() != null) {
                                map.putString("msg", result.getMsg());
                            }
                            promise.resolve(map);
                        } else {
                            promise.resolve("Auto charge initiated");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("AUTO_CHARGE_ERROR", "Auto charge failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("AUTO_CHARGE_ERROR", "Battery component not available");
            }
        } catch (Exception e) {
            promise.reject("AUTO_CHARGE_ERROR", e.getMessage());
        }
    }

    // --- Motor Control ---
    @ReactMethod
    public void moveForward(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().forward(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        promise.resolve("Forward movement initiated");
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("MOTOR_ERROR", "Forward failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("MOTOR_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("MOTOR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void moveBackward(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().backward(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        promise.resolve("Backward movement initiated");
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("MOTOR_ERROR", "Backward failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("MOTOR_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("MOTOR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void turnLeft(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().turnLeft(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        WritableMap map = Arguments.createMap();
                        map.putString("message", "Turn left initiated");
                        map.putString("rawResult", result != null ? result.toString() : "null");
                        if (result != null && result.getData() != null) {
                            map.putString("data", result.getData());
                        }
                        if (result != null) {
                            map.putInt("code", result.getCode());
                        }
                        if (result != null && result.getMsg() != null) {
                            map.putString("msg", result.getMsg());
                        }
                        promise.resolve(map);
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("MOTOR_ERROR", "Turn left failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("MOTOR_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("MOTOR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void turnRight(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().turnRight(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        WritableMap map = Arguments.createMap();
                        map.putString("message", "Turn right initiated");
                        map.putString("rawResult", result != null ? result.toString() : "null");
                        if (result != null && result.getData() != null) {
                            map.putString("data", result.getData());
                        }
                        if (result != null) {
                            map.putInt("code", result.getCode());
                        }
                        if (result != null && result.getMsg() != null) {
                            map.putString("msg", result.getMsg());
                        }
                        promise.resolve(map);
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("MOTOR_ERROR", "Turn right failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("MOTOR_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("MOTOR_ERROR", e.getMessage());
        }
    }

    // --- Motor Enable/Disable ---
    @ReactMethod
    public void enableMotor(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().enable(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        WritableMap map = Arguments.createMap();
                        map.putString("message", "Motor enabled");
                        map.putString("rawResult", result != null ? result.toString() : "null");
                        if (result != null && result.getData() != null) {
                            map.putString("data", result.getData());
                        }
                        if (result != null) {
                            map.putInt("code", result.getCode());
                        }
                        if (result != null && result.getMsg() != null) {
                            map.putString("msg", result.getMsg());
                        }
                        promise.resolve(map);
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("MOTOR_ERROR", "Motor enable failed: " + error.toString());
                    }
                }, 1);
            } else {
                promise.reject("MOTOR_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("MOTOR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void disableMotor(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().enable(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        WritableMap map = Arguments.createMap();
                        map.putString("message", "Motor disabled");
                        map.putString("rawResult", result != null ? result.toString() : "null");
                        if (result != null && result.getData() != null) {
                            map.putString("data", result.getData());
                        }
                        if (result != null) {
                            map.putInt("code", result.getCode());
                        }
                        if (result != null && result.getMsg() != null) {
                            map.putString("msg", result.getMsg());
                        }
                        promise.resolve(map);
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("MOTOR_ERROR", "Motor disable failed: " + error.toString());
                    }
                }, 0);
            } else {
                promise.reject("MOTOR_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("MOTOR_ERROR", e.getMessage());
        }
    }

    // --- Motor Status ---
    @ReactMethod
    public void getMotorStatus(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().getStatus(new ApiCallback<BaseResp<MotorStatusBean>>() {
                    @Override
                    public void onSuccess(BaseResp<MotorStatusBean> result) {
                        if (result != null && result.getData() != null) {
                            WritableMap map = Arguments.createMap();
                            map.putString("motorStatus", result.getData().toString());
                            map.putInt("code", result.getCode());
                            if (result.getMsg() != null) {
                                map.putString("msg", result.getMsg());
                            }
                            promise.resolve(map);
                        } else {
                            promise.reject("MOTOR_STATUS_ERROR", "No motor status data");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<MotorStatusBean> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("MOTOR_STATUS_ERROR", "Motor status failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("MOTOR_STATUS_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("MOTOR_STATUS_ERROR", e.getMessage());
        }
    }

    // --- Motor Health ---
    @ReactMethod
    public void getMotorHealth(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().getStatus(new ApiCallback<BaseResp<MotorStatusBean>>() {
                    @Override
                    public void onSuccess(BaseResp<MotorStatusBean> result) {
                        if (result != null && result.getData() != null) {
                            WritableMap map = Arguments.createMap();
                            map.putString("motorStatus", result.getData().toString());
                            map.putInt("code", result.getCode());
                            if (result.getMsg() != null) {
                                map.putString("msg", result.getMsg());
                            }
                            promise.resolve(map);
                        } else {
                            promise.reject("MOTOR_HEALTH_ERROR", "No motor status data");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<MotorStatusBean> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("MOTOR_HEALTH_ERROR", "Motor health failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("MOTOR_HEALTH_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("MOTOR_HEALTH_ERROR", e.getMessage());
        }
    }

    // --- Get Speed ---
    @ReactMethod
    public void getSpeed(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() != null) {
                PeanutSDK.getInstance().motor().getStatus(new ApiCallback<BaseResp<MotorStatusBean>>() {
                    @Override
                    public void onSuccess(BaseResp<MotorStatusBean> result) {
                        if (result != null && result.getData() != null) {
                            WritableMap map = Arguments.createMap();
                            map.putString("message", "Speed retrieved successfully");
                            map.putString("motorStatus", result.getData().toString());
                            map.putInt("code", result.getCode());
                            if (result.getMsg() != null) {
                                map.putString("msg", result.getMsg());
                            }
                            promise.resolve(map);
                        } else {
                            promise.reject("SPEED_ERROR", "No speed data available");
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<MotorStatusBean> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("SPEED_ERROR", "Get speed failed: " + error.toString());
                    }
                });
            } else {
                promise.reject("SPEED_ERROR", "Motor component not available");
            }
        } catch (Exception e) {
            promise.reject("SPEED_ERROR", e.getMessage());
        }
    }

    // --- Move Control ---
    // @ReactMethod
    // public void moveControl(String direction, ReadableMap moveParams, Promise
    // promise) {
    // try {
    // if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().motor() !=
    // null) {
    // ApiCallback<BaseResp<String>> callback = new ApiCallback<BaseResp<String>>()
    // {
    // @Override
    // public void onSuccess(BaseResp<String> result) {
    // WritableMap map = Arguments.createMap();
    // map.putString("message", "Move control executed: " + direction);
    // map.putString("direction", direction);
    // map.putString("rawResult", result != null ? result.toString() : "null");
    // if (result != null && result.getData() != null) {
    // map.putString("data", result.getData());
    // }
    // if (result != null) {
    // map.putInt("code", result.getCode());
    // }
    // if (result != null && result.getMsg() != null) {
    // map.putString("msg", result.getMsg());
    // }
    // promise.resolve(map);
    // }
    //
    // @Override
    // public void onSuccess(String requestId, BaseResp<String> result) {
    // onSuccess(result);
    // }
    //
    // @Override
    // public void onFail(ApiError error) {
    // promise.reject("MOVE_CONTROL_ERROR", "Move control failed: " +
    // error.toString());
    // }
    // };
    //
    // // Create ParamDevicesMove object
    // com.keenon.sdk.robot.model.param.ParamDevicesMove paramMove = new
    // com.keenon.sdk.robot.model.param.ParamDevicesMove();
    //
    // // Set parameters from ReadableMap
    // if (moveParams != null) {
    // if (moveParams.hasKey("linear")) {
    // paramMove.setLinear(moveParams.getDouble("linear"));
    // }
    // if (moveParams.hasKey("angular")) {
    // paramMove.setAngular(moveParams.getDouble("angular"));
    // }
    // if (moveParams.hasKey("direction")) {
    // paramMove.setDirection(moveParams.getDouble("direction"));
    // }
    // if (moveParams.hasKey("time")) {
    // paramMove.setTime(moveParams.getDouble("time"));
    // }
    // if (moveParams.hasKey("reset")) {
    // paramMove.setReset(moveParams.getInt("reset"));
    // }
    // }
    //
    // // Execute movement based on direction
    // switch (direction.toLowerCase()) {
    // case "forward":
    // PeanutSDK.getInstance().motor().forward(callback, paramMove);
    // break;
    // case "backward":
    // PeanutSDK.getInstance().motor().backward(callback, paramMove);
    // break;
    // case "left":
    // PeanutSDK.getInstance().motor().turnLeft(callback, paramMove);
    // break;
    // case "right":
    // PeanutSDK.getInstance().motor().turnRight(callback, paramMove);
    // break;
    // case "stop":
    // // Try to use stop method if available, otherwise disable motor
    // try {
    // PeanutSDK.getInstance().motor().stop(callback, paramMove);
    // } catch (Exception e) {
    // // If stop method doesn't exist, disable motor instead
    // PeanutSDK.getInstance().motor().enable(callback, 0);
    // }
    // break;
    // default:
    // promise.reject("MOVE_CONTROL_ERROR", "Invalid direction: " + direction);
    // return;
    // }
    // } else {
    // promise.reject("MOVE_CONTROL_ERROR", "Motor component not available");
    // }
    // } catch (Exception e) {
    // promise.reject("MOVE_CONTROL_ERROR", e.getMessage());
    // }
    // }

    // --- Door Control ---
    @ReactMethod
    public void openDoor(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().door() != null) {
                PeanutSDK.getInstance().door().open(new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        WritableMap map = Arguments.createMap();
                        map.putString("message", "Door opened");
                        map.putString("rawResult", result != null ? result : "null");
                        promise.resolve(map);
                    }

                    @Override
                    public void onSuccess(String requestId, String result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("DOOR_ERROR", "Door open failed: " + error.toString());
                    }
                }, 1);
            } else {
                promise.reject("DOOR_ERROR", "Door component not available");
            }
        } catch (Exception e) {
            promise.reject("DOOR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void closeDoor(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().door() != null) {
                PeanutSDK.getInstance().door().close(new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        WritableMap map = Arguments.createMap();
                        map.putString("message", "Door closed");
                        map.putString("rawResult", result != null ? result : "null");
                        promise.resolve(map);
                    }

                    @Override
                    public void onSuccess(String requestId, String result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("DOOR_ERROR", "Door close failed: " + error.toString());
                    }
                }, 1);
            } else {
                promise.reject("DOOR_ERROR", "Door component not available");
            }
        } catch (Exception e) {
            promise.reject("DOOR_ERROR", e.getMessage());
        }
    }

    // --- Human Detection & Object Perception ---
    @ReactMethod
    public void startHumanDetection(Promise promise) {
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().runtime() != null) {
                PeanutSDK.getInstance().runtime().setWelcomeSwitch(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        subscribePerceptionEvent();
                        WritableMap map = Arguments.createMap();
                        map.putString("message", "Human detection started");
                        map.putString("rawResult", result != null ? result.toString() : "null");
                        if (result != null && result.getData() != null) {
                            map.putString("data", result.getData());
                        }
                        if (result != null) {
                            map.putInt("code", result.getCode());
                        }
                        if (result != null && result.getMsg() != null) {
                            map.putString("msg", result.getMsg());
                        }
                        promise.resolve(map);
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        promise.resolve("Human detection started 2 param ne");
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("HUMAN_DETECTION_ERROR", "Failed to start human detection: " + error.toString());
                    }
                }, true);
            } else {
                promise.reject("HUMAN_DETECTION_ERROR", "Runtime component not available");
            }
        } catch (Exception e) {
            promise.reject("HUMAN_DETECTION_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void stopHumanDetection(Promise promise) {
        PeanutSDK.getInstance().runtime().setWelcomeSwitch(null, false);

        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().runtime() != null) {
                PeanutSDK.getInstance().runtime().setWelcomeSwitch(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        unsubscribePerceptionEvent();
                        WritableMap map = Arguments.createMap();
                        map.putString("message", "Human detection stopped");
                        map.putString("rawResult", result != null ? result.toString() : "null");
                        if (result != null && result.getData() != null) {
                            map.putString("data", result.getData());
                        }
                        if (result != null) {
                            map.putInt("code", result.getCode());
                        }
                        if (result != null && result.getMsg() != null) {
                            map.putString("msg", result.getMsg());
                        }
                        promise.resolve(map);
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        promise.reject("HUMAN_DETECTION_ERROR", "Failed to stop human detection: " + error.toString());
                    }
                }, false);
            } else {
                promise.reject("HUMAN_DETECTION_ERROR", "Runtime component not available");
            }
        } catch (Exception e) {
            promise.reject("HUMAN_DETECTION_ERROR", e.getMessage());
        }
    }

    private void subscribePerceptionEvent() {
        try {
            ApiCallback<String> perceptionCallback = new ApiCallback<String>() {
                @Override
                public void onSuccess(String requestId, String result) {
                    if (result != null) {
                        handleObjectPerception(result);
                    }
                }

                @Override
                public void onSuccess(String result) {
                    if (result != null) {
                        handleObjectPerception(result);
                    }
                }

                @Override
                public void onFail(ApiError error) {
                    WritableMap errorMap = Arguments.createMap();
                    errorMap.putString("error", "Perception subscription error: " + error.toString());
                    sendEvent("onPerceptionError", errorMap);
                }
            };

            PeanutSDK.getInstance().subscribe(ApiTopic.SENSOR_EVENT, 500, perceptionCallback);
            // PeanutSDK.getInstance().subscribe(ApiTopic.SENSOR_DEPTH_V2, 500,
            // perceptionCallback);
            // PeanutSDK.getInstance().subscribe(ApiTopic.OBJECT_PERCEPTION, 500,
            // perceptionCallback);
            PeanutSDK.getInstance().subscribe(ApiTopic.DYNAMIC_OBJECT_PERCEPTION, 500, perceptionCallback);
            // PeanutSDK.getInstance().subscribe(ApiTopic.NAVI_HUMAN_DETECTION_STATUS, 500,
            // perceptionCallback);

            isPerceptionSubscribed = true;

            WritableMap map = Arguments.createMap();
            map.putString("message", "Perception events subscribed");
            sendEvent("onPerceptionSubscribed", map);

        } catch (Exception e) {
            WritableMap errorMap = Arguments.createMap();
            errorMap.putString("error", "Failed to subscribe to perception events: " + e.getMessage());
            sendEvent("onPerceptionError", errorMap);
        }
    }

    @ReactMethod
    public void unsubscribePerceptionEvent(Promise promise) {
        try {
            PeanutSDK.getInstance().unSubscribe(ApiTopic.SENSOR_EVENT, null);
            // PeanutSDK.getInstance().unSubscribe(ApiTopic.SENSOR_DEPTH_V2, null);
            // PeanutSDK.getInstance().unSubscribe(ApiTopic.OBJECT_PERCEPTION, null);
            PeanutSDK.getInstance().unSubscribe(ApiTopic.DYNAMIC_OBJECT_PERCEPTION, null);
            // PeanutSDK.getInstance().unSubscribe(ApiTopic.NAVI_HUMAN_DETECTION_STATUS,
            // null);

            isPerceptionSubscribed = false;

            WritableMap map = Arguments.createMap();
            map.putString("message", "Perception events unsubscribed");
            promise.resolve(map);

        } catch (Exception e) {
            promise.reject("PERCEPTION_UNSUBSCRIBE_ERROR",
                    "Failed to unsubscribe from perception events: " + e.getMessage());
        }
    }

    private void unsubscribePerceptionEvent() {
        try {
            PeanutSDK.getInstance().unSubscribe(ApiTopic.SENSOR_EVENT, null);
            // PeanutSDK.getInstance().unSubscribe(ApiTopic.SENSOR_DEPTH_V2, null);
            // PeanutSDK.getInstance().unSubscribe(ApiTopic.OBJECT_PERCEPTION, null);
            PeanutSDK.getInstance().unSubscribe(ApiTopic.DYNAMIC_OBJECT_PERCEPTION, null);
            // PeanutSDK.getInstance().unSubscribe(ApiTopic.NAVI_HUMAN_DETECTION_STATUS,
            // null);

            isPerceptionSubscribed = false;

            WritableMap map = Arguments.createMap();
            map.putString("message", "Perception events unsubscribed");
            sendEvent("onPerceptionUnsubscribed", map);

        } catch (Exception e) {
            WritableMap errorMap = Arguments.createMap();
            errorMap.putString("error", "Failed to unsubscribe from perception events: " + e.getMessage());
            sendEvent("onPerceptionError", errorMap);
        }
    }

    private void handleObjectPerception(String data) {
        try {
            WritableMap map = Arguments.createMap();
            map.putString("rawData", data);
            map.putString("timestamp", String.valueOf(System.currentTimeMillis()));

            // Try to parse as ObjectPerceptionBean if possible
            try {
                // You might need to add Gson dependency for JSON parsing
                // For now, we'll send the raw data and let React Native handle parsing
                map.putString("parsed", "true");
            } catch (Exception e) {
                map.putString("parsed", "false");
                map.putString("parseError", e.getMessage());
            }

            sendEvent("onObjectPerception", map);

        } catch (Exception e) {
            WritableMap errorMap = Arguments.createMap();
            errorMap.putString("error", "Failed to handle object perception: " + e.getMessage());
            sendEvent("onPerceptionError", errorMap);
        }
    }

    // --- Runtime Listener ---
    private final PeanutRuntime.Listener runtimeListener = new PeanutRuntime.Listener() {
        @Override
        public void onEvent(int event, Object data) {
            WritableMap map = Arguments.createMap();
            map.putInt("event", event);
            map.putString("rawData", data != null ? data.toString() : "null");
            sendEvent("onRuntimeEvent", map);
        }

        @Override
        public void onHealth(Object data) {
            WritableMap map = Arguments.createMap();
            map.putString("health", data != null ? data.toString() : "null");
            sendEvent("onRuntimeHealth", map);
        }

        @Override
        public void onHeartbeat(Object data) {
            WritableMap map = Arguments.createMap();
            map.putString("hearrtbeat", data != null ? data.toString() : "null");
            sendEvent("onRuntimeHeartbeat", map);
        }
    };

    // --- Head Motor Control ---
    @ReactMethod
    public void getHeadMotorState(Promise promise) {
        try {
            // Kiểm tra xem SensorHeadMotor có được khởi tạo đúng cách không
            try {
                SensorHeadMotor headMotorSensor = SensorHeadMotor.getInstance();
                if (headMotorSensor != null) {
                    headMotorSensor.getHeadMotorState();
                    WritableMap map = Arguments.createMap();
                    map.putString("message", "Head motor state request sent");
                    map.putBoolean("sensorRegistered", true);
                    map.putBoolean("headMotorInitialized", true);
                    promise.resolve(map);
                } else {
                    WritableMap map = Arguments.createMap();
                    map.putString("message", "Head motor sensor not properly registered");
                    map.putBoolean("sensorRegistered", false);
                    map.putBoolean("headMotorInitialized", true);
                    promise.resolve(map);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to check sensor registration: " + e.getMessage());
                WritableMap map = Arguments.createMap();
                map.putString("message", "Head motor sensor not properly registered");
                map.putBoolean("sensorRegistered", false);
                map.putBoolean("headMotorInitialized", true);
                promise.resolve(map);
            }
        } catch (Exception e) {
            promise.reject("HEAD_MOTOR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void checkHeadMotorStatus(Promise promise) {
        try {
            WritableMap map = Arguments.createMap();

            // Kiểm tra headMotor instance
            map.putBoolean("headMotorInstance", SensorHeadMotor.getInstance() != null);

            // Kiểm tra TopicManager
            map.putBoolean("topicManagerAvailable", topicManager != null);

            // Kiểm tra MySensorObserver
            map.putBoolean("sensorObserverAvailable", mySensorObserver != null);

            // Kiểm tra sensor registration - sử dụng cách khác
            boolean sensorRegistered = false;
            try {
                // Thử lấy sensor instance trực tiếp
                SensorHeadMotor headMotorSensor = SensorHeadMotor.getInstance();
                sensorRegistered = headMotorSensor != null;
            } catch (Exception e) {
                Log.w(TAG, "Failed to check sensor registration: " + e.getMessage());
            }
            map.putBoolean("sensorRegistered", sensorRegistered);

            map.putString("topicManagerClass", topicManager != null ? topicManager.getClass().getSimpleName() : "null");

            map.putString("message", "Head motor status checked");
            promise.resolve(map);

        } catch (Exception e) {
            promise.reject("HEAD_MOTOR_STATUS_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void reinitializeHeadMotor(Promise promise) {
        try {
            Log.d(TAG, "Reinitializing head motor...");

            // Cleanup existing head motor
            try {
                SensorHeadMotor.getInstance().release();
            } catch (Exception e) {
                Log.w(TAG, "Failed to release existing head motor: " + e.getMessage());
            }

            // Reinitialize
            initializeTopicManager();

            // Get new instance
            SensorHeadMotor.getInstance();

            WritableMap map = Arguments.createMap();
            map.putString("message", "Head motor reinitialized successfully");
            map.putBoolean("success", true);
            promise.resolve(map);

        } catch (Exception e) {
            Log.e(TAG, "Failed to reinitialize head motor: " + e.getMessage());
            promise.reject("HEAD_MOTOR_REINIT_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void reinitializeTopicManager(Promise promise) {
        try {
            Log.d(TAG, "Reinitializing TopicManager...");

            // Cleanup existing TopicManager
            cleanupTopicManager();

            // Reinitialize TopicManager
            initializeTopicManager();

            WritableMap map = Arguments.createMap();
            map.putString("message", "TopicManager reinitialized successfully");
            map.putBoolean("success", true);
            promise.resolve(map);

        } catch (Exception e) {
            Log.e(TAG, "Failed to reinitialize TopicManager: " + e.getMessage());
            promise.reject("TOPIC_MANAGER_REINIT_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void forceInitializeHeadMotor(Promise promise) {
        try {
            Log.d(TAG, "Force initializing HeadMotor...");

            // Force khởi tạo lại headMotor
            try {
                SensorHeadMotor.getInstance();
                Log.d(TAG, "HeadMotor force initialized successfully");

                WritableMap map = Arguments.createMap();
                map.putString("message", "HeadMotor force initialized successfully");
                map.putBoolean("success", true);
                map.putBoolean("headMotorInitialized", true);
                promise.resolve(map);

            } catch (Exception e) {
                Log.e(TAG, "Failed to force initialize HeadMotor: " + e.getMessage());
                promise.reject("HEAD_MOTOR_FORCE_INIT_ERROR", e.getMessage());
            }

        } catch (Exception e) {
            Log.e(TAG, "Force initialize HeadMotor error: " + e.getMessage());
            promise.reject("HEAD_MOTOR_FORCE_INIT_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void debugSensors(Promise promise) {
        try {
            WritableMap map = Arguments.createMap();

            // Kiểm tra TopicManager
            map.putBoolean("topicManagerAvailable", topicManager != null);
            map.putBoolean("sensorObserverAvailable", mySensorObserver != null);

            // Kiểm tra PeanutSensors instance
            boolean peanutSensorsAvailable = PeanutSensors.getInstance() != null;
            map.putBoolean("peanutSensorsAvailable", peanutSensorsAvailable);

            if (peanutSensorsAvailable) {
                // Kiểm tra các sensors đã đăng ký - sử dụng cách khác
                boolean headMotorRegistered = false;
                boolean keyEventRegistered = false;
                boolean earKeyEventRegistered = false;

                try {
                    // Thử lấy sensor instances trực tiếp
                    SensorHeadMotor headMotorSensor = SensorHeadMotor.getInstance();
                    headMotorRegistered = headMotorSensor != null;
                } catch (Exception e) {
                    Log.w(TAG, "SensorHeadMotor not available: " + e.getMessage());
                }

                try {
                    SensorKeyEvent keyEventSensor = SensorKeyEvent.getInstance();
                    keyEventRegistered = keyEventSensor != null;
                } catch (Exception e) {
                    Log.w(TAG, "SensorKeyEvent not available: " + e.getMessage());
                }

                try {
                    SensorEarKeyEvent earKeyEventSensor = SensorEarKeyEvent.getInstance();
                    earKeyEventRegistered = earKeyEventSensor != null;
                } catch (Exception e) {
                    Log.w(TAG, "SensorEarKeyEvent not available: " + e.getMessage());
                }

                map.putBoolean("headMotorRegistered", headMotorRegistered);
                map.putBoolean("keyEventRegistered", keyEventRegistered);
                map.putBoolean("earKeyEventRegistered", earKeyEventRegistered);
            }

            // Kiểm tra headMotor instance
            boolean headMotorAvailable = false;
            try {
                SensorHeadMotor.getInstance();
                headMotorAvailable = true;
            } catch (Exception e) {
                Log.w(TAG, "SensorHeadMotor not available: " + e.getMessage());
            }
            map.putBoolean("headMotorInstance", headMotorAvailable);

            // Kiểm tra SDK status
            boolean sdkAvailable = PeanutSDK.getInstance() != null;
            map.putBoolean("sdkAvailable", sdkAvailable);

            // Kiểm tra runtime status
            boolean runtimeAvailable = PeanutRuntime.getInstance() != null;
            map.putBoolean("runtimeAvailable", runtimeAvailable);

            map.putString("message", "Sensor debug information collected");
            promise.resolve(map);

        } catch (Exception e) {
            Log.e(TAG, "Debug sensors error: " + e.getMessage());
            promise.reject("DEBUG_SENSORS_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void controlHeadMotor(String action, Promise promise) {
        try {
            // Kiểm tra xem sensor có được đăng ký đúng cách không
            try {
                // Đảm bảo SensorHeadMotor được đăng ký với PeanutSensors
                PeanutSensors.getInstance().putSensor(SensorHeadMotor.getInstance());
                SensorHeadMotor.getInstance().setSerialDirect(true);
                Log.d(TAG, "Head motor sensor registered with PeanutSensors");

                HeadMotorInterface.MotorAction motorAction = parseMotorAction(action);
                if (motorAction != null) {
                    Log.d(TAG, "Executing head motor action: " + action);
                    SensorHeadMotor.getInstance().onControlHeadMotorPlay(motorAction);
                    WritableMap map = Arguments.createMap();
                    map.putString("message", "Head motor control executed: " + action);
                    map.putString("action", action);
                    map.putBoolean("sensorRegistered", true);
                    promise.resolve(map);
                } else {
                    promise.reject("HEAD_MOTOR_ERROR", "Invalid action: " + action);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to register head motor sensor: " + e.getMessage());
                promise.reject("HEAD_MOTOR_ERROR", "Head motor sensor not properly registered");
            }
        } catch (Exception e) {
            Log.e(TAG, "Head motor control error: " + e.getMessage());
            promise.reject("HEAD_MOTOR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void controlHeadMotorWithAngle(String action, int angle, Promise promise) {
        try {
            SensorHeadMotor headMotorSensor = SensorHeadMotor.getInstance();
            HeadMotorInterface.MotorAction motorAction = parseMotorAction(action);
            if (motorAction != null) {
                headMotorSensor.onControlHeadMotorPlay(motorAction, (short) angle);
                WritableMap map = Arguments.createMap();
                map.putString("message", "Head motor control with angle executed");
                map.putString("action", action);
                map.putInt("angle", angle);
                promise.resolve(map);
            } else {
                promise.reject("HEAD_MOTOR_ERROR", "Invalid action: " + action);
            }
        } catch (Exception e) {
            promise.reject("HEAD_MOTOR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void controlHeadMotorWithAngles(String action, int angleX, int angleY, Promise promise) {
        try {
            SensorHeadMotor headMotorSensor = SensorHeadMotor.getInstance();
            HeadMotorInterface.MotorAction motorAction = parseMotorAction(action);
            if (motorAction != null) {
                headMotorSensor.onControlHeadMotorPlay(motorAction, (short) angleX, (short) angleY);
                WritableMap map = Arguments.createMap();
                map.putString("message", "Head motor control with angles executed");
                map.putString("action", action);
                map.putInt("angleX", angleX);
                map.putInt("angleY", angleY);
                promise.resolve(map);
            } else {
                promise.reject("HEAD_MOTOR_ERROR", "Invalid action: " + action);
            }
        } catch (Exception e) {
            promise.reject("HEAD_MOTOR_ERROR", e.getMessage());
        }
    }

    // Helper method to parse motor action string to enum
    private HeadMotorInterface.MotorAction parseMotorAction(String action) {
        if (action == null)
            return null;

        switch (action.toLowerCase()) {
            case "reset":
            case "calibrate":
                return HeadMotorInterface.MotorAction.RESET;
            case "vertical":
            case "pa":
                return HeadMotorInterface.MotorAction.PA;
            case "tilt1":
            case "pb":
                return HeadMotorInterface.MotorAction.PB;
            case "nod":
            case "pc":
                return HeadMotorInterface.MotorAction.PC;
            case "shake":
            case "pd":
                return HeadMotorInterface.MotorAction.PD;
            case "look_around":
            case "pe":
                return HeadMotorInterface.MotorAction.PE;
            case "turn":
            case "pf":
                return HeadMotorInterface.MotorAction.PF;
            case "tilt2":
            case "pg":
                return HeadMotorInterface.MotorAction.PG;
            case "turn_left":
            case "turnleft":
            case "ph":
                return HeadMotorInterface.MotorAction.PH;
            case "turn_right":
            case "turnright":
            case "pi":
                return HeadMotorInterface.MotorAction.PI;
            case "face_tracking":
            case "pj":
                return HeadMotorInterface.MotorAction.PJ;
            case "vertical2":
            case "pk":
                return HeadMotorInterface.MotorAction.PK;
            case "block_way":
            case "pl":
                return HeadMotorInterface.MotorAction.PL;
            case "startup":
            case "pz":
                return HeadMotorInterface.MotorAction.PZ;
            case "nod2":
            case "px":
                return HeadMotorInterface.MotorAction.PX;
            case "touch_ear":
            case "pv":
                return HeadMotorInterface.MotorAction.PV;
            default:
                return null;
        }
    }

    // --- Head Motor Convenience Methods ---
    @ReactMethod
    public void headNod(Promise promise) {
        controlHeadMotor("nod", promise);
    }

    @ReactMethod
    public void headShake(Promise promise) {
        controlHeadMotor("shake", promise);
    }

    @ReactMethod
    public void headTurnLeft(Promise promise) {
        controlHeadMotor("turn_left", promise);
    }

    @ReactMethod
    public void headTurnRight(Promise promise) {
        controlHeadMotor("turn_right", promise);
    }

    @ReactMethod
    public void headReset(Promise promise) {
        controlHeadMotor("reset", promise);
    }

    // --- New Head Motor Actions ---
    @ReactMethod
    public void headVertical(Promise promise) {
        controlHeadMotor("vertical", promise);
    }

    @ReactMethod
    public void headTilt1(Promise promise) {
        controlHeadMotor("tilt1", promise);
    }

    @ReactMethod
    public void headTilt2(Promise promise) {
        controlHeadMotor("tilt2", promise);
    }

    @ReactMethod
    public void headLookAround(Promise promise) {
        controlHeadMotor("look_around", promise);
    }

    @ReactMethod
    public void headTurn(Promise promise) {
        controlHeadMotor("turn", promise);
    }

    @ReactMethod
    public void headFaceTracking(Promise promise) {
        controlHeadMotor("face_tracking", promise);
    }

    @ReactMethod
    public void headVertical2(Promise promise) {
        controlHeadMotor("vertical2", promise);
    }

    @ReactMethod
    public void headBlockWay(Promise promise) {
        controlHeadMotor("block_way", promise);
    }

    @ReactMethod
    public void headStartup(Promise promise) {
        controlHeadMotor("startup", promise);
    }

    @ReactMethod
    public void headNod2(Promise promise) {
        controlHeadMotor("nod2", promise);
    }

    @ReactMethod
    public void headTouchEar(Promise promise) {
        controlHeadMotor("touch_ear", promise);
    }

    // --- Head Motor Actions with Angle ---
    @ReactMethod
    public void headTurnLeftWithAngle(int angle, Promise promise) {
        controlHeadMotorWithAngle("turn_left", angle, promise);
    }

    @ReactMethod
    public void headTurnRightWithAngle(int angle, Promise promise) {
        controlHeadMotorWithAngle("turn_right", angle, promise);
    }

    @ReactMethod
    public void headVerticalWithAngle(int angle, Promise promise) {
        controlHeadMotorWithAngle("vertical", angle, promise);
    }

    @ReactMethod
    public void headTilt1WithAngle(int angle, Promise promise) {
        controlHeadMotorWithAngle("tilt1", angle, promise);
    }

    @ReactMethod
    public void headTilt2WithAngle(int angle, Promise promise) {
        controlHeadMotorWithAngle("tilt2", angle, promise);
    }

    @ReactMethod
    public void headTurnWithAngle(int angle, Promise promise) {
        controlHeadMotorWithAngle("turn", angle, promise);
    }

    @ReactMethod
    public void headMoveToPosition(int angleX, int angleY, Promise promise) {
        controlHeadMotorWithAngles("nod", angleX, angleY, promise);
    }

    // để dùng cho js nhận sự kiện khi runtime dưới java thay d
    public void sendEvent(String eventName, @Nullable WritableMap params) {
        ReactContext reactContext = getReactApplicationContext();
        if (reactContext != null && reactContext.hasActiveReactInstance()) {
            reactContext.getJSModule(RCTNativeAppEventEmitter.class).emit(eventName, params);
        }
    }

    // Required for NativeEventEmitter
    @ReactMethod
    public void addListener(String eventName) {
    }

    @ReactMethod
    public void removeListeners(Integer count) {
    }
}
