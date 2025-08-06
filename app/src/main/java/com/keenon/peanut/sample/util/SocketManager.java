package com.keenon.peanut.sample.util;

import android.util.Log;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.keenon.peanut.sample.util.ServerConfig;

public class SocketManager {
    private static final String TAG = "SocketManager";
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ExecutorService executor;
    private boolean isConnected = false;
    private SocketCallback callback;
    
    public interface SocketCallback {
        void onCommandReceived(String command);
        void onAudioProcessed(String screenName, String message);
        void onConnectionStatusChanged(boolean connected);
        void onError(String error);
    }
    
    public SocketManager(SocketCallback callback) {
        this.callback = callback;
        this.executor = Executors.newCachedThreadPool();
    }
    
    public void connect() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ServerConfig.getHost(), ServerConfig.getPort());
                    socket.setSoTimeout(ServerConfig.READ_TIMEOUT);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    isConnected = true;
                    
                    if (callback != null) {
                        callback.onConnectionStatusChanged(true);
                    }
                    
                    Log.d(TAG, "Kết nối socket thành công");
                    
                    // Bắt đầu lắng nghe response từ server
                    startListening();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi kết nối socket: " + e.getMessage());
                    isConnected = false;
                    if (callback != null) {
                        callback.onError("Không thể kết nối đến server: " + e.getMessage());
                        callback.onConnectionStatusChanged(false);
                    }
                }
            }
        });
    }
    
    public void sendAudioData(byte[] audioData) {
        if (!isConnected || out == null) {
            if (callback != null) {
                callback.onError("Chưa kết nối đến server");
            }
            return;
        }
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Gửi audio data dưới dạng base64
                    String audioBase64 = android.util.Base64.encodeToString(audioData, android.util.Base64.DEFAULT);
                    out.println("AUDIO:" + audioBase64);
                    Log.d(TAG, "Đã gửi audio data đến server");
                    
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi gửi audio data: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("Lỗi gửi audio: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    public void sendTextCommand(String textCommand) {
        if (!isConnected || out == null) {
            if (callback != null) {
                callback.onError("Chưa kết nối đến server");
            }
            return;
        }
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    out.println("TEXT:" + textCommand);
                    Log.d(TAG, "Đã gửi text command: " + textCommand);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi gửi text command: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("Lỗi gửi text: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    private void startListening() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String response;
                    while (isConnected && (response = in.readLine()) != null) {
                        Log.d(TAG, "Nhận response từ server: " + response);
                        
                        if (response.startsWith("COMMAND:")) {
                            String command = response.substring(8); // Bỏ "COMMAND:"
                            if (callback != null) {
                                callback.onCommandReceived(command);
                            }
                        } else if (response.startsWith("AUDIO_RESULT:")) {
                            // Format: AUDIO_RESULT:screen_name|message
                            String result = response.substring(13); // Bỏ "AUDIO_RESULT:"
                            String[] parts = result.split("\\|", 2);
                            if (parts.length == 2) {
                                String screenName = parts[0];
                                String message = parts[1];
                                if (callback != null) {
                                    callback.onAudioProcessed(screenName, message);
                                }
                            }
                        } else if (response.startsWith("ERROR:")) {
                            String error = response.substring(6); // Bỏ "ERROR:"
                            if (callback != null) {
                                callback.onError(error);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi đọc response: " + e.getMessage());
                    isConnected = false;
                    if (callback != null) {
                        callback.onConnectionStatusChanged(false);
                    }
                }
            }
        });
    }
    
    public void disconnect() {
        isConnected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi đóng socket: " + e.getMessage());
        }
        
        if (callback != null) {
            callback.onConnectionStatusChanged(false);
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void setServerConfig(String host, int port) {
        ServerConfig.setConfig(host, port);
    }
} 