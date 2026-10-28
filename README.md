# Kids TV MMHY

Biến TV TCL (Google TV, Android 12) của gia đình thành **TV cho bé**: bật lên chỉ thấy một ô **YouTube** (YouTube Kids nay là hồ sơ của bé nằm ngay trong ứng dụng YouTube), mọi ứng dụng khác bị chặn, có hẹn giờ xem. Màn hình chính dùng hình nền công ty Minh Minh Hưng Yên, có đồng hồ, thời tiết Yên Mỹ và lịch âm dương.

Ứng dụng này là **launcher (màn hình chính) thay thế** + một **dịch vụ bảo vệ** (trợ năng). Cài bằng adb qua mạng LAN, không cần root, không cần Google Play. Hoàn tác được bất cứ lúc nào bằng `8-THOAT-LAUNCHER-BE.bat`.

## Cách hoạt động

| Việc | Cách làm |
|---|---|
| Chỉ có YouTube | Màn hình chính mới chỉ có một ô YouTube (`com.google.android.youtube.tv`). Ứng dụng nào khác (Cài đặt, Live TV, Play Store, màn hình chính gốc…) vừa hiện lên là bị đẩy về màn hình trẻ em ngay. Ứng dụng YouTube Kids đời cũ trên TV nay chỉ còn màn hình báo "đã chuyển đi nơi khác", được coi cùng nhóm với YouTube. |
| Hẹn giờ | Mỗi lần xem **30 phút** (đổi được trong Menu phụ huynh). Nhắc trước 5 phút và 1 phút. Hết giờ thì khóa, YouTube bị tắt hẳn. |
| Phiên đầu tiên mỗi ngày | Lần bật TV đầu tiên trong ngày: miễn phí, không cần mật khẩu hay làm toán. "Ngày" đổi lúc 05:00 sáng. Tắt được trong Menu phụ huynh. |
| Mỗi lần bật TV (từ lần 2 trong ngày) | Tắt TV (nút Nguồn, màn hình tắt) hoặc TV khởi động lại thì **bỏ hết giờ còn dư và khóa**. Lần bật sau hiện **10 bài toán** (đúng liên tiếp 10 câu, sai thì làm lại từ đầu); xong thì bé được **một phiên mới đầy đủ** (không tính là lượt thưởng). Số câu chỉnh trong Menu phụ huynh (Tắt / 5 / 10 / 15 / 20); để **Tắt** thì tắt TV chỉ tạm dừng đếm giờ như trước. Tắt TV cũng kết thúc chế độ phụ huynh. Cập nhật ứng dụng (cài đè) thì không bị khóa. |
| Hết giờ | Màn hình khóa hiện **bài toán cộng trừ trong phạm vi 100** (4 đáp án, chọn bằng remote). Bé trả lời **đúng liên tiếp 20 câu** thì được xem thêm **15 phút**. Sai một câu là đếm lại từ đầu. |
| Mật khẩu phụ huynh | Mặc định là **ngày hôm nay dạng ddMMyyyy**: hôm nay 28/10/2026 là `28102026`, ngày mai là `29102026`. Có thể đặt mật khẩu cố định 4–8 số trong Menu phụ huynh. Sai 5 lần thì khóa nhập 60 giây. |
| Menu phụ huynh | Cho bé xem N phút · đổi thời lượng, thời gian thưởng, số câu toán khi hết giờ, giới hạn lượt thưởng mỗi ngày · bật/tắt phiên miễn phí đầu ngày · số câu toán mỗi lần bật TV · đổi mật khẩu · mở Cài đặt TV (30 phút) · dùng TV bình thường (60 phút) · khóa TV ngay. Tự thoát về màn hình chính sau 90 giây không bấm. |
| Màn hình chính | Hình nền công ty, đồng hồ, ngày dương, ngày âm kèm can chi, thời tiết hiện tại + 3 ngày (Open-Meteo), lịch tháng có ngày âm. Thời tiết lưu bản gần nhất nên mất mạng vẫn hiện. |

