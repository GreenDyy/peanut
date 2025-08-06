package com.keenon.peanut.sample.test;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.SocketManager;
import com.keenon.peanut.sample.util.AudioRecorder;

import java.util.ArrayList;
import java.util.Locale;

public class MainAppActivity extends BaseActivity {

    private ImageView leftEye, rightEye;
    private Handler handler;
    private Runnable blinkRunnable;
    private boolean isBlinking = true;

    private FloatingActionButton voiceButton;
    private SpeechRecognizer speechRecognizer;
    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int PERMISSION_REQUEST_CODE = 101;
    
    // Socket Manager để kết nối với server xử lý audio
    private SocketManager socketManager;
    private AudioRecorder audioRecorder;
    private boolean useSocketProcessing = true; // Flag để bật/tắt xử lý qua socket

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);

        setTitle("Robot Assistant");
        setButtonBack();

        // Khởi tạo views
        leftEye = findViewById(R.id.left_eye);
        rightEye = findViewById(R.id.right_eye);
        voiceButton = findViewById(R.id.voice_button);

        // Khởi tạo handler cho animation nhấp nháy
        handler = new Handler();
        blinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBlinking) {
                    // Nhấp nháy mắt
                    leftEye.setVisibility(View.INVISIBLE);
                    rightEye.setVisibility(View.INVISIBLE);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            leftEye.setVisibility(View.VISIBLE);
                            rightEye.setVisibility(View.VISIBLE);
                        }
                    }, 200); // Mở mắt sau 200ms

                    // Lặp lại sau 3 giây
                    handler.postDelayed(this, 3000);
                }
            }
        };

        // Bắt đầu animation nhấp nháy
        handler.post(blinkRunnable);

        // Set click listener cho toàn bộ màn hình
        View rootView = findViewById(R.id.robot_face_container);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến màn hình chức năng
                Intent intent = new Intent(MainAppActivity.this, FunctionMenuActivity.class);
                startActivity(intent);
            }
        });

        // Set click listener cho voice button
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioRecorder != null && audioRecorder.isRecording()) {
                    // Nếu đang ghi âm thì dừng
                    audioRecorder.stopRecording();
                    voiceButton.setImageResource(R.drawable.ic_mic_off); // Icon mic tắt
                } else {
                    // Bắt đầu ghi âm liên tục
                    startContinuousRecording();
                }
            }
        });

        Toast.makeText(this, "Nhấn vào màn hình để mở menu chức năng hoặc nhấn nút voice để nói", Toast.LENGTH_LONG)
                .show();
        
        // Khởi tạo Socket Manager và Audio Recorder
        initSocketManager();
        initAudioRecorder();
    }

    private void startVoiceRecognition() {
        // Kiểm tra quyền RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    PERMISSION_REQUEST_CODE);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói tên chức năng bạn muốn mở...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể khởi động voice recognition", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, bắt đầu voice recognition
                startVoiceRecognition();
            } else {
                Toast.makeText(this, "Cần quyền ghi âm để sử dụng tính năng voice", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0).toLowerCase();
                
                if (useSocketProcessing && socketManager != null && socketManager.isConnected()) {
                    // Gửi text command đến server để xử lý
                    Toast.makeText(this, "Gửi lệnh đến server: " + spokenText, Toast.LENGTH_SHORT).show();
                    socketManager.sendTextCommand(spokenText);
                } else {
                    // Fallback về xử lý local
                    processVoiceCommand(spokenText);
                }
            }
        }
    }

    private void initSocketManager() {
        socketManager = new SocketManager(new SocketManager.SocketCallback() {
            @Override
            public void onCommandReceived(String command) {
                // Xử lý command nhận được từ server
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainAppActivity.this, 
                            "Server trả về: " + command, Toast.LENGTH_SHORT).show();
                        processVoiceCommand(command);
                    }
                });
            }

            @Override
            public void onAudioProcessed(String screenName, String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainAppActivity.this, 
                            message, Toast.LENGTH_LONG).show();
                        
                        // Chuyển đến màn hình tương ứng
                        navigateToScreen(screenName);
                        
                        // Reset icon mic
                        voiceButton.setImageResource(R.drawable.ic_mic_off);
                    }
                });
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connected) {
                            Toast.makeText(MainAppActivity.this, 
                                "Đã kết nối với server xử lý audio", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainAppActivity.this, 
                                "Mất kết nối với server", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainAppActivity.this, 
                            "Lỗi socket: " + error, Toast.LENGTH_SHORT).show();
                        // Fallback về xử lý local nếu có lỗi
                        useSocketProcessing = false;
                    }
                });
            }
        });
        
        // Kết nối với server
        socketManager.connect();
    }

    private void initAudioRecorder() {
        audioRecorder = new AudioRecorder(socketManager, new AudioRecorder.AudioRecorderCallback() {
            @Override
            public void onAudioProcessed(String screenName, String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainAppActivity.this, 
                            "Server: " + message, Toast.LENGTH_SHORT).show();
                        
                        // Chuyển đến màn hình tương ứng
                        navigateToScreen(screenName);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainAppActivity.this, 
                            "Lỗi audio: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onSilenceDetected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Có thể thêm visual feedback khi im lặng
                        Toast.makeText(MainAppActivity.this, 
                            "Đang im lặng...", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onSoundDetected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Có thể thêm visual feedback khi có âm thanh
                        Toast.makeText(MainAppActivity.this, 
                            "Đang nghe...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void startContinuousRecording() {
        // Kiểm tra quyền RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    PERMISSION_REQUEST_CODE);
            return;
        }

        if (audioRecorder != null) {
            audioRecorder.startRecording();
            voiceButton.setImageResource(R.drawable.ic_mic_on); // Icon mic đang ghi âm
            Toast.makeText(this, "Bắt đầu ghi âm liên tục. Nói và im lặng 3 giây để gửi.", Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToScreen(String screenName) {
        Intent intent = null;
        
        switch (screenName.toLowerCase()) {
            case "can_cuoc":
            case "căn cước công dân":
                intent = new Intent(this, CanCuocActivity.class);
                break;
            case "tam_tru":
            case "khai báo tạm trú":
                intent = new Intent(this, TamTruActivity.class);
                break;
            case "xac_nhan_cu_tru":
            case "xin giấy xác nhận cư trú":
                intent = new Intent(this, XacNhanCuTruActivity.class);
                break;
            case "test_tts":
            case "test tts":
                intent = new Intent(this, TestTTSActivity.class);
                break;
            case "menu":
            case "menu chức năng":
                intent = new Intent(this, FunctionMenuActivity.class);
                break;
            default:
                Toast.makeText(this, "Không tìm thấy màn hình: " + screenName, Toast.LENGTH_SHORT).show();
                return;
        }
        
        if (intent != null) {
            startActivity(intent);
        }
    }

    private void processVoiceCommand(String command) {
        Toast.makeText(this, "Bạn đã nói: " + command, Toast.LENGTH_SHORT).show();

        Intent intent = null;

        // Xử lý các từ khóa cho "Căn cước công dân"
        if (command.contains("căn cước") || command.contains("can cuoc") || command.contains("cccd")
                || command.contains("căn cước công dân") || command.contains("can cuoc cong dan")
                || command.contains("chứng minh") || command.contains("chung minh")) {
            intent = new Intent(this, CanCuocActivity.class);
        }
        // Xử lý các từ khóa cho "Khai báo tạm trú"
        else if (command.contains("tạm trú") || command.contains("tam tru") || command.contains("khai báo")
                || command.contains("khai báo tạm trú") || command.contains("khai bao tam tru")
                || command.contains("tạm vắng") || command.contains("tam vang")) {
            intent = new Intent(this, TamTruActivity.class);
        }
        // Xử lý các từ khóa cho "Xin giấy xác nhận cư trú"
        else if (command.contains("xác nhận") || command.contains("xac nhan") || command.contains("cư trú")
                || command.contains("cu tru") || command.contains("giấy xác nhận") || command.contains("giay xac nhan")
                || command.contains("xác nhận cư trú") || command.contains("xac nhan cu tru")) {
            intent = new Intent(this, XacNhanCuTruActivity.class);
        }
        // Xử lý các từ khóa cho "Test TTS"
        else if (command.contains("test") || command.contains("tts") || command.contains("text to speech")
                || command.contains("phát âm") || command.contains("phat am") || command.contains("đọc text")
                || command.contains("doc text") || command.contains("nói") || command.contains("noi")) {
            intent = new Intent(this, TestTTSActivity.class);
        }
        // Xử lý các từ khóa cho "Menu chức năng"
        else if (command.contains("menu") || command.contains("chức năng") || command.contains("chuc nang")
                || command.contains("danh sách") || command.contains("danh sach") || command.contains("tất cả")
                || command.contains("tat ca") || command.contains("quay lại") || command.contains("quay lai")) {
            intent = new Intent(this, FunctionMenuActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this,
                    "Không hiểu lệnh. Vui lòng thử lại với các từ khóa: căn cước, tạm trú, xác nhận, test tts, menu",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng animation khi activity bị hủy
        isBlinking = false;
        if (handler != null) {
            handler.removeCallbacks(blinkRunnable);
        }
        
        // Đóng socket connection
        if (socketManager != null) {
            socketManager.disconnect();
        }
        
        // Dừng audio recorder
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
        }
    }
}