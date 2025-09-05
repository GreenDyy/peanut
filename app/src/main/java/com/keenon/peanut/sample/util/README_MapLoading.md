# Hướng dẫn sử dụng Map Loading với PeanutSDKManager

## Tổng quan

`PeanutSDKManager` đã được tích hợp chức năng load map từ hệ thống Keenon. Chức năng này cho phép:

- ✅ Tự động kiểm tra và load map khi cần thiết
- ✅ Load map với resource cụ thể
- ✅ Theo dõi trạng thái map loading
- ✅ Xử lý lỗi và progress callback
- ✅ Tích hợp với ROS system

## Các phương thức chính

### 1. `shouldLoadMap()`
```java
boolean shouldLoad = PeanutSDKManager.shouldLoadMap();
```
- Kiểm tra xem có cần load map hay không
- Trả về `true` nếu machine location type = 1 (ROS mode)

### 2. `loadMap(Context, Resource, MapLoadListener)`
```java
PeanutSDKManager.loadMap(context, resource, new PeanutSDKManager.MapLoadListener() {
    @Override
    public void onMapLoadStart() {
        // Map bắt đầu load
    }
    
    @Override
    public void onMapLoadSuccess() {
        // Map load thành công
    }
    
    @Override
    public void onMapLoadError(String error) {
        // Map load lỗi
    }
    
    @Override
    public void onMapLoadProgress(int progress) {
        // Progress update
    }
});
```

### 3. `checkAndLoadMap(Context, MapLoadListener)`
```java
PeanutSDKManager.checkAndLoadMap(context, listener);
```
- Tự động kiểm tra và load map nếu cần
- Sử dụng logic từ `PeanutResourceManager`

### 4. `isMapLoading()`
```java
boolean isLoading = PeanutSDKManager.isMapLoading();
```
- Kiểm tra trạng thái map loading hiện tại

### 5. `stopMapLoading()`
```java
PeanutSDKManager.stopMapLoading();
```
- Dừng quá trình map loading

## Cách sử dụng

### Bước 1: Khởi tạo SDK
```java
PeanutSDKManager.initializeSDK(context, new PeanutSDK.ErrorListener() {
    @Override
    public void onInit(int statusCode) {
        if (statusCode == PeanutSDK.SDK_INIT_SUCCESS) {
            // SDK đã sẵn sàng, có thể sử dụng map loading
        }
    }
});
```

### Bước 2: Kiểm tra và load map
```java
// Tự động kiểm tra và load map
PeanutSDKManager.checkAndLoadMap(context, new PeanutSDKManager.MapLoadListener() {
    @Override
    public void onMapLoadStart() {
        // Hiển thị loading UI
    }
    
    @Override
    public void onMapLoadSuccess() {
        // Map sẵn sàng sử dụng
    }
    
    @Override
    public void onMapLoadError(String error) {
        // Xử lý lỗi
    }
    
    @Override
    public void onMapLoadProgress(int progress) {
        // Cập nhật progress
    }
});
```

### Bước 3: Cleanup
```java
// Trong onDestroy() của Activity
if (PeanutSDKManager.isMapLoading()) {
    PeanutSDKManager.stopMapLoading();
}
```

## Luồng hoạt động

```
1. Khởi tạo SDK
   ↓
2. Kiểm tra machine location type
   ↓
3. Nếu cần load map:
   - Khởi tạo RosMapDownLoadManager
   - Tạo MapConfig với thông tin resource
   - Download map qua TFTP
   - Tích hợp với ROS system
   ↓
4. Callback success/error
```

## Lưu ý quan trọng

1. **SDK phải được khởi tạo trước** khi sử dụng map loading
2. **Machine location type = 1** mới cần load map (ROS mode)
3. **Resource phải có đầy đủ thông tin**: rosMapMd5, mapFilePath, rosMapId
4. **Map loading là async**, sử dụng callback để theo dõi
5. **Cleanup** khi không cần thiết để tránh memory leak

## Xem thêm

- `MapLoadExample.java` - Các ví dụ sử dụng chi tiết
- `RosMapDownLoadManager.java` - Implementation chi tiết
- `PeanutResourceManager.java` - Resource management

## Troubleshooting

### Lỗi "SDK chưa được khởi tạo"
- Đảm bảo gọi `PeanutSDKManager.initializeSDK()` trước
- Kiểm tra `PeanutSDKManager.isInitialized()`

### Lỗi "Resource null"
- Kiểm tra resource object có null không
- Đảm bảo resource có đầy đủ thông tin cần thiết

### Map không load
- Kiểm tra `shouldLoadMap()` trả về true
- Kiểm tra machine location type
- Kiểm tra network connection cho TFTP