Dịch vụ bảo vệ **không đọc nội dung màn hình**, chỉ biết gói ứng dụng nào vừa mở cửa sổ.

## Cài lên TV (Windows, trong `D:\TV-Kids`)

1. `5-CAI-LAUNCHER-BE.bat`: cài ứng dụng, cấp quyền, **bật lại YouTube** (nếu đã bị tắt ở bước 2 cũ), đặt làm màn hình chính mặc định, mở thử. (`2-...` và `4-...` đã ngưng dùng, mở ra chỉ báo chuyển sang bước 5.)
2. `6-KIEM-TRA-TV.bat`: tự thử trên TV thật (HOME, chặn Cài đặt, mở YouTube, hết giờ, khóa) và chụp ảnh màn hình TV vào `out\`.
3. `7-XEM-NHAT-KY.bat`: lấy nhật ký khi cần xem ứng dụng nào đã bị chặn.
4. `8-THOAT-LAUNCHER-BE.bat`: **thoát/hoàn tác** (tạm dừng hoặc gỡ hẳn), TV về màn hình chính gốc.
5. `9-DAT-LAI-MAT-KHAU.bat`: đặt lại mật khẩu về mật khẩu theo ngày (quên mật khẩu cố định).
6. `10-THU-NUT-NGUON.bat`: thử có hướng dẫn trên TV thật: bấm nút Nguồn trên remote để tắt rồi bật TV, bat đọc lại xem TV có gửi sự kiện tắt màn hình, ứng dụng có khóa chờ toán không, và lấy nhật ký nguồn điện (gọi luôn `7-XEM-NHAT-KY.bat`).

Sau khi xong, nhớ **tắt "Gỡ lỗi mạng" (Network debugging) trên TV**: cổng adb 5555 không mã hóa, ai cùng mạng wifi đều vào được.

## Thiết lập YouTube cho bé (làm một lần trên TV)

Chế độ Kids nằm **trong ứng dụng YouTube**, nên chuyện bé chỉ xem nội dung trẻ em do hồ sơ và mã phụ huynh của YouTube quyết định, không phải do ứng dụng này:

1. Trên màn hình chính bấm **Phụ huynh**, nhập mật khẩu, chọn **Dùng TV bình thường** (60 phút), mở **YouTube**.
2. Ở màn hình "Ai đang xem?" chọn **hồ sơ của bé** (Kids). Chưa có thì tạo theo trang trợ giúp của Google: https://support.google.com/youtubekids/answer/9618031
3. Trong YouTube vào **Cài đặt** (bên trái) → **Mã phụ huynh** → đặt 4 số. Theo Google, có mã này thì bé không xem YouTube khi chưa đăng nhập, không vào được tài khoản của người lớn trong nhà và không gỡ được tài khoản khỏi TV. https://support.google.com/youtubekids/answer/6172308
4. Bấm HOME, vào lại **Phụ huynh** → **Thoát phụ huynh**.

Ứng dụng này không đọc được nội dung bên trong YouTube (chỉ biết gói nào đang mở) nên **không thay thế được bước 3**. Nếu bé chuyển được sang tài khoản người lớn, nghĩa là chưa đặt mã phụ huynh.

## Lệnh adb (cứu hộ, kiểm thử)

```
adb shell am broadcast -n com.minhminhhungyen.kidstv/.AdminReceiver -a com.minhminhhungyen.kidstv.<LỆNH> [--ei tham_số giá_trị]
```

`STATUS` xem trạng thái · `GRANT --ei minutes 30` (hoặc `--ei seconds N`) cho xem · `EXPIRE` khóa ngay · `RESET_DAY` về trạng thái đầu ngày mới · `RESET_PWD` đặt lại mật khẩu · `PARENT_ON --ei minutes N` / `PARENT_OFF` chế độ phụ huynh · `CONFIG --ei session N --ei bonus N --ei streak N --ei cap N --ei free 0|1 --ei gate N` (`gate` = số câu toán mỗi lần bật TV, 0 = tắt; `--ei freeDay 0` = coi như hôm nay chưa dùng phiên miễn phí) · `POWERCYCLE` giả lập "TV vừa tắt" · `BOOT` giả lập "TV vừa khởi động" · `EVENTS` các lần tắt/bật gần nhất · `SHUTDOWN` tắt dịch vụ bảo vệ. Chỉ tài khoản adb shell gửi được; ứng dụng thường thì không.

Nhật ký: `adb logcat -d -s KidsTV:V`. Các sự kiện tắt/bật màn hình, khởi động, cấp giờ, khóa còn được ghi vào một tệp nhỏ trong ứng dụng (lệnh `EVENTS`), nên **vẫn xem được sau khi TV khởi động lại** (lúc đó logcat đã bị xóa). `7-XEM-NHAT-KY.bat` lấy cả hai.

## Xây dựng và kiểm thử (GitHub Actions)

Mỗi lần đẩy lên `main`, workflow `Android CI`:

1. **Unit test**: lịch âm (Tết 2000–2029, Trung thu, tháng nhuận, từng ngày liên tục trong 12 năm), bài toán, mật khẩu theo ngày, đọc thời tiết.
2. **Build** `MMHY-KidsTV-release.apk` và `MMHY-KidsTV-debug.apk` (ký bằng khóa cố định `app/kids-tv.jks`, nên bản mới cài đè bản cũ được) và `FakeYTKids-debug.apk` (YouTube **giả**, mang tên gói `com.google.android.youtube.tv`, chỉ để thử trên máy ảo, không cài lên TV).
3. **Máy ảo Android 12, màn hình 1920×1080**: chạy `.github/scripts/emulator-checks.sh` đúng các lệnh adb mà file bat sẽ chạy, thử từ đầu đến cuối (chặn Cài đặt và ứng dụng lạ, OK mở YouTube, hết giờ, bài toán đúng/sai, mật khẩu ngày và khóa 60 giây, mật khẩu cố định, chế độ phụ huynh, báo trước 1 phút, tắt/bật màn hình, tự khóa khi hết giờ, **toán mỗi lần bật TV**: lần đầu trong ngày miễn phí, lần 2 phải giải toán rồi nhận phiên mới đầy đủ, giả lập khởi động lại, cài đè không bị khóa), chụp ảnh từng bước.
4. Đẩy kết quả (APK, ảnh, `results.txt`) lên nhánh `ci-artifacts`.

Những gì máy ảo **không** thử được và chỉ kiểm chứng trên TV thật bằng `6-KIEM-TRA-TV.bat` và nhật ký: các cửa sổ riêng của TCL (nút Nguồn/Source, nút Cài đặt nhanh), giữ nguyên màn hình chính sau khi khởi động lại TV thật, và nút Nguồn của TV TCL thực sự gửi sự kiện gì (tắt màn hình, hay khởi động lại hẳn): xem `out\su-kien-tv.txt` của `7-XEM-NHAT-KY.bat`.

## Ghi chú bảo mật

- Repo này **riêng tư**. Khóa ký `app/kids-tv.jks` (mật khẩu `mmhykids`) nằm trong repo để các bản cài sau cài đè được nhau; đây không phải khóa phát hành lên cửa hàng ứng dụng.
- Mật khẩu theo ngày dễ đoán với bé lớn đã biết quy tắc. Muốn chắc hơn: Menu phụ huynh → Mật khẩu phụ huynh → đặt mật khẩu cố định.
- Ai có adb vào được TV (mạng LAN + Gỡ lỗi mạng đang bật) đều gỡ được bảo vệ. Tắt Gỡ lỗi mạng sau khi cài xong.
