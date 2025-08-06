package com.keenon.peanut.sample.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    
    // Cấu hình audio
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    
    // Cấu hình silence detection
    private static final int SILENCE_THRESHOLD = 1000; // Ngưỡng âm lượng im lặng
    private static final int SILENCE_DURATION_MS = 3000; // 3 giây im lặng
    private static final int CHECK_INTERVAL_MS = 100; // Kiểm tra mỗi 100ms
    
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private AudioRecorderCallback callback;
    private SocketManager socketManager;
    
    // Buffer để lưu audio data
    private ByteArrayOutputStream audioBuffer;
    private long lastSoundTime = 0;
    private boolean isSilent = false;
    
    public interface AudioRecorderCallback {
        void onAudioProcessed(String screenName, String message);
        void onError(String error);
        void onSilenceDetected();
        void onSoundDetected();
    }
    
    public AudioRecorder(SocketManager socketManager, AudioRecorderCallback callback) {
        this.socketManager = socketManager;
        this.callback = callback;
        this.audioBuffer = new ByteArrayOutputStream();
    }
    
    public void startRecording() {
        if (isRecording) {
            return;
        }
        
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("AudioRecord không thể khởi tạo");
            }
            
            audioRecord.startRecording();
            isRecording = true;
            audioBuffer.reset();
            lastSoundTime = System.currentTimeMillis();
            
            // Bắt đầu thread ghi âm
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    recordAudio();
                }
            });
            recordingThread.start();
            
            Log.d(TAG, "Bắt đầu ghi âm liên tục");
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi động ghi âm: " + e.getMessage());
            if (callback != null) {
                callback.onError("Không thể khởi động ghi âm: " + e.getMessage());
            }
        }
    }
    
    public void stopRecording() {
        if (!isRecording) {
            return;
        }
        
        isRecording = false;
        
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        
        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Thread bị interrupt");
            }
            recordingThread = null;
        }
        
        Log.d(TAG, "Dừng ghi âm");
    }
    
    private void recordAudio() {
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (isRecording) {
            int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);
            
            if (readSize > 0) {
                // Lưu audio data vào buffer
                audioBuffer.write(buffer, 0, readSize);
                
                // Kiểm tra âm lượng
                int volume = calculateVolume(buffer, readSize);
                
                if (volume > SILENCE_THRESHOLD) {
                    // Có âm thanh
                    if (isSilent) {
                        isSilent = false;
                        if (callback != null) {
                            callback.onSoundDetected();
                        }
                    }
                    lastSoundTime = System.currentTimeMillis();
                } else {
                    // Im lặng
                    if (!isSilent) {
                        isSilent = true;
                        if (callback != null) {
                            callback.onSilenceDetected();
                        }
                    }
                    
                    // Kiểm tra xem đã im lặng đủ 3 giây chưa
                    long silenceDuration = System.currentTimeMillis() - lastSoundTime;
                    if (silenceDuration >= SILENCE_DURATION_MS && audioBuffer.size() > 0) {
                        // Gửi audio data đến server
                        sendAudioToServer();
                        break; // Dừng ghi âm sau khi gửi
                    }
                }
            }
            
            try {
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private int calculateVolume(byte[] buffer, int size) {
        int sum = 0;
        for (int i = 0; i < size; i += 2) {
            if (i + 1 < size) {
                short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                sum += Math.abs(sample);
            }
        }
        return sum / (size / 2);
    }
    
    private void sendAudioToServer() {
        try {
            // Tạo file WAV từ PCM data
            byte[] wavData = createWavFile(audioBuffer.toByteArray());
            
            // Gửi audio data đến server
            if (socketManager != null && socketManager.isConnected()) {
                socketManager.sendAudioData(wavData);
                Log.d(TAG, "Đã gửi audio data đến server, kích thước: " + wavData.length + " bytes");
            } else {
                Log.w(TAG, "Socket không kết nối, không thể gửi audio");
                if (callback != null) {
                    callback.onError("Không thể kết nối đến server");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi gửi audio: " + e.getMessage());
            if (callback != null) {
                callback.onError("Lỗi gửi audio: " + e.getMessage());
            }
        }
    }
    
    private byte[] createWavFile(byte[] pcmData) throws IOException {
        // WAV header
        ByteArrayOutputStream wavStream = new ByteArrayOutputStream();
        
        // RIFF header
        wavStream.write("RIFF".getBytes());
        writeInt(wavStream, 36 + pcmData.length); // File size
        wavStream.write("WAVE".getBytes());
        
        // fmt chunk
        wavStream.write("fmt ".getBytes());
        writeInt(wavStream, 16); // Chunk size
        writeShort(wavStream, 1); // Audio format (PCM)
        writeShort(wavStream, 1); // Channels (mono)
        writeInt(wavStream, SAMPLE_RATE); // Sample rate
        writeInt(wavStream, SAMPLE_RATE * 2); // Byte rate
        writeShort(wavStream, 2); // Block align
        writeShort(wavStream, 16); // Bits per sample
        
        // data chunk
        wavStream.write("data".getBytes());
        writeInt(wavStream, pcmData.length); // Data size
        wavStream.write(pcmData); // Audio data
        
        return wavStream.toByteArray();
    }
    
    private void writeInt(ByteArrayOutputStream stream, int value) throws IOException {
        stream.write(value & 0xFF);
        stream.write((value >> 8) & 0xFF);
        stream.write((value >> 16) & 0xFF);
        stream.write((value >> 24) & 0xFF);
    }
    
    private void writeShort(ByteArrayOutputStream stream, int value) throws IOException {
        stream.write(value & 0xFF);
        stream.write((value >> 8) & 0xFF);
    }
    
    public boolean isRecording() {
        return isRecording;
    }
} 