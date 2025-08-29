# Test Navigation Component

## Mô tả
Màn hình test này được tạo để kiểm tra các chức năng của NavigationComponent trong PeanutSDK.

## Các chức năng chính

### 1. Thông tin bản đồ
- **Lấy thông tin bản đồ**: Hiển thị thông tin vị trí robot từ RuntimeComponent
- **Bản đồ hiện tại**: Hiển thị thông tin runtime và bản đồ đang được sử dụng

### 2. Điều hướng cơ bản
- **Điều hướng đến điểm**: Sử dụng NavManager để điều hướng đến điểm test
- **Dừng điều hướng**: Dừng quá trình điều hướng hiện tại
- **Vị trí hiện tại**: Lấy vị trí hiện tại của robot từ RuntimeComponent
- **Trạng thái điều hướng**: Kiểm tra trạng thái điều hướng từ NavManager

### 3. Điều hướng nâng cao
- **Điều hướng đến pose**: Sử dụng NavManager để điều hướng đến pose với thông tin chi tiết
- **Điều hướng đến goal**: Sử dụng NavManager để điều hướng đến goal với manual control
- **Đặt chế độ điều hướng**: Đặt tốc độ điều hướng thông qua NavManager
- **Lấy chế độ điều hướng**: Kiểm tra thông tin điều hướng hiện tại từ NavManager

### 4. Test NavManager (API cũ)
- **Test NavManager**: Khởi tạo và test NavManager với 2 điểm đích
- **Test Simple Navigation**: Test điều hướng đơn giản

### 5. Auto Charge
- **Auto Charge**: Tìm dock sạc trong map và bắt đầu tự động sạc pin
- **Auto Charge (30s)**: Tìm dock sạc trong map và bắt đầu tự động sạc pin với timeout 30 giây
- **Manual Charge**: Sạc pin thủ công
- **Cancel Charge**: Hủy bỏ quá trình sạc pin hiện tại
- **Adapter Charge**: Sạc pin bằng adapter (sử dụng motor().enable())

### 6. SdkHelper Navigation
- **Test SdkHelper**: Hiển thị danh sách các chức năng SdkHelper có sẵn
- **Set Speed**: Đặt tốc độ robot (80%)
- **Set Accel**: Đặt chế độ ổn định (stable mode = 1)
- **Lock Motor**: Khóa motor
- **Unlock Motor**: Mở khóa motor
- **Nearest Target**: Tìm điểm đích gần nhất
- **Start Nav**: Bắt đầu điều hướng đến điểm 1
- **Pause**: Tạm dừng điều hướng
- **Continue**: Tiếp tục điều hướng
- **Stop Nav**: Dừng điều hướng
- **Plan Multi Path**: Lập kế hoạch đường đi đa điểm (1, 2, 3)
- **Get Points**: Lấy danh sách tất cả zones trong map
- **Navigate to Table 1**: Điều hướng tới Bàn 1 (ID=101)
- **Find Dock**: Tìm dock sạc trong map

## Cách sử dụng

1. **Khởi động ứng dụng** và chọn "Test Navigation Component"
2. **Kết nối robot** bằng nút "Kết nối"
3. **Test các chức năng** theo thứ tự:
   - Đầu tiên test thông tin bản đồ
   - Sau đó test điều hướng cơ bản
   - Cuối cùng test điều hướng nâng cao và NavManager

## Lưu ý quan trọng

### API thực tế được sử dụng
- **RuntimeComponent**: Lấy thông tin vị trí robot và runtime
- **NavManager**: Điều hướng đa điểm với PeanutNavigation
- **MotorComponent**: Điều khiển chuyển động cơ bản và adapter charge
- **MapComponent**: Lấy danh sách points và tìm dock sạc
- **BatteryComponent**: 
  - `autoCharge(pointId, callback)` - Tự động sạc pin với pointId của dock sạc
  - `manualCharge(callback)` - Sạc pin thủ công
  - `stopCharge(callback)` - Dừng sạc pin
- **NavigationComponent** (SdkHelper):
  - `setSpeed(speed, callback)` - Đặt tốc độ robot
  - `setStableMode(mode, callback)` - Đặt chế độ ổn định
  - `setTarget(dstId, mode, callback)` - Đặt điểm đích
  - `pause(callback)` - Tạm dừng điều hướng
  - `resume(callback)` - Tiếp tục điều hướng
  - `stop(callback)` - Dừng điều hướng
  - `sortTargets(pointList, callback)` - Lập kế hoạch đường đi đa điểm
- **MapComponent**:
  - `mapZoneList(callback)` - Lấy danh sách tất cả zones trong map
- **MotorComponent** (SdkHelper):
  - `hrc(callback, enable)` - Khóa/mở khóa motor
- Tất cả API đều có error handling và logging chi tiết

### NavManager (API cũ)
- Sử dụng PeanutNavigation và RouteNode
- Hỗ trợ điều hướng đa điểm với repeat
- Có callback để theo dõi trạng thái điều hướng

### Trạng thái điều hướng
- **STATE_DESTINATION**: Đã đến đích
- **STATE_COLLISION**: Phát hiện va chạm
- **STATE_BLOCKED**: Bị chặn
- **STATE_BLOCKING**: Đang chờ timeout

## Troubleshooting

### Lỗi kết nối
- Kiểm tra robot có hoạt động không
- Đảm bảo SDK đã được khởi tạo thành công

### API không hoạt động
- Tất cả API đã được cập nhật để sử dụng các component thực tế
- Kiểm tra log để xem thông báo lỗi cụ thể
- NavManager được sử dụng làm API chính cho điều hướng

### Điều hướng không thành công
- Kiểm tra bản đồ có tồn tại không
- Đảm bảo điểm đích hợp lệ
- Kiểm tra robot có bị chặn không

### Auto Charge không hoạt động
- Kiểm tra map có được cấu hình dock sạc chưa
- Dock sạc phải có type = "charger" hoặc "dock"
- Đảm bảo dock sạc có areaId hợp lệ

## Cấu trúc code

```
TestNavigationActivity
├── initViews() - Khởi tạo UI
├── setupListeners() - Thiết lập event listeners
├── initRuntime() - Khởi tạo PeanutRuntime
├── connectToRobot() - Kết nối robot
├── getMaps() - Lấy danh sách bản đồ
├── navigateToPoint() - Điều hướng đến điểm
├── testNavManager() - Test NavManager
└── Navigation.Listener - Callback cho điều hướng
```

## Dependencies

- PeanutSDK
- RuntimeComponent
- NavManager
- PeanutRuntime
- RouteNode
- MyPoint
- MotorComponent
- BatteryComponent
- NavigationComponent
- MapComponent
- SdkHelper (tham khảo)
