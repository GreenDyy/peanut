package com.keenon.peanut.sample.test;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DialogActivity extends BaseActivity {

    private static final String TAG = "DialogActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int SPEECH_REQUEST_CODE = 456;
    
    private Button btnStartDialog;
    private Button btnStopDialog;
    private Button btnClearDialog;
    private TextView tvDialogContent;
    private ScrollView scrollView;
    
    private MediaRecorder mediaRecorder;
    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private String outputFile;
    private StringBuilder dialogHistory = new StringBuilder();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        
        setTitle("Dialog - Cuộc hội thoại");
        setButtonBack();
        
        initViews();
        checkPermissions();
        setupClickListeners();
        initializeSpeechRecognizer();
    }
    
    private void initViews() {
        btnStartDialog = findViewById(R.id.btn_start_dialog);
        btnStopDialog = findViewById(R.id.btn_stop_dialog);
        btnClearDialog = findViewById(R.id.btn_clear_dialog);
        tvDialogContent = findViewById(R.id.tv_dialog_content);
        scrollView = findViewById(R.id.scroll_view);
        
        // Ban đầu chỉ hiển thị nút Start
        btnStopDialog.setEnabled(false);
        btnClearDialog.setEnabled(false);
        
        // Hiển thị thông tin ban đầu
        tvDialogContent.setText("Chào mừng đến với Dialog!\n\nNhấn 'Bắt đầu hội thoại' để bắt đầu ghi âm và chuyển đổi thành văn bản.\n\n");
    }
    
    private void setupClickListeners() {
        btnStartDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startDialogRecording();
                }
            }
        });
        
        btnStopDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopDialogRecording();
                }
            }
        });
        
        btnClearDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearDialog();
            }
        });
    }
    
    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Sẵn sàng nhận diện giọng nói");
                    addToDialog("🎤 Sẵn sàng nhận diện...", true);
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Bắt đầu nói");
                    addToDialog("🎤 Đang nghe...", true);
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Có thể thêm visual feedback ở đây
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "Kết thúc nói");
                    addToDialog("🎤 Đã nghe xong, đang xử lý...", true);
                }

                @Override
                public void onError(int error) {
                    String errorMessage = "❌ Lỗi nhận diện: ";
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            errorMessage += "Lỗi audio";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            errorMessage += "Lỗi client";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            errorMessage += "Thiếu quyền";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            errorMessage += "Lỗi mạng";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            errorMessage += "Hết thời gian mạng";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            errorMessage += "Không nhận diện được";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            errorMessage += "Bộ nhận diện đang bận";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            errorMessage += "Lỗi server";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorMessage += "Hết thời gian nói";
                            break;
                        default:
                            errorMessage += "Lỗi không xác định";
                            break;
                    }
                    addToDialog(errorMessage, true);
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String recognizedText = matches.get(0);
                        addToDialog("👤 Bạn: " + recognizedText, false);
                        
                        // Tự động phản hồi đơn giản
                        String response = generateSimpleResponse(recognizedText);
                        addToDialog("🤖 Bot: " + response, false);
                        
                        // Tiếp tục lắng nghe
                        startSpeechRecognition();
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        } else {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_LONG).show();
        }
    }
    
    private void startDialogRecording() {
        try {
            // Tạo tên file với timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String saveDirectory = Environment.DIRECTORY_DOWNLOADS;
            File directory = Environment.getExternalStoragePublicDirectory(saveDirectory);
            
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            outputFile = directory.getAbsolutePath() + "/dialog_" + timeStamp + ".mp3";
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(outputFile);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            
            btnStartDialog.setEnabled(false);
            btnStopDialog.setEnabled(true);
            btnClearDialog.setEnabled(true);
            
            addToDialog("🎙️ Bắt đầu cuộc hội thoại...", true);
            
            // Bắt đầu nhận diện giọng nói
            startSpeechRecognition();
            
            Toast.makeText(this, "Bắt đầu cuộc hội thoại", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi bắt đầu ghi âm: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi bắt đầu ghi âm", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopDialogRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (IllegalStateException e) {
                Log.e(TAG, "Lỗi khi dừng ghi âm: " + e.getMessage());
            }
        }
        
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        
        isRecording = false;
        
        btnStartDialog.setEnabled(true);
        btnStopDialog.setEnabled(false);
        
        addToDialog("🔚 Kết thúc cuộc hội thoại", true);
        Toast.makeText(this, "Đã kết thúc cuộc hội thoại", Toast.LENGTH_SHORT).show();
    }
    
    private void startSpeechRecognition() {
        if (speechRecognizer != null && isRecording) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói gì đó...");
            
            speechRecognizer.startListening(intent);
        }
    }
    
    private void addToDialog(String message, boolean isSystem) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String formattedMessage = "[" + timestamp + "] " + message + "\n\n";
        
        dialogHistory.append(formattedMessage);
        tvDialogContent.setText(dialogHistory.toString());
        
        // Tự động scroll xuống cuối
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
    
    private void clearDialog() {
        dialogHistory = new StringBuilder();
        tvDialogContent.setText("Chào mừng đến với Dialog!\n\nNhấn 'Bắt đầu hội thoại' để bắt đầu ghi âm và chuyển đổi thành văn bản.\n\n");
        btnClearDialog.setEnabled(false);
        Toast.makeText(this, "Đã xóa cuộc hội thoại", Toast.LENGTH_SHORT).show();
    }
    
    private String generateSimpleResponse(String userInput) {
        String input = userInput.toLowerCase();
        
        if (input.contains("xin chào") || input.contains("hello") || input.contains("hi")) {
            return "Xin chào! Rất vui được gặp bạn!";
        } else if (input.contains("tạm biệt") || input.contains("goodbye") || input.contains("bye")) {
            return "Tạm biệt! Hẹn gặp lại bạn!";
        } else if (input.contains("cảm ơn") || input.contains("thank")) {
            return "Không có gì! Rất vui được giúp đỡ bạn!";
        } else if (input.contains("tên") || input.contains("name")) {
            return "Tôi là Bot Assistant, rất vui được làm quen với bạn!";
        } else if (input.contains("thời tiết") || input.contains("weather")) {
            return "Tôi không thể kiểm tra thời tiết, nhưng bạn có thể xem trên ứng dụng thời tiết!";
        } else if (input.contains("giờ") || input.contains("time")) {
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            return "Bây giờ là " + currentTime;
        } else if (input.contains("ngày") || input.contains("date")) {
            String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            return "Hôm nay là ngày " + currentDate;
        } else if (input.contains("khỏe") || input.contains("health")) {
            return "Cảm ơn bạn đã hỏi! Tôi luôn khỏe mạnh và sẵn sàng phục vụ!";
        } else {
            return "Tôi đã hiểu bạn nói: \"" + userInput + "\". Bạn có thể nói thêm gì không?";
        }
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    protected void onDestroy() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền đã được cấp", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần quyền để ghi âm và nhận diện giọng nói", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 