package com.keenon.peanut.sample.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;

public class FunctionMenuActivity extends BaseActivity {
    
    private Button btnCanCuoc, btnTamTru, btnXacNhanCuTru, btnTestTTS;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function_menu);
        
        setTitle("Menu Chức Năng");
        setButtonBack();
        
        // Khởi tạo views
        btnCanCuoc = findViewById(R.id.btn_can_cuoc);
        btnTamTru = findViewById(R.id.btn_tam_tru);
        btnXacNhanCuTru = findViewById(R.id.btn_xac_nhan_cu_tru);
        btnTestTTS = findViewById(R.id.btn_test_tts);
        
        // Set click listeners
        btnCanCuoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FunctionMenuActivity.this, CanCuocActivity.class);
                startActivity(intent);
            }
        });
        
        btnTamTru.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FunctionMenuActivity.this, TamTruActivity.class);
                startActivity(intent);
            }
        });
        
        btnXacNhanCuTru.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FunctionMenuActivity.this, XacNhanCuTruActivity.class);
                startActivity(intent);
            }
        });
        
        btnTestTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FunctionMenuActivity.this, TestTTSActivity.class);
                startActivity(intent);
            }
        });
    }
} 