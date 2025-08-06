package com.keenon.peanut.sample.test;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;

import java.util.Locale;

public class TestTTSActivity extends BaseActivity {
    
    private EditText etInputText;
    private Button btnSubmit;
    private TextToSpeech textToSpeech;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_tts);
        
        setTitle("Test Text To Speech");
        setButtonBack();
        
        // Khởi tạo views
        etInputText = findViewById(R.id.et_input_text);
        btnSubmit = findViewById(R.id.btn_submit);
        
        // Khởi tạo TextToSpeech
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Thiết lập ngôn ngữ tiếng Việt
                    int result = textToSpeech.setLanguage(new Locale("vi"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(TestTTSActivity.this, "Ngôn ngữ tiếng Việt không được hỗ trợ", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TestTTSActivity.this, "Text To Speech đã sẵn sàng", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TestTTSActivity.this, "Không thể khởi tạo Text To Speech", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Set click listener cho button submit
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etInputText.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(TestTTSActivity.this, "Vui lòng nhập text để phát âm thanh", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Phát âm thanh
                if (textToSpeech != null) {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Giải phóng TextToSpeech
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
} 