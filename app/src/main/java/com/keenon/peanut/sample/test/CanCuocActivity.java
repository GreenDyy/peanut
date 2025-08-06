package com.keenon.peanut.sample.test;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CanCuocActivity extends BaseActivity {
    
    private TextView tvQueueNumber, tvTitle;
    private ImageView ivQRCode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);
        
        setTitle("Căn Cước Công Dân");
        setButtonBack();
        
        // Khởi tạo views
        tvQueueNumber = findViewById(R.id.tv_queue_number);
        tvTitle = findViewById(R.id.tv_title);
        ivQRCode = findViewById(R.id.iv_qr_code);
        
        // Set title
        tvTitle.setText("Căn Cước Công Dân");
        
        // Tạo số thứ tự ngẫu nhiên
        Random random = new Random();
        int queueNumber = random.nextInt(999) + 1;
        tvQueueNumber.setText("Số thứ tự: " + String.format("%03d", queueNumber));
        
        // Tạo mã QR
        String qrData = "CAN_CUOC_" + String.format("%03d", queueNumber) + "_" + System.currentTimeMillis();
        generateQRCode(qrData);
    }
    
    private void generateQRCode(String data) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 400, 400, hints);
            
            Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565);
            for (int x = 0; x < 400; x++) {
                for (int y = 0; y < 400; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            ivQRCode.setImageBitmap(bitmap);
            
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
} 