# Tính Năng Voice Recognition - MainAppActivity

## Mô tả
Tính năng voice recognition cho phép người dùng sử dụng giọng nói để điều hướng đến các màn hình chức năng khác nhau trong ứng dụng.

## Cách sử dụng

### 1. Khởi động Voice Recognition
- Mở màn hình MainAppActivity (Robot Assistant)
- Nhấn vào nút FloatingActionButton có icon microphone ở góc dưới bên phải
- Hệ thống sẽ yêu cầu quyền ghi âm nếu chưa được cấp

### 2. Các lệnh voice được hỗ trợ

#### Mở màn hình "Căn cước công dân":
- "căn cước"
- "can cuoc"
- "cccd"
- "căn cước công dân"
- "can cuoc cong dan"
- "chứng minh"
- "chung minh"

#### Mở màn hình "Khai báo tạm trú":
- "tạm trú"
- "tam tru"
- "khai báo"
- "khai báo tạm trú"
- "khai bao tam tru"
- "tạm vắng"
- "tam vang"

#### Mở màn hình "Xin giấy xác nhận cư trú":
- "xác nhận"
- "xac nhan"
- "cư trú"
- "cu tru"
- "giấy xác nhận"
- "giay xac nhan"
- "xác nhận cư trú"
- "xac nhan cu tru"

#### Mở màn hình "Menu chức năng":
- "menu"
- "chức năng"
- "chuc nang"
- "danh sách"
- "danh sach"
- "tất cả"
- "tat ca"
- "quay lại"
- "quay lai"

## Cấu trúc code

### MainAppActivity.java
- **startVoiceRecognition()**: Khởi động voice recognition
- **onRequestPermissionsResult()**: Xử lý kết quả yêu cầu quyền
- **onActivityResult()**: Xử lý kết quả voice recognition
- **processVoiceCommand()**: Phân tích và xử lý lệnh voice

### Layout (activity_main_app.xml)
- FloatingActionButton với icon microphone
- Vị trí: góc dưới bên phải màn hình
- Màu: colorAccent (#FF4081)

### Permissions
- `RECORD_AUDIO`: Quyền ghi âm (đã có trong AndroidManifest.xml)

### Dependencies
- `com.google.android.material:material:1.2.1`: Material Design Components cho FloatingActionButton

## Lưu ý
1. Cần cấp quyền ghi âm khi lần đầu sử dụng
2. Voice recognition hoạt động tốt nhất trong môi trường yên tĩnh
3. Hỗ trợ tiếng Việt và tiếng Anh (không dấu)
4. Có thể nói từ khóa ngắn gọn hoặc đầy đủ

## Troubleshooting
- Nếu không hiểu lệnh: Thử nói rõ ràng hơn hoặc sử dụng từ khóa khác
- Nếu không có phản hồi: Kiểm tra quyền ghi âm trong Settings
- Nếu lỗi: Restart ứng dụng và thử lại 