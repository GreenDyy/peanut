// File: PeanutSDKManager.java
package com.keenon.peanut.sample.util;

import android.content.Context;
import android.util.Log;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.sdk.component.runtime.PeanutRuntime;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.external.PeanutSDK.ErrorListener;

public class PeanutSDKManager {

    private static final String TAG = "PeanutSDKManager";
    // Biến cờ trạng thái để kiểm tra quá trình khởi tạo
    private static volatile boolean isSdkInitializing = false;
    private static volatile boolean isSdkInitialized = false;

    /**
     * Phương thức khởi tạo PeanutSDK.
     */
    public static void initializeSDK(Context context, String linkType, ErrorListener listener) {
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

        // Cấu hình SDK
        PeanutConfig.getConfig()
                .setLinkType(PeanutConstants.REMOTE_LINK_PROXY.equals(linkType) ?
                        PeanutConstants.LinkType.COAP : PeanutConstants.LinkType.COM_COAP)
                .setLinkIP(linkType)
                .enableLog(true)
                .setLogLevel(Log.DEBUG)
                .setAppId("bcb8ebc7f22345bebb378aead035cfb3")
                .setSecret("nPlQERTP4qJWimTp0+ZXXkM5ND93iEyWpM6eXAGIZ/HQmyEg8zN7x5tGLebwINKLYScXEjg5lhQBvt1QCODovm2gq7dsXAK4pgjBRK2OqQHxl4nvTjq2AX9Or6XrdfFfVgOiHqHqW0mw+qWGDJc1/EUBg3llLOzMNUiDqwPsXMZYs=")
                .enableUMLog(false);

        // Gọi hàm init() và xử lý kết quả
        PeanutSDK.getInstance().init(context.getApplicationContext(), new ErrorListener() {
            @Override
            public void onInit(int statusCode) {
                isSdkInitializing = false;
                if (statusCode == PeanutSDK.SDK_INIT_SUCCESS) {
                    isSdkInitialized = true;
                    Log.d(TAG, "✅ PeanutSDK khởi tạo thành công.");
                } else {
                    isSdkInitialized = false;
                    Log.e(TAG, "❌ PeanutSDK khởi tạo thất bại với mã: " + statusCode);
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
     * Kiểm tra xem Runtime đã được khởi tạo hay chưa.
     */
    public static boolean isRuntimeStarted() {
        return PeanutRuntime.getInstance() != null;
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
    public static void initializeRuntime(PeanutRuntime.Listener listener) {
        startRuntime(listener);
    }

    public static void releaseSDK() {
        Log.d(TAG, "Giải phóng PeanutSDK.");
        try {
            PeanutSDK.getInstance().release();
            isSdkInitialized = false;
            isSdkInitializing = false;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi giải phóng SDK: " + e.getMessage());
        }
    }
}