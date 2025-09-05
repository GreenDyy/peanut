# Hướng dẫn xem và sử dụng Location IDs trong Map

## 📍 **Cách xem các ID Location có sẵn trong Map**

### 1. **Sử dụng TestNavigate2Activity**

**Bước 1:** Mở `TestNavigate2Activity`
**Bước 2:** Nhấn nút **"Load Map"** (màu xanh lá)
**Bước 3:** Xem danh sách các location IDs được hiển thị

### 2. **Các loại Location IDs**

Khi load map thành công, bạn sẽ thấy các loại location sau:

#### 🎯 **TARGETS THÔNG THƯỜNG** (màu xanh lá)
- Các điểm đích thông thường trong map
- Dùng cho navigation bình thường
- Ví dụ: ID 1, 2, 3, 4, 5, 6...

#### 🔋 **ĐIỂM SẠC** (màu xanh dương)
- Các điểm sạc pin của robot
- Robot sẽ tự động đi đến khi pin yếu
- Ví dụ: Charger ID 100, 101...

#### 🛗 **THANG MÁY** (màu tím)
- Các điểm thang máy trong tòa nhà
- Dùng cho navigation đa tầng
- Ví dụ: Elevator ID 200, 201...

#### 🏠 **ĐIỂM GỐC** (màu cam)
- Các điểm xuất phát/trở về
- Thường là điểm home của robot
- Ví dụ: Origin ID 0, 1...

## 🚀 **Cách sử dụng Location IDs cho Navigation**

### **Phương pháp 1: Chọn từ danh sách**
1. Nhấn **"Load Map"** để xem danh sách
2. Nhấn vào bất kỳ button nào để chọn ID
3. ID sẽ tự động điền vào ô "Target ID"
4. Nhấn **"Start Navigate"** để bắt đầu

### **Phương pháp 2: Nhập thủ công**
1. Nhập ID trực tiếp vào ô "Target ID"
2. Nhập thời gian approximate (mặc định: 1)
3. Nhấn **"Start Navigate"**

## 📊 **Thông tin chi tiết về Map**

### **NavigationPointBean Structure:**
```java
NavigationPointBean {
    count: int,           // Tổng số targets
    list: List<Integer>,  // Targets thông thường
    chargers: List<Integer>, // Điểm sạc
    elevators: List<Integer>, // Thang máy
    origins: List<Integer>    // Điểm gốc
}
```

### **API để lấy thông tin:**
```java
PeanutSDK.getInstance().navigation().getAllTargets(new ApiCallback<BaseResp<NavigationPointBean>>() {
    @Override
    public void onSuccess(BaseResp<NavigationPointBean> response) {
        NavigationPointBean data = response.getData();
        // data.getList() - targets thông thường
        // data.getChargers() - điểm sạc
        // data.getElevators() - thang máy
        // data.getOrigins() - điểm gốc
    }
});
```

## 🎮 **Các chức năng Navigation**

### **1. Start Navigation**
```java
PeanutSDK.getInstance().navigation().setTarget(targetId, approximateTime, callback);
```

### **2. Stop Navigation**
```java
PeanutSDK.getInstance().navigation().stop(callback);
```

### **3. Pause/Resume Navigation**
```java
PeanutSDK.getInstance().navigation().pause(callback);
PeanutSDK.getInstance().navigation().resume(callback);
```

### **4. Get Navigation Status**
```java
PeanutSDK.getInstance().navigation().getStatus(callback);
```

## 🔧 **Troubleshooting**

### **Lỗi thường gặp:**

1. **"PeanutSDK chưa được khởi tạo"**
   - Đảm bảo SDK đã được init trước khi sử dụng
   - Kiểm tra `PeanutSDKManager.isInitialized()`

2. **"Response null" hoặc "Data null"**
   - Robot có thể chưa có map
   - Kiểm tra kết nối với robot
   - Thử load map trước

3. **"Navigation thất bại"**
   - Kiểm tra Target ID có tồn tại không
   - Đảm bảo robot đang ở trạng thái sẵn sàng
   - Kiểm tra map đã được load chưa

## 📝 **Ví dụ sử dụng**

### **Ví dụ 1: Navigation đến Target ID 6**
```java
// 1. Load map để xem các ID có sẵn
PeanutSDK.getInstance().navigation().getAllTargets(callback);

// 2. Chọn Target ID 6
int targetId = 6;
int approximateTime = 1;

// 3. Bắt đầu navigation
PeanutSDK.getInstance().navigation().setTarget(targetId, approximateTime, new ApiCallback<BaseResp<String>>() {
    @Override
    public void onSuccess(BaseResp<String> response) {
        Log.d("Navigation", "Bắt đầu navigation đến ID: " + targetId);
    }
    
    @Override
    public void onFail(ApiError error) {
        Log.e("Navigation", "Lỗi: " + error.getMsg());
    }
});
```

### **Ví dụ 2: Kiểm tra tất cả locations**
```java
PeanutSDK.getInstance().navigation().getAllTargets(new ApiCallback<BaseResp<NavigationPointBean>>() {
    @Override
    public void onSuccess(BaseResp<NavigationPointBean> response) {
        NavigationPointBean data = response.getData();
        
        Log.d("Map", "Tổng số targets: " + data.getCount());
        Log.d("Map", "Targets thông thường: " + data.getList());
        Log.d("Map", "Điểm sạc: " + data.getChargers());
        Log.d("Map", "Thang máy: " + data.getElevators());
        Log.d("Map", "Điểm gốc: " + data.getOrigins());
    }
});
```

## 🎯 **Tips sử dụng**

1. **Luôn load map trước** khi navigation để đảm bảo có đầy đủ thông tin
2. **Kiểm tra Target ID** có tồn tại trong danh sách trước khi navigate
3. **Sử dụng approximate time** phù hợp (thường là 1-3 giây)
4. **Theo dõi log** để debug khi có lỗi
5. **Test với các ID khác nhau** để hiểu rõ map structure

---

**Lưu ý:** Các ID location có thể khác nhau tùy theo map và cấu hình robot. Luôn kiểm tra danh sách thực tế trước khi sử dụng.
