package com.keenon.peanut.sample.util;

/**
 * Cấu hình server xử lý audio
 * Thay đổi IP và port tại đây
 */
public class ServerConfig {

    // Cấu hình mặc định cho TCP Socket
    // Sử dụng 10.0.2.2 cho Android Emulator hoặc IP thật của máy cho thiết bị
    public static final String DEFAULT_HOST = "10.0.2.2"; // Emulator -> PC
    public static final int DEFAULT_PORT = 8080;

    // Cấu hình hiện tại (có thể thay đổi runtime)
    private static String currentHost = DEFAULT_HOST;
    private static int currentPort = DEFAULT_PORT;

    // Timeout cho socket connection (milliseconds)
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 10000;

    // Buffer size cho audio data
    public static final int AUDIO_BUFFER_SIZE = 4096;

    public static String getHost() {
        return currentHost;
    }

    public static int getPort() {
        return currentPort;
    }

    public static void setHost(String host) {
        currentHost = host;
    }

    public static void setPort(int port) {
        currentPort = port;
    }

    public static void setConfig(String host, int port) {
        currentHost = host;
        currentPort = port;
    }

    /**
     * Reset về cấu hình mặc định
     */
    public static void resetToDefault() {
        currentHost = DEFAULT_HOST;
        currentPort = DEFAULT_PORT;
    }

    /**
     * Kiểm tra cấu hình hợp lệ
     */
    public static boolean isValidConfig() {
        return currentHost != null && !currentHost.isEmpty() && currentPort > 0 && currentPort <= 65535;
    }

    /**
     * Lấy URL đầy đủ của server
     */
    public static String getServerUrl() {
        return "tcp://" + currentHost + ":" + currentPort;
    }
}