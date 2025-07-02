# FoodApp
Ứng dụng Android đa vai trò cho hệ sinh thái nhà hàng: khách hàng đặt món, nhà hàng quản lý vận hành, nhân viên xử lý đơn, và shipper theo dõi giao hàng. Dự án dùng Jetpack Compose, kiến trúc tách lớp và sử dụng flavors để build theo từng vai trò.

## Mục lục
- Tổng quan
- Tính năng
- Vai trò và phạm vi chức năng
- Kiến trúc và công nghệ
- Cấu trúc thư mục
- Yêu cầu hệ thống
- Cài đặt và cấu hình
- Build và chạy
- Testing
- Lưu ý và troubleshooting
- Đóng góp
- Giấy phép

## Tổng quan
FoodApp gồm 4 flavor chính, mỗi flavor có AndroidManifest riêng và tập chức năng phù hợp:
- customer: đặt món, đặt bàn, theo dõi đơn, thông báo.
- admin: quản lý menu, kho, nhân sự, voucher.
- staff: xử lý đơn, cập nhật trạng thái, hỗ trợ vận hành.
- shipper: theo dõi lộ trình, cập nhật giao hàng.

## Tính năng
- Duyệt menu, xem chi tiết món, thêm vào giỏ, đặt món.
- Theo dõi trạng thái đơn, nhận thông báo đẩy.
- Quản lý kho, nhập/xuất, danh mục món và voucher.
- Hỗ trợ định vị và chỉ đường giao hàng.
- Lưu trữ cục bộ và đồng bộ dữ liệu.

## Vai trò và phạm vi chức năng
### Customer
- Duyệt menu, xem chi tiết món, thêm vào giỏ.
- Đặt món, theo dõi trạng thái đơn, nhận thông báo.
- Quản lý địa chỉ, hồ sơ, lịch sử đơn.

### Admin (Restaurant)
- Quản lý danh mục món, menu, nguyên liệu.
- Quản lý kho: nhập/xuất, theo dõi tồn.
- Quản lý nhân sự, voucher, thông tin nhà hàng.

### Staff
- Tiếp nhận và xử lý đơn hàng.
- Cập nhật trạng thái đơn và thông báo cho khách.

### Shipper
- Theo dõi đơn cần giao, cập nhật trạng thái giao hàng.
- Hỗ trợ định vị và chỉ đường giao hàng.

## Kiến trúc và công nghệ
- UI: Jetpack Compose, Material 3, Compose Navigation.
- Architecture: MVVM + phân lớp data/domain/ui.
- DI: Hilt (KSP).
- Network: Retrofit + OkHttp.
- Data: Room, DataStore.
- Map/Location: Mapbox, Google Play Services Location.
- Firebase: Messaging, Auth, Firestore, Storage.

### Phân lớp chính
- data: dto, remote, local, mapper, repository.
- domain: use_case, model, repository interface.
- ui: screen, components, theme.

## Cấu trúc thư mục
Đường dẫn chính:

```
app/src/main/java/com/example/foodapp/
  data/
  domain/
  navigation/
  notification/
  ui/
  utils/
app/src/{customer,admin,staff,shipper}/AndroidManifest.xml
```

## Yêu cầu hệ thống
- Android Studio (AGP 8.x)
- JDK 11
- Android SDK 35

## Cài đặt và cấu hình
### Bước 1: Clone project

```bash
git clone <repository-url>
```

### Bước 2: Thiết lập key và file
1) Tạo file secrets.properties ở root project.
2) Điền các key cần thiết:

```properties
BACKEND_URL=http://10.0.2.2:8080/api/v1/
GOOGLE_WEB_CLIENT_ID=your_google_web_client_id
OPEN_CAGE_API_KEY=your_open_cage_key
ORS_KEY=your_ors_key
MAPS_BOX_KEYL=your_mapbox_key
```

3) Đặt file app/google-services.json từ Firebase Console.
4) Tạo file Mapbox token:

```
app/src/main/res/values/mapbox_access_token.xml
```

Nội dung gợi ý:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <string name="mapbox_access_token" translatable="false" tools:ignore="UnusedResources">YOUR_MAPBOX_TOKEN</string>
</resources>
```

## Build và chạy
### Build theo flavor

```bash
./gradlew assembleCustomerDebug
./gradlew assembleAdminDebug
./gradlew assembleStaffDebug
./gradlew assembleShipperDebug
```

### Chạy từ Android Studio
Mở project, chọn Build Variant: customerDebug/adminDebug/staffDebug/shipperDebug.

## Testing

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Lưu ý và troubleshooting
- Nếu lỗi google-services.json: kiểm tra package name và dùng file từ Firebase Console.
- Nếu Mapbox không hiển thị: kiểm tra token và quyền truy cập Maps Downloads.
- Nếu lỗi backend: kiểm tra BACKEND_URL và kết nối emulator/thiết bị.
- Nếu FCM không nhận: kiểm tra google-services.json và token device.

## Đóng góp
Hiện chưa có quy trình đóng góp chính thức. Vui lòng tạo issue hoặc pull request nếu bạn muốn đóng góp.

## Giấy phép
Dự án chưa công bố giấy phép. Nếu bạn muốn sử dụng hoặc phân phối lại, vui lòng liên hệ tác giả.
