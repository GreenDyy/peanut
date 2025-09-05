// File: PeanutSDKManager.java
package com.keenon.peanut.sample.util;

import android.content.Context;
import android.util.Log;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.peanut.common.robot.TopicManager;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.external.PeanutSDK.ErrorListener;
import com.keenon.peanut.common.resouorce.manager.RosMapDownLoadManager;
import com.keenon.peanut.common.resouorce.manager.PeanutResourceManager;
import com.keenon.peanut.common.resouorce.resourceBean.Resource;
import com.keenon.peanut.common.config.ConfigManager;

public class PeanutSDKManager {

    private static final String TAG = "PeanutSDKManager";
    // Biến cờ trạng thái để kiểm tra quá trình khởi tạo
    private static volatile boolean isSdkInitializing = false;
    private static volatile boolean isSdkInitialized = false;
    
    // Interface cho Map Loading Callback
    public interface MapLoadListener {
        void onMapLoadStart();
        void onMapLoadSuccess();
        void onMapLoadError(String error);
        void onMapLoadProgress(int progress);
    }

    /**
     * Phương thức khởi tạo PeanutSDK.
     */

    public static void initializeSDK(Context context, ErrorListener listener) {
        // Kiểm tra cờ trạng thái để tránh khởi tạo lại
        if (isSdkInitialized) {
            Log.d(TAG, "PeanutSDK đã được khởi tạo. Bỏ qua.");
            if (listener != null) {
                listener.onInit(PeanutSDK.SDK_INIT_SUCCESS);
            }
            return;
        }

        if (isSdkInitializing) {
            Log.d(TAG, "PeanutSDK đang trong quá trình khởi tạo. Vui lòng đợi.");
            return;
        }

        Log.d(TAG, "Đang khởi tạo PeanutSDK...");
        isSdkInitializing = true;

        // Cấu hình SDK cho T10, T10 thì toàn cổng COM3, còn con khác thì xem file @PeanutManager của nó
        PeanutConfig.getConfig()
                .setLinkType(PeanutConstants.LinkType.COAP)
                .setLinkIP(PeanutConstants.REMOTE_LINK_PROXY)
                .setLinkPort(5683)
                .enableLog(true)
                .setLogLevel(Log.DEBUG)
                .setAppId("bcb8ebc7f22345bebb378aead035cfb3")
                .setSecret(
                        "nPlQERTP4qJWimTp0+ZXXkM5ND93iEyWpM6eXAGIZ/HQmyEg8zN7x5tGLebwINKLYScXEjg5lhQBvt1QCODovm2gq7dsXAK4pgjBRK2OqQHxl4nvTjq2AX9Or6XrdfFfVgOiHqHqW0mw+qWGDJc1/EUBg3llLOzMNUiDqwPsXMZYs=")
                .enableUMLog(false)
                .setLinkCOM(PeanutConstants.COM3)
                .setEmotionLinkCOM(PeanutConstants.COM3)
                .setLightLinkCOM(PeanutConstants.COM3)
                .setDoorLinkCOM(PeanutConstants.COM3)
                .setKc01EmotionLinkCOM(PeanutConstants.COM3);
        Log.d(TAG, "PeanutConfig configured. Initializing PeanutSDK...");

//        if (RobotMachineTypeHelper.match("T10", "T10 S,T10S", "T11")) {
//            PeanutConfig.getConfig().setEmotionLinkCOM(PeanutConstants.COM3).setLightLinkCOM(PeanutConstants.COM3).setLinkCOM(PeanutConstants.COM3).setDoorLinkCOM(PeanutConstants.COM3).setKc01EmotionLinkCOM(PeanutConstants.COM3);
//        } else if (RobotMachineTypeHelper.match("T3,T2 Pro,T2P", "T9 Pro,T5 Pro_Pickup,T9P")) {
//            PeanutConfig.getConfig().setDoorLinkCOM(PeanutConstants.COM_USB0);
//        }


        // Gọi hàm init() và xử lý kết quả
        PeanutSDK.getInstance().init(context.getApplicationContext(), new ErrorListener() {
            @Override
            public void onInit(int statusCode) {
                isSdkInitializing = false;
                switch (statusCode) {
                    case PeanutSDK.SDK_INIT_SUCCESS:
                        isSdkInitialized = true;
                        Log.d(TAG, "✅ PeanutSDK khởi tạo thành công.");

                        // init thành công thì subscribe các topic
                        // TopicManager topicManager = new TopicManager();
                        // topicManager.subscribeTopics();

                        // Khởi tạo Resource Manager sau khi SDK init thành công
//                        initializeResourceManager(context);
                        break;

                    case PeanutSDK.SDK_INITIALIZING:
                        isSdkInitialized = false;
                        Log.d(TAG, "PeanutSDK đang khởi tạo...");
                        break;

                    default:
                        isSdkInitialized = false;
                        Log.e(TAG, "❌ PeanutSDK khởi tạo thất bại với mã: " + statusCode);
                        break;
                }

                if (listener != null) {
                    listener.onInit(statusCode);
                }
            }
        });
    }

    /**
     * Kiểm tra xem SDK đã được khởi tạo thành công hay chưa.
     */
    public static boolean isInitialized() {
        return isSdkInitialized;
    }

