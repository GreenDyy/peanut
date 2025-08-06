package com.keenon.peanut.sample.test;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecorderActivity extends BaseActivity {

    private static final String TAG = "AudioRecorderActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private Button btnRecord;
    private Button btnPlay;
    private Button btnStop;
    private TextView tvStatus;
    private TextView tvDuration;
    
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String outputFile;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private long startTime = 0;
    private long endTime = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recorder);
        
        setTitle("Audio Recorder");
        setButtonBack();
        
        initViews();
        checkPermissions();
        setupClickListeners();
    }
    
    private void initViews() {
        btnRecord = findViewById(R.id.btn_record);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        tvStatus = findViewById(R.id.tv_status);
        tvDuration = findViewById(R.id.tv_duration);
        
        // Thêm nút kiểm tra thư mục
        Button btnCheckDirectory = findViewById(R.id.btn_check_directory);
        btnCheckDirectory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDirectoryInfo();
            }
        });
        
        // Ban đầu chỉ hiển thị nút Record
        btnPlay.setEnabled(false);
        btnStop.setEnabled(false);
        tvStatus.setText("Sẵn sàng ghi âm");
        
        // Hiển thị thông tin thư mục lưu file
        showSaveDirectoryInfo();
    }
    
    private void setupClickListeners() {
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecording();
                }
            }
        });
        
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    startPlaying();
                }
            }
        });
        
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopRecording();
                } else if (isPlaying) {
                    stopPlaying();
                }
            }
        });
    }
    
    private void showSaveDirectoryInfo() {
        String saveDirectory = Environment.DIRECTORY_DOWNLOADS; // Cùng với startRecording()
        File directory = Environment.getExternalStoragePublicDirectory(saveDirectory);
        
        String directoryPath = directory.getAbsolutePath();
        String directoryName = directory.getName();
        
        // Log thông tin chi tiết
        Log.d(TAG, "=== THÔNG TIN THƯ MỤC LƯU FILE ===");
        Log.d(TAG, "Thư mục: " + directoryName);
        Log.d(TAG, "Đường dẫn đầy đủ: " + directoryPath);
        Log.d(TAG, "Thư mục tồn tại: " + directory.exists());
        Log.d(TAG, "Có thể ghi: " + directory.canWrite());
        Log.d(TAG, "================================");
        
        // Hiển thị thông tin trong Toast
        String info = "Thư mục: " + directoryName + "\nĐường dẫn: " + directoryPath;
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
        
        // Cập nhật status
        tvStatus.setText("Thư mục lưu: " + directoryName);
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
    
    private void startRecording() {
        try {
            // Tạo tên file với timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            
            // BẠN CÓ THỂ THAY ĐỔI THƯ MỤC LƯU FILE Ở ĐÂY
            // Các lựa chọn:
            // 1. Downloads: Environment.DIRECTORY_DOWNLOADS
            // 2. Music: Environment.DIRECTORY_MUSIC  
            // 3. Documents: Environment.DIRECTORY_DOCUMENTS
            // 4. Pictures: Environment.DIRECTORY_PICTURES
            // 5. Movies: Environment.DIRECTORY_MOVIES
            // 6. DCIM: Environment.DIRECTORY_DCIM
            
            String saveDirectory = Environment.DIRECTORY_DOWNLOADS; // Thay đổi ở đây
            File directory = Environment.getExternalStoragePublicDirectory(saveDirectory);
            
            // Tạo thư mục nếu chưa tồn tại
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            outputFile = directory.getAbsolutePath() + "/audio_" + timeStamp + ".mp3";
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(outputFile);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            startTime = System.currentTimeMillis();
            
            btnRecord.setEnabled(false);
            btnPlay.setEnabled(false);
            btnStop.setEnabled(true);
            tvStatus.setText("Đang ghi âm...");
            tvDuration.setText("00:00");
            
            Toast.makeText(this, "Bắt đầu ghi âm", Toast.LENGTH_SHORT).show();
            
            // Cập nhật thời gian ghi âm
            updateRecordingTime();
            
        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi bắt đầu ghi âm: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi bắt đầu ghi âm", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                
                isRecording = false;
                endTime = System.currentTimeMillis();
                long duration = (endTime - startTime) / 1000;
                
                btnRecord.setEnabled(true);
                btnPlay.setEnabled(true);
                btnStop.setEnabled(false);
                tvStatus.setText("Ghi âm hoàn thành");
                tvDuration.setText(String.format("%02d:%02d", duration / 60, duration % 60));
                
                Toast.makeText(this, "Ghi âm hoàn thành: " + outputFile, Toast.LENGTH_LONG).show();
                
            } catch (IllegalStateException e) {
                Log.e(TAG, "Lỗi khi dừng ghi âm: " + e.getMessage());
                Toast.makeText(this, "Lỗi khi dừng ghi âm", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void startPlaying() {
        if (outputFile != null && new File(outputFile).exists()) {
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(outputFile);
                mediaPlayer.prepare();
                mediaPlayer.start();
                
                isPlaying = true;
                
                btnRecord.setEnabled(false);
                btnPlay.setEnabled(false);
                btnStop.setEnabled(true);
                tvStatus.setText("Đang phát audio...");
                
                Toast.makeText(this, "Bắt đầu phát audio", Toast.LENGTH_SHORT).show();
                
                // Lắng nghe sự kiện hoàn thành
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopPlaying();
                    }
                });
                
            } catch (IOException e) {
                Log.e(TAG, "Lỗi khi phát audio: " + e.getMessage());
                Toast.makeText(this, "Lỗi khi phát audio", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Không tìm thấy file audio", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            
            isPlaying = false;
            
            btnRecord.setEnabled(true);
            btnPlay.setEnabled(true);
            btnStop.setEnabled(false);
            tvStatus.setText("Phát audio đã dừng");
            
            Toast.makeText(this, "Đã dừng phát audio", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateRecordingTime() {
        if (isRecording) {
            long currentTime = System.currentTimeMillis();
            long duration = (currentTime - startTime) / 1000;
            tvDuration.setText(String.format("%02d:%02d", duration / 60, duration % 60));
            
            // Cập nhật mỗi giây
            btnRecord.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isRecording) {
                        updateRecordingTime();
                    }
                }
            }, 1000);
        }
    }
    
    @Override
    protected void onDestroy() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
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
                Toast.makeText(this, "Cần quyền để ghi âm", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 