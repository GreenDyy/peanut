package com.keenon.peanut.sample.test;

/*
 * TestNavigationActivity - Sử dụng PeanutSDKManager để quản lý SDK
 * 
 * Lợi ích:
 * - Tránh duplicate code khởi tạo SDK
 * - Quản lý trạng thái SDK tập trung
 * - Tự động kiểm tra SDK đã được khởi tạo từ Main
 * - Sử dụng Runtime đã được start từ Main
 */

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.peanut.sample.util.PeanutSDKManager;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.robot.ApiCallback;
import com.keenon.sdk.robot.base.BaseResp;
import com.keenon.sdk.robot.model.bean.map.MapAreaBean;
import com.keenon.sdk.robot.model.bean.map.ZoneInfoBean;
import com.keenon.peanut.sample.manager.NavManager;
import com.keenon.peanut.sample.bean.MyPoint;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.route.RouteNode;
import java.util.List;
import java.util.ArrayList;
//charge
import com.keenon.sdk.component.charger.ChargerImpl;
import com.keenon.sdk.component.charger.PeanutCharger;

public class TestNavigationActivity extends BaseActivity {
    private Button connectButton, disconnectButton;
    private Button getPositionButton;
    private Button autoChargeButton;
    private Button manualChargeButton, cancelChargeButton;
    private Button setSpeedButton;
    private Button pauseNavigationButton, continueNavigationButton, stopNavigationSdkButton;
    private Button queryNearestTargetButton;
    private Button getAllTargetsButton, getGuideInfoButton;
    private Button getPointsButton;
    private Button getMapListButton, getMapDataButton;
    private Button checkWorkModeButton;
    private android.widget.EditText targetIdInput;
    private Button executeNavigationButton;
    private TextView logTextView;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_navigation);

        initViews();
        setupListeners();
        setButtonBack();

        // Test log ngay khi khởi tạo
        addLog("Init", "🚀 TestNavigationActivity đã khởi tạo");
        addLog("Init", "📱 Đang chuẩn bị kết nối đến robot...");

        // Khởi tạo SDK sử dụng PeanutSDKManager
        // Kiểm tra xem SDK đã được khởi tạo chưa, nếu rồi thì thôi, chưa thì mới init
        if (PeanutSDKManager.isInitialized() && PeanutSDKManager.isSDKAvailable()) {
            addLog("SDK", "ℹ️ PeanutSDK đã được khởi tạo trước đó");
            initRuntime();
        } else {
            // Sử dụng PeanutSDKManager để khởi tạo SDK
            PeanutSDKManager.initializeSDK(this, "192.168.1.100", new PeanutSDK.ErrorListener() {
                @Override
                public void onInit(int statusCode) {
                    if (statusCode == PeanutSDK.SDK_INIT_SUCCESS) {
                        addLog("SDK", "✅ PeanutSDK khởi tạo thành công");
                        initRuntime();
                    } else if (statusCode == PeanutSDK.SDK_INITIALIZING) {
                        addLog("SDK", "⏳ PeanutSDK đang trong quá trình khởi tạo...");
                    } else {
                        addLog("SDK", "❌ PeanutSDK khởi tạo thất bại với mã: " + statusCode);
                    }
                }
            });
        }

    }

    private void initViews() {
        connectButton = findViewById(R.id.btn_connect);
        disconnectButton = findViewById(R.id.btn_disconnect);
        getPositionButton = findViewById(R.id.btn_get_position);
        autoChargeButton = findViewById(R.id.btn_auto_charge);
        manualChargeButton = findViewById(R.id.btn_manual_charge);
        cancelChargeButton = findViewById(R.id.btn_cancel_charge);

        // SdkHelper buttons
        setSpeedButton = findViewById(R.id.btn_set_speed);
        pauseNavigationButton = findViewById(R.id.btn_pause_navigation);
        continueNavigationButton = findViewById(R.id.btn_continue_navigation);
        stopNavigationSdkButton = findViewById(R.id.btn_stop_navigation_sdk);
        queryNearestTargetButton = findViewById(R.id.btn_query_nearest_target);
        getAllTargetsButton = findViewById(R.id.btn_get_all_targets);
        getGuideInfoButton = findViewById(R.id.btn_get_guide_info);
        getPointsButton = findViewById(R.id.btn_get_points);
        getMapListButton = findViewById(R.id.btn_get_map_list);
        getMapDataButton = findViewById(R.id.btn_get_map_data);
        checkWorkModeButton = findViewById(R.id.btn_check_workmode);
        targetIdInput = findViewById(R.id.et_target_id);
        executeNavigationButton = findViewById(R.id.btn_execute_navigation);
        logTextView = findViewById(R.id.tv_log);
    }

    private void setupListeners() {
        connectButton.setOnClickListener(v -> connectToRobot());
        disconnectButton.setOnClickListener(v -> disconnectFromRobot());
        getPositionButton.setOnClickListener(v -> getPosition());
        autoChargeButton.setOnClickListener(v -> autoCharge());
        manualChargeButton.setOnClickListener(v -> manualCharge());
        cancelChargeButton.setOnClickListener(v -> cancelCharge());
        checkWorkModeButton.setOnClickListener(v -> checkWorkMode());
        // SdkHelper listeners
        setSpeedButton.setOnClickListener(v -> setRobotSpeed());
        pauseNavigationButton.setOnClickListener(v -> pauseNavigation());
        continueNavigationButton.setOnClickListener(v -> continueNavigation());
        stopNavigationSdkButton.setOnClickListener(v -> stopNavigationSdk());
        queryNearestTargetButton.setOnClickListener(v -> queryNearestTarget());
        getAllTargetsButton.setOnClickListener(v -> getAllTargets());
        getGuideInfoButton.setOnClickListener(v -> getGuideInfo());
        getPointsButton.setOnClickListener(v -> getPoints());
        getMapListButton.setOnClickListener(v -> getMapList());
        getMapDataButton.setOnClickListener(v -> getMapData());
        executeNavigationButton.setOnClickListener(v -> executeNavigation());
    }

    private void initRuntime() {
        try {
            if (PeanutRuntime.getInstance() != null) {
                // Sử dụng PeanutSDKManager để khởi tạo Runtime
                PeanutSDKManager.startRuntime(new PeanutRuntime.Listener() {
                    @Override
                    public void onEvent(int event, Object obj) {
                        addLog("Runtime", "Event: " + event + ", Content: " + obj);
                    }

                    @Override
                    public void onHealth(Object content) {
                        addLog("Health", "Status: " + content);
                    }

                    @Override
                    public void onHeartbeat(Object content) {
                        // Không log heartbeat để tránh spam
                    }
                });
                addLog("Runtime", "✅ PeanutRuntime khởi tạo thành công");
            } else {
                addLog("Runtime", "❌ PeanutRuntime không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Khởi tạo Runtime thất bại: " + e.getMessage());
        }
    }

    private void connectToRobot() {
        addLog("Connection", "🔄 Đang kết nối đến robot...");
        isConnected = true;
        updateButtonStates();
        addLog("Connection", "✅ Đã kết nối thành công đến robot");
    }

    private void disconnectFromRobot() {
        addLog("Connection", "🔄 Đang ngắt kết nối...");
        isConnected = false;
        updateButtonStates();
        addLog("Connection", "✅ Đã ngắt kết nối khỏi robot");
    }

    private void updateButtonStates() {
        boolean enabled = isConnected;
        getPositionButton.setEnabled(enabled);
        autoChargeButton.setEnabled(enabled);
        manualChargeButton.setEnabled(enabled);
        cancelChargeButton.setEnabled(enabled);

        // SdkHelper buttons
        setSpeedButton.setEnabled(enabled);
        pauseNavigationButton.setEnabled(enabled);
        continueNavigationButton.setEnabled(enabled);
        stopNavigationSdkButton.setEnabled(enabled);
        queryNearestTargetButton.setEnabled(enabled);
        getAllTargetsButton.setEnabled(enabled);
        getGuideInfoButton.setEnabled(enabled);
        getPointsButton.setEnabled(enabled);
        getMapListButton.setEnabled(enabled);
        getMapDataButton.setEnabled(enabled);
        checkWorkModeButton.setEnabled(enabled);
        executeNavigationButton.setEnabled(enabled);
    }

    // ==================== ĐIỀU HƯỚNG CƠ BẢN ====================

    private void getPosition() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Position", "🔄 Đang lấy vị trí hiện tại...");
        try {
            // Sử dụng RuntimeComponent để lấy vị trí
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().runtime() != null) {
                PeanutSDK.getInstance().runtime().getRobotPosition(
                        new ApiCallback<BaseResp<com.keenon.sdk.robot.model.bean.map.PositionInfoBean>>() {
                            @Override
                            public void onSuccess(
                                    BaseResp<com.keenon.sdk.robot.model.bean.map.PositionInfoBean> result) {
                                if (result != null && result.getData() != null) {
                                    addLog("Position", "✅ Lấy vị trí hiện tại thành công");
                                    addLog("Position", "📍 Vị trí: " + result.getData().toString());
                                } else {
                                    addLog("Position", "⚠️ Không có dữ liệu vị trí");
                                }
                            }

                            @Override
                            public void onSuccess(String requestId,
                                    BaseResp<com.keenon.sdk.robot.model.bean.map.PositionInfoBean> result) {
                                onSuccess(result);
                            }

                            @Override
                            public void onFail(ApiError error) {
                                addLog("Error", "❌ Lấy vị trí hiện tại thất bại: " + error.toString());
                            }
                        });
            } else {
                addLog("Error", "❌ Runtime component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi lấy vị trí: " + e.getMessage());
        }
    }

    // ==================== AUTO CHARGE ====================

    private void autoCharge() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("AutoCharge", "🔄 Đang tìm dock sạc trong map...");
        try {
            // if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().map() != null)
            // {
            // // Lấy danh sách zones từ map
            // PeanutSDK.getInstance().map().mapZoneList(new
            // ApiCallback<BaseResp<MapAreaBean>>() {
            // @Override
            // public void onSuccess(BaseResp<MapAreaBean> result) {
            // if (result.getData() != null) {
            // MapAreaBean mapArea = result.getData();
            // if (mapArea.getZoneArray() != null && !mapArea.getZoneArray().isEmpty()) {
            // // Tìm dock sạc (type = charger)
            // ZoneInfoBean dockZone = null;
            // for (ZoneInfoBean zone : mapArea.getZoneArray()) {
            // if ("charger".equals(zone.getZoneType()) ||
            // "dock".equals(zone.getZoneType())) {
            // dockZone = zone;
            // break;
            // }
            // }
            //
            // if (dockZone != null) {
            // addLog("AutoCharge", "✅ Tìm thấy dock sạc: " + dockZone.getName() + " (ID="
            // + dockZone.getZoneId() + ")");
            // // Gọi auto charge với zoneId của dock
            // startAutoCharge(dockZone.getZoneId());
            // } else {
            // addLog("Warning", "⚠️ Không tìm thấy dock sạc trong map");
            // addLog("Info", "💡 Vui lòng kiểm tra map có được cấu hình dock sạc chưa");
            // }
            // } else {
            // addLog("Warning", "⚠️ Không có zones nào trong map");
            // }
            // } else {
            // addLog("Warning", "⚠️ Không có dữ liệu zones");
            // }
            // }
            //
            // @Override
            // public void onSuccess(String s, BaseResp<MapAreaBean> mapAreaBeanBaseResp) {
            //
            // }
            //
            // @Override
            // public void onFail(ApiError apiError) {
            // addLog("Error", "❌ Lấy danh sách zones thất bại: " + apiError.toString());
            // }
            // });
            // } else {
            // addLog("Error", "❌ Map component không khả dụng");
            // }
            // new char nè
            ChargerImpl.getInstance().autoCharge();
            // new charge nữa nè
            // PeanutCharger charger = new PeanutCharger.Builder()
            // .setPile(pointId) // dock id
            // .setListener(listener)
            // .build();
            //
            // charger.performAction(PeanutCharger.CHARGE_ACTION_AUTO);
            // lỏ lắm

        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi tìm dock sạc: " + e.getMessage());
        }
    }

    private void startAutoCharge(int dockPointId) {
        addLog("AutoCharge", "🔄 Đang bắt đầu auto charge với dock ID: " + dockPointId);
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().autoCharge(dockPointId, new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("AutoCharge", "✅ Auto charge thành công");
                        addLog("AutoCharge", "🔋 Kết quả: " + result.getData());
                        addLog("AutoCharge", "📍 Robot sẽ tự tìm đường về dock sạc");
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Auto charge thất bại: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Battery component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi gọi auto charge: " + e.getMessage());
        }
    }

    private void manualCharge() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("ManualCharge", "🔄 Đang bắt đầu manual charge...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().manualCharge(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("ManualCharge", "✅ Manual charge thành công");
                        addLog("ManualCharge", "🔋 Kết quả: " + result.getData());
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Manual charge thất bại: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Battery component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi gọi manual charge: " + e.getMessage());
        }
    }

    private void cancelCharge() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("CancelCharge", "🔄 Đang hủy bỏ sạc pin...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().battery() != null) {
                PeanutSDK.getInstance().battery().stopCharge(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("CancelCharge", "✅ Hủy bỏ sạc pin thành công");
                        addLog("CancelCharge", "🔋 Kết quả: " + result.getData());
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Hủy bỏ sạc pin thất bại: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Battery component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi hủy bỏ sạc pin: " + e.getMessage());
        }
    }

    // ==================== WORK MODE ====================
    
    private void checkWorkMode() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("WorkMode", "🔍 Đang kiểm tra work mode hiện tại...");
        try {
            if (PeanutRuntime.getInstance() != null && PeanutRuntime.getInstance().getRuntimeInfo() != null) {
                int workMode = PeanutRuntime.getInstance().getRuntimeInfo().getWorkMode();
                String modeDescription = getWorkModeDescription(workMode);
                
                addLog("WorkMode", "✅ Work Mode hiện tại: " + workMode + " (" + modeDescription + ")");
                
                // Hiển thị thông tin chi tiết về work mode
                addLog("WorkMode", "📋 Thông tin Work Mode:");
                addLog("WorkMode", "   • Mã Mode: " + workMode);
                addLog("WorkMode", "   • Mô tả: " + modeDescription);
                addLog("WorkMode", "   • Trạng thái: " + (workMode >= 0 ? "Hoạt động" : "Chưa thiết lập"));
                
                // Thêm thông tin về các work mode có thể có
                addLog("WorkMode", "📚 Các Work Mode có sẵn:");
                addLog("WorkMode", "   • -1: Chưa thiết lập");
                addLog("WorkMode", "   • 0: Hoạt động bình thường");
                addLog("WorkMode", "   • 1: Kiểm tra sản xuất");
                addLog("WorkMode", "   • 2: Chế độ Debug");
                
            } else {
                addLog("Error", "❌ Runtime hoặc RuntimeInfo không khả dụng");
            }
            
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi kiểm tra work mode: " + e.getMessage());
            addLog("Debug", "Loại exception: " + e.getClass().getSimpleName());
        }
    }
    
    private String getWorkModeDescription(int mode) {
        // Từ decompiled code: work mode values
        switch (mode) {
            case -1: return "Chưa thiết lập";
            case 0: return "Hoạt động bình thường";
            case 1: return "Kiểm tra sản xuất";
            case 2: return "Chế độ Debug";
            default: return "Không xác định (" + mode + ")";
        }
    }

    // ==================== SDK HELPER ====================

    private void setRobotSpeed() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Speed", "🔄 Đang đặt tốc độ robot: 80%");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().setSpeed(80, new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Speed", "✅ Đặt tốc độ thành công: 80%");
                        addLog("Speed", "📊 Kết quả: " + result);
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Đặt tốc độ thất bại: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Navigation component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi đặt tốc độ: " + e.getMessage());
        }
    }

    private void pauseNavigation() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Navigation", "🔄 Đang tạm dừng điều hướng...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().pause(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Navigation", "✅ Tạm dừng điều hướng thành công");
                        addLog("Navigation", "📊 Kết quả: " + result.getData());
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Tạm dừng điều hướng thất bại: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Navigation component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi tạm dừng điều hướng: " + e.getMessage());
        }
    }

    private void continueNavigation() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Navigation", "🔄 Đang tiếp tục điều hướng...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().resume(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Navigation", "✅ Tiếp tục điều hướng thành công");
                        addLog("Navigation", "📊 Kết quả: " + result.getData());
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Tiếp tục điều hướng thất bại: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Navigation component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi tiếp tục điều hướng: " + e.getMessage());
        }
    }

    private void stopNavigationSdk() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Navigation", "🔄 Đang dừng điều hướng (SdkHelper)...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().stop(new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Navigation", "✅ Dừng điều hướng thành công");
                        addLog("Navigation", "📊 Kết quả: " + result.getData());
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Dừng điều hướng thất bại: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Navigation component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi dừng điều hướng: " + e.getMessage());
        }
    }

    private void queryNearestTarget() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("NearestTarget", "🔄 Đang tìm điểm đích gần nhất...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().runtime() != null) {
                PeanutSDK.getInstance().runtime().getLocationId(
                        new ApiCallback<BaseResp<com.keenon.sdk.robot.model.bean.map.NearestTargetBean>>() {
                            @Override
                            public void onSuccess(
                                    BaseResp<com.keenon.sdk.robot.model.bean.map.NearestTargetBean> result) {
                                addLog("NearestTarget", "✅ Tìm điểm đích gần nhất thành công");
                                if (result.getData() != null) {
                                    addLog("NearestTarget", "📍 Điểm gần nhất: " + result.getData().getDstId());
                                } else {
                                    addLog("NearestTarget", "⚠️ Không có điểm đích gần nhất");
                                }
                            }

                            @Override
                            public void onSuccess(String requestId,
                                    BaseResp<com.keenon.sdk.robot.model.bean.map.NearestTargetBean> result) {
                                onSuccess(result);
                            }

                            @Override
                            public void onFail(ApiError error) {
                                addLog("Error", "❌ Tìm điểm đích gần nhất thất bại: " + error.toString());
                            }
                        });
            } else {
                addLog("Error", "❌ Runtime component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi tìm điểm đích gần nhất: " + e.getMessage());
        }
    }

    // ==================== MAP POINTS & NAVIGATION ====================

    private void getPoints() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Map", "🔄 Đang lấy danh sách zones trong map...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().map() != null) {
                PeanutSDK.getInstance().map().mapZoneList(new ApiCallback<BaseResp<MapAreaBean>>() {
                    @Override
                    public void onSuccess(BaseResp<MapAreaBean> result) {
                        addLog("Map", "✅ Lấy danh sách zones thành công");
                        if (result.getData() != null) {
                            MapAreaBean mapArea = result.getData();
                            if (mapArea.getZoneArray() != null && !mapArea.getZoneArray().isEmpty()) {
                                addLog("Map", "📍 Tổng số zones: " + mapArea.getZoneArray().size());
                                for (ZoneInfoBean zone : mapArea.getZoneArray()) {
                                    addLog("Map", "   - Zone: " + zone.getName() + " | ID=" + zone.getZoneId()
                                            + " | Type=" + zone.getZoneType());
                                }
                            } else {
                                addLog("Map", "⚠️ Không có zones nào trong map");
                            }
                        } else {
                            addLog("Map", "⚠️ Không có dữ liệu zones");
                        }
                    }

                    @Override
                    public void onSuccess(String s, BaseResp<MapAreaBean> mapAreaBeanBaseResp) {

                    }

                    @Override
                    public void onFail(ApiError apiError) {
                        addLog("Error", "❌ Lấy danh sách zones thất bại: " + apiError.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Map component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi lấy danh sách zones: " + e.getMessage());
        }
    }

    private void executeNavigation() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        String targetIdStr = targetIdInput.getText().toString().trim();
        if (targetIdStr.isEmpty()) {
            addLog("Warning", "⚠️ Vui lòng nhập Target ID");
            return;
        }

        try {
            int pointId = Integer.parseInt(targetIdStr);
            if (pointId <= 0) {
                addLog("Warning", "⚠️ Target ID phải là số nguyên dương");
                return;
            }

            addLog("Navigation", "🎯 Bắt đầu điều hướng với Target ID: " + pointId);
            navigateToPointId(pointId);

        } catch (NumberFormatException e) {
            addLog("Error", "❌ Target ID không hợp lệ. Vui lòng nhập số nguyên");
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi xử lý Target ID: " + e.getMessage());
        }
    }

    private void navigateToPointId(int pointId) {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Navigation", "🔄 Đang điều hướng tới điểm đích ID=" + pointId + "...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().setTarget(pointId, 0, new ApiCallback<BaseResp<String>>() {
                    @Override
                    public void onSuccess(BaseResp<String> result) {
                        addLog("Navigation", "✅ Điều hướng tới điểm đích ID=" + pointId + " thành công");
                        addLog("Navigation", "📊 Kết quả: " + result.getData());
                        addLog("Navigation", "🔢 Mã phản hồi: " + result.getCode());
                    }

                    @Override
                    public void onSuccess(String requestId, BaseResp<String> result) {
                        onSuccess(result);
                    }

                    @Override
                    public void onFail(ApiError error) {
                        addLog("Error", "❌ Điều hướng tới điểm đích ID=" + pointId + " thất bại: " + error.toString());
                    }
                });
            } else {
                addLog("Error", "❌ Navigation component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi điều hướng tới điểm đích ID=" + pointId + ": " + e.getMessage());
        }
    }

    // ==================== NAVIGATION COMPONENT ====================

    private void getAllTargets() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Navigation", "🔄 Đang lấy tất cả điểm đích...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().getAllTargets(
                        new ApiCallback<BaseResp<com.keenon.sdk.robot.model.bean.navigation.NavigationPointBean>>() {
                            @Override
                            public void onSuccess(
                                    BaseResp<com.keenon.sdk.robot.model.bean.navigation.NavigationPointBean> result) {
                                addLog("Navigation", "✅ Lấy tất cả điểm đích thành công");
                                if (result.getData() != null) {
                                    addLog("Navigation", "📍 Dữ liệu điểm đích: " + result.getData().toString());
                                } else {
                                    addLog("Navigation", "⚠️ Không có dữ liệu điểm đích");
                                }
                            }

                            @Override
                            public void onSuccess(String requestId,
                                    BaseResp<com.keenon.sdk.robot.model.bean.navigation.NavigationPointBean> result) {
                                onSuccess(result);
                            }

                            @Override
                            public void onFail(ApiError error) {
                                addLog("Error", "❌ Lấy tất cả điểm đích thất bại: " + error.toString());
                            }
                        });
            } else {
                addLog("Error", "❌ Navigation component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi lấy tất cả điểm đích: " + e.getMessage());
        }
    }

    private void getGuideInfo() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("Navigation", "🔄 Đang lấy thông tin hướng dẫn...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().navigation() != null) {
                PeanutSDK.getInstance().navigation().getGuideInfo(
                        new ApiCallback<BaseResp<com.keenon.sdk.robot.model.bean.navigation.GuideInfoBean>>() {
                            @Override
                            public void onSuccess(
                                    BaseResp<com.keenon.sdk.robot.model.bean.navigation.GuideInfoBean> result) {
                                addLog("Navigation", "✅ Lấy thông tin hướng dẫn thành công");
                                if (result.getData() != null) {
                                    addLog("Navigation", "📍 Dữ liệu hướng dẫn: " + result.getData().toString());
                                } else {
                                    addLog("Navigation", "⚠️ Không có dữ liệu hướng dẫn");
                                }
                            }

                            @Override
                            public void onSuccess(String requestId,
                                    BaseResp<com.keenon.sdk.robot.model.bean.navigation.GuideInfoBean> result) {
                                onSuccess(result);
                            }

                            @Override
                            public void onFail(ApiError error) {
                                addLog("Error", "❌ Lấy thông tin hướng dẫn thất bại: " + error.toString());
                            }
                        });
            } else {
                addLog("Error", "❌ Navigation component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi lấy thông tin hướng dẫn: " + e.getMessage());
        }
    }

    // ==================== MAP COMPONENT ====================

    private void getMapList() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("MapList", "🔄 Đang lấy danh sách bản đồ...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().map() != null) {
                PeanutSDK.getInstance().map()
                        .mapList(new ApiCallback<BaseResp<com.keenon.sdk.robot.model.bean.map.MapListBean>>() {
                            @Override
                            public void onSuccess(BaseResp<com.keenon.sdk.robot.model.bean.map.MapListBean> result) {
                                addLog("MapList", "✅ Lấy danh sách bản đồ thành công");
                                if (result.getData() != null) {
                                    com.keenon.sdk.robot.model.bean.map.MapListBean mapList = result.getData();
                                    addLog("MapList", "🗺️ Tổng số bản đồ: "
                                            + (mapList.getMapArray() != null ? mapList.getMapArray().size() : 0));
                                    addLog("MapList", "📊 Dữ liệu bản đồ: " + mapList.toString());

                                    if (mapList.getMapArray() != null && !mapList.getMapArray().isEmpty()) {
                                        addLog("MapList", "📍 Danh sách bản đồ:");
                                        for (com.keenon.sdk.robot.model.bean.map.MapInfoBean map : mapList
                                                .getMapArray()) {
                                            addLog("MapList", "   - " + map.getName() + " | Floor=" + map.getFloor()
                                                    + " | Type=" + map.getType() + " | MD5=" + map.getMd5());
                                        }
                                    } else {
                                        addLog("MapList", "⚠️ Không có bản đồ nào");
                                    }
                                } else {
                                    addLog("MapList", "⚠️ Không có dữ liệu bản đồ");
                                }
                            }

                            @Override
                            public void onSuccess(String requestId,
                                    BaseResp<com.keenon.sdk.robot.model.bean.map.MapListBean> result) {
                                onSuccess(result);
                            }

                            @Override
                            public void onFail(ApiError error) {
                                addLog("Error", "❌ Lấy danh sách bản đồ thất bại: " + error.toString());
                            }
                        });
            } else {
                addLog("Error", "❌ Map component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi lấy danh sách bản đồ: " + e.getMessage());
        }
    }

    private void getMapData() {
        if (!isConnected) {
            addLog("Warning", "⚠️ Vui lòng kết nối đến robot trước");
            return;
        }

        addLog("MapData", "🔄 Đang lấy dữ liệu bản đồ hiện tại...");
        try {
            if (PeanutSDK.getInstance() != null && PeanutSDK.getInstance().map() != null) {
                // Lấy dữ liệu bản đồ với ID=1 và name="current"
                PeanutSDK.getInstance().map()
                        .mapDataGet(new ApiCallback<BaseResp<com.keenon.sdk.robot.model.bean.map.MapDataBean>>() {
                            @Override
                            public void onSuccess(BaseResp<com.keenon.sdk.robot.model.bean.map.MapDataBean> result) {
                                addLog("MapData", "✅ Lấy dữ liệu bản đồ thành công");
                                if (result.getData() != null) {
                                    com.keenon.sdk.robot.model.bean.map.MapDataBean mapData = result.getData();
                                    addLog("MapData", "🗺️ Thông tin bản đồ:");
                                    addLog("MapData", "📊 Dữ liệu chi tiết: " + mapData.toString());

                                    // Hiển thị thông tin cơ bản nếu có
                                    try {
                                        // Thử gọi các method có thể có
                                        if (mapData.getClass().getMethod("getName") != null) {
                                            addLog("MapData", "   - Name: " + mapData.toString());
                                        }
                                    } catch (NoSuchMethodException e) {
                                        addLog("MapData",
                                                "   - Cấu trúc MapDataBean: " + mapData.getClass().getSimpleName());
                                    }
                                } else {
                                    addLog("MapData", "⚠️ Không có dữ liệu bản đồ");
                                }
                            }

                            @Override
                            public void onSuccess(String requestId,
                                    BaseResp<com.keenon.sdk.robot.model.bean.map.MapDataBean> result) {
                                onSuccess(result);
                            }

                            @Override
                            public void onFail(ApiError error) {
                                addLog("Error", "❌ Lấy dữ liệu bản đồ thất bại: " + error.toString());
                            }
                        }, 1, "current"); // ID=1, name="current"
            } else {
                addLog("Error", "❌ Map component không khả dụng");
            }
        } catch (Exception e) {
            addLog("Error", "❌ Lỗi khi lấy dữ liệu bản đồ: " + e.getMessage());
        }
    }

    private String getStateName(int state) {
        switch (state) {
            case Navigation.STATE_DESTINATION:
                return "Đã đến đích";
            case Navigation.STATE_COLLISION:
                return "Phát hiện va chạm";
            case Navigation.STATE_BLOCKED:
                return "Bị chặn";
            case Navigation.STATE_BLOCKING:
                return "Đang chờ";
            default:
                return "Trạng thái " + state;
        }
    }

    private void addLog(String tag, String message) {
        String timeStamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        final String logMessage = "\n[" + timeStamp + "] " + tag + ": " + message;

        runOnUiThread(() -> {
            try {
                // Thêm log message
                logTextView.append(logMessage);

                // Log ra console để debug
                android.util.Log.d("TestNavigation", logMessage);

                // Tự động cuộn xuống cuối một cách an toàn
                logTextView.post(() -> {
                    try {
                        if (logTextView.getLayout() != null) {
                            final int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount())
                                    - logTextView.getHeight();
                            if (scrollAmount > 0) {
                                logTextView.scrollTo(0, scrollAmount);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("TestNavigation", "Lỗi khi scroll: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("TestNavigation", "Lỗi khi thêm log: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnected) {
            disconnectFromRobot();
        }
    }
}