    /**
     * Kiểm tra xem SDK instance có khả dụng hay không.
     */
    public static boolean isSDKAvailable() {
        return PeanutSDK.getInstance() != null;
    }

    /**
     * Lấy SDK instance hiện tại (nếu đã khởi tạo).
     */
    public static PeanutSDK getSDKInstance() {
        if (isSdkInitialized) {
            return PeanutSDK.getInstance();
        }
        return null;
    }

    /**
     * Khởi tạo PeanutRuntime với listener.
     */
    public static void startRuntime(PeanutRuntime.Listener listener) {
        if (isSdkInitialized) {
            Log.d(TAG, "Bắt đầu PeanutRuntime.");
            PeanutRuntime.getInstance().start(listener);
        } else {
            Log.e(TAG, "SDK chưa được khởi tạo. Không thể bắt đầu Runtime.");
        }
    }

    /**
     * Khởi tạo PeanutRuntime với listener (alias cho startRuntime).
     */

    /**
     * Khởi tạo Resource Manager
     */
    //ko dùng nua
    private static void initializeResourceManager(Context context) {
        try {
            Log.d(TAG, "Đang khởi tạo Resource Manager...");
            // Khởi tạo ConfigManager nếu cần
            if (ConfigManager.getInstance() == null) {
                Log.d(TAG, "ConfigManager chưa được khởi tạo");
            }
            Log.d(TAG, "✅ Resource Manager đã sẵn sàng");
        } catch (Exception e) {
            Log.e(TAG, "❌ Lỗi khi khởi tạo Resource Manager: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra xem có cần load map hay không
     */
    public static boolean shouldLoadMap() {
        try {
            // Kiểm tra machine location type
            int machineLocationType = ConfigManager.getInstance().getMachineLocationType();
            Log.d(TAG, "Machine Location Type: " + machineLocationType);
            return machineLocationType == 1; // ROS mode
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi kiểm tra shouldLoadMap: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load map với resource cụ thể
     */
//    public static void loadMap(Context context, Resource resource, MapLoadListener listener) {
//        if (!isSdkInitialized) {
//            Log.e(TAG, "SDK chưa được khởi tạo. Không thể load map.");
//            if (listener != null) {
//                listener.onMapLoadError("SDK chưa được khởi tạo");
//            }
//            return;
//        }
//
//        if (!shouldLoadMap()) {
//            Log.d(TAG, "Không cần load map cho machine type này");
//            if (listener != null) {
//                listener.onMapLoadSuccess();
//            }
//            return;
//        }
//
//        if (resource == null) {
//            Log.e(TAG, "Resource null. Không thể load map.");
//            if (listener != null) {
//                listener.onMapLoadError("Resource null");
//            }
//            return;
//        }
//
//        Log.d(TAG, "🔄 Bắt đầu load map...");
//        if (listener != null) {
//            listener.onMapLoadStart();
//        }
//
//        try {
//            // Khởi tạo RosMapDownLoadManager để update resource
//            RosMapDownLoadManager.getInstance().init(context);
//
//            // Upload map
//            RosMapDownLoadManager.getInstance().uploadRosMap(resource);
//
//            Log.d(TAG, "✅ Map load đã được khởi tạo thành công");
//            if (listener != null) {
//                listener.onMapLoadSuccess();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Lỗi khi load map: " + e.getMessage());
//            if (listener != null) {
//                listener.onMapLoadError(e.getMessage());
//            }
//        }
//    }

    /**
     * Kiểm tra và load map tự động
     */
    public static void checkAndLoadMap(Context context, MapLoadListener listener) {
        if (!isSdkInitialized) {
            Log.e(TAG, "SDK chưa được khởi tạo. Không thể check map.");
            if (listener != null) {
                listener.onMapLoadError("SDK chưa được khởi tạo");
            }
            return;
        }

        try {
            Log.d(TAG, "🔍 Kiểm tra map cần load...");
            
            // Kiểm tra resource cần apply resource có sẳn trong ROS
            PeanutResourceManager.getInstance().checkNeedApplyResource();
            
            // Kiểm tra ROS location map scene
            PeanutResourceManager.getInstance().checkRosLocationMapScene();
            
            Log.d(TAG, "✅ Map check hoàn tất");
            if (listener != null) {
                listener.onMapLoadSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Lỗi khi check map: " + e.getMessage());
            if (listener != null) {
                listener.onMapLoadError(e.getMessage());
            }
        }
    }

    /**
     * Lấy trạng thái map loading
     */
    public static boolean isMapLoading() {
        try {
            return PeanutResourceManager.getInstance().isUpdateResource();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi kiểm tra map loading status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Dừng map loading
     */
    public static void stopMapLoading() {
        try {
            Log.d(TAG, "Dừng map loading...");
            PeanutResourceManager.getInstance().setIsUpdateResource(false);
            Log.d(TAG, "✅ Map loading đã được dừng");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi dừng map loading: " + e.getMessage());
        }
    }

    public static void releaseSDK() {
        Log.d(TAG, "Giải phóng PeanutSDK.");
        try {
            // Dừng map loading trước khi release
            stopMapLoading();
            
            PeanutSDK.getInstance().release();
            isSdkInitialized = false;
            isSdkInitializing = false;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi giải phóng SDK: " + e.getMessage());
        }
    }
}