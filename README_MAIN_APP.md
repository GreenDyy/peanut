# Main App - Robot Assistant

## Mô tả
MainAppActivity là một ứng dụng robot assistant với giao diện thân thiện và các chức năng dịch vụ công dân.

## Tính năng

### 1. Màn hình Robot Face (MainAppActivity)
- Hiển thị mặt robot với 2 mắt nhấp nháy
- Animation mắt nhấp nháy tự động mỗi 3 giây
- Nhấn vào màn hình để chuyển đến menu chức năng

### 2. Menu Chức Năng (FunctionMenuActivity)
- 3 nút chức năng chính:
  - **Căn Cước Công Dân**
  - **Khai Báo Tạm Trú** 
  - **Xin Giấy Xác Nhận Cư Trú**

### 3. Màn hình Chi tiết Dịch vụ
Mỗi chức năng sẽ mở ra màn hình riêng với:
- **Số thứ tự**: Tự động tạo ngẫu nhiên từ 001-999
- **Mã QR**: Chứa thông tin dịch vụ và timestamp
- **Giao diện**: Thiết kế đẹp mắt với màu sắc chuyên nghiệp

## Cấu trúc Code

### Activities
- `MainAppActivity.java` - Màn hình chính với robot face
- `FunctionMenuActivity.java` - Menu chọn chức năng
- `CanCuocActivity.java` - Dịch vụ căn cước công dân
- `TamTruActivity.java` - Dịch vụ khai báo tạm trú
- `XacNhanCuTruActivity.java` - Dịch vụ xác nhận cư trú

### Layouts
- `activity_main_app.xml` - Layout màn hình robot face
- `activity_function_menu.xml` - Layout menu chức năng
- `activity_service_detail.xml` - Layout chung cho các dịch vụ

### Drawables
- `robot_face_bg.xml` - Background mặt robot
- `robot_eye.xml` - Mắt robot
- `robot_mouth.xml` - Miệng robot

## Cách sử dụng

1. Chạy ứng dụng và chọn "Main App" từ danh sách
2. Nhấn vào màn hình robot để mở menu chức năng
3. Chọn dịch vụ cần thiết
4. Xem số thứ tự và mã QR được tạo

## Dependencies
- `com.google.zxing:core:3.4.1` - Tạo mã QR
- Các thư viện Android cơ bản

## Lưu ý
- Mã QR chứa thông tin: `[LOẠI_DỊCH_VỤ]_[SỐ_THỨ_TỰ]_[TIMESTAMP]`
- Số thứ tự được tạo ngẫu nhiên mỗi lần mở
- Animation mắt nhấp nháy tự động dừng khi đóng activity 