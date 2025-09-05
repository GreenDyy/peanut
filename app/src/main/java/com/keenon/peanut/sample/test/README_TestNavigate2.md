# Hướng dẫn sử dụng TestNavigate2Activity

## Tổng quan

`TestNavigate2Activity` đã được cập nhật để tích hợp đầy đủ với **Map Loading** và **Navigation** functionality. Activity này cho phép:

- ✅ **Load Map** bằng cả API trực tiếp và PeanutSDKManager
- ✅ **Xem thông tin map** và các điểm đích có sẵn
- ✅ **Navigation** đến các điểm đích
- ✅ **Kiểm tra trạng thái** map và navigation
- ✅ **Quản lý navigation** (start, stop, pause, resume)

## Các chức năng chính

### 🗺️ **Map Loading**

#### 1. **Load Map (API)** - Button màu xanh lá
- Sử dụng `PeanutSDK.getInstance().navigation().getAllTargets()`
- Lấy danh sách tất cả các điểm đích từ robot
- Hiển thị các điểm: targets, chargers, elevators, origins

#### 2. **Load Map (SDK)** - Button màu xanh nhạt  
- Sử dụng `PeanutSDKManager.checkAndLoadMap()`
- Tự động kiểm tra và load map nếu cần
- Tích hợp với ROS system và TFTP download
- Có callback để theo dõi progress

#### 3. **Check Status** - Button màu xám
- Kiểm tra trạng thái map loading
- Hiển thị thông tin: đang loading, cần load, SDK initialized

### 🧭 **Navigation**

#### 1. **Start Navigate** - Button màu xanh dương
- Bắt đầu navigation đến điểm đích đã chọn
- Sử dụng `PeanutSDK.getInstance().navigation().setTarget()`

#### 2. **Stop** - Button màu đỏ
- Dừng navigation hiện tại
- Sử dụng `PeanutSDK.getInstance().navigation().stop()`

#### 3. **Pause** - Button màu cam
- Tạm dừng navigation
- Sử dụng `PeanutSDK.getInstance().navigation().pause()`

#### 4. **Resume** - Button màu xanh lá
- Tiếp tục navigation đã tạm dừng
- Sử dụng `PeanutSDK.getInstance().navigation().resume()`

#### 5. **Get Status** - Button màu tím
- Lấy trạng thái navigation hiện tại
- Hiển thị thông tin chi tiết về navigation

## Cách sử dụng

### Bước 1: Khởi động Activity
```
Activity sẽ tự động:
1. Khởi tạo PeanutSDK
2. Khởi tạo NavigationComponent  
3. Kiểm tra trạng thái map
```

### Bước 2: Load Map
```
1. Nhấn "Load Map (SDK)" để load map với PeanutSDKManager
2. Hoặc nhấn "Load Map (API)" để lấy targets trực tiếp
3. Nhấn "Check Status" để kiểm tra trạng thái
```

### Bước 3: Chọn điểm đích
```
1. Sau khi load map, các button điểm đích sẽ hiển thị
2. Nhấn vào button để chọn điểm đích
3. Hoặc nhập Target ID thủ công
```

### Bước 4: Navigation
```
1. Nhập Target ID và Approximate Time
2. Nhấn "Start Navigate" để bắt đầu
3. Sử dụng Pause/Resume/Stop để điều khiển
4. Nhấn "Get Status" để theo dõi
```

## Giao diện

### **Input Section**
- **Target ID**: ID của điểm đích (mặc định: 6)
- **Approximate Time**: Thời gian dự kiến (mặc định: 1)

### **Map Loading Buttons**
- **Load Map (API)**: Xanh lá - Load targets bằng API
- **Load Map (SDK)**: Xanh nhạt - Load map bằng SDK Manager  
- **Check Status**: Xám - Kiểm tra trạng thái map

### **Navigation Buttons**
- **Start Navigate**: Xanh dương - Bắt đầu navigation
- **Stop**: Đỏ - Dừng navigation
- **Get Status**: Tím - Lấy trạng thái navigation

### **Control Buttons**
- **Pause**: Cam - Tạm dừng navigation
- **Resume**: Xanh lá - Tiếp tục navigation

### **Available Targets**
- Hiển thị danh sách các điểm đích có sẵn
- **Targets**: Màu mặc định
- **Chargers**: Màu xanh lá
- **Elevators**: Màu tím  
- **Origins**: Màu cam

### **Log Output**
- Hiển thị log real-time với timestamp
- Màu xanh lá trên nền đen
- Font monospace để dễ đọc

## Luồng hoạt động

```
1. Activity khởi tạo
   ↓
2. PeanutSDKManager.initializeSDK()
   ↓
3. initNavigation() - Khởi tạo NavigationComponent
   ↓
4. checkMapStatus() - Kiểm tra trạng thái map
   ↓
5. User có thể:
   - Load map (API hoặc SDK)
   - Chọn điểm đích
   - Bắt đầu navigation
   - Điều khiển navigation
   ↓
6. onDestroy() - Cleanup map loading
```

## Lưu ý quan trọng

1. **SDK phải được khởi tạo** trước khi sử dụng navigation
2. **Map phải được load** trước khi navigation
3. **Target ID phải hợp lệ** (có trong danh sách targets)
4. **Navigation là async** - sử dụng callback để theo dõi
5. **Cleanup tự động** khi destroy activity

## Troubleshooting

### Lỗi "SDK chưa được khởi tạo"
- Đợi SDK khởi tạo hoàn tất
- Kiểm tra log để xem trạng thái init

### Lỗi "Navigation component chưa được khởi tạo"  
- Đảm bảo SDK init thành công
- Kiểm tra PeanutSDK.getInstance() != null

### Không có targets hiển thị
- Nhấn "Load Map (API)" để lấy targets
- Kiểm tra robot có map không
- Kiểm tra network connection

### Navigation không hoạt động
- Kiểm tra Target ID có hợp lệ không
- Đảm bảo map đã được load
- Kiểm tra robot không bị block

## Xem thêm

- `PeanutSDKManager.java` - Map loading functionality
- `MapLoadExample.java` - Ví dụ sử dụng map loading
- `README_MapLoading.md` - Hướng dẫn chi tiết map loading
