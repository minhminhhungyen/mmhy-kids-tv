# Kids TV MMHY

Biến TV TCL (Google TV, Android 12) của gia đình thành **TV cho bé**: bật lên chỉ thấy một ô **YouTube Kids**, mọi ứng dụng khác bị chặn, có hẹn giờ xem. Màn hình chính dùng hình nền công ty Minh Minh Hưng Yên, có đồng hồ, thời tiết Yên Mỹ và lịch âm dương.

Ứng dụng này là **launcher (màn hình chính) thay thế** + một **dịch vụ bảo vệ** (trợ năng). Cài bằng adb qua mạng LAN, không cần root, không cần Google Play. Hoàn tác được bất cứ lúc nào bằng `8-THOAT-LAUNCHER-BE.bat`.

## Cách hoạt động

| Việc | Cách làm |
|---|---|
| Chỉ có YouTube Kids | Màn hình chính mới chỉ có một ô YouTube Kids. Ứng dụng nào khác (Cài đặt, Live TV, Play Store, màn hình chính gốc…) vừa hiện lên là bị đẩy về màn hình trẻ em ngay. |
| Hẹn giờ | Mỗi lần xem **30 phút** (đổi được trong Menu phụ huynh). Nhắc trước 5 phút và 1 phút. Hết giờ thì khóa, YouTube Kids bị tắt hẳn. Tắt màn hình TV thì tạm dừng đếm giờ. |
| Phiên đầu tiên mỗi ngày | Miễn phí, không cần mật khẩu. "Ngày" đổi lúc 05:00 sáng. Tắt được trong Menu phụ huynh. |
| Hết giờ | Màn hình khóa hiện **bài toán cộng trừ trong phạm vi 100** (4 đáp án, chọn bằng remote). Bé trả lời **đúng liên tiếp 20 câu** thì được xem thêm **15 phút**. Sai một câu là đếm lại từ đầu. |
| Mật khẩu phụ huynh | Mặc định là **ngày hôm nay dạng ddMMyyyy**: hôm nay 28/10/2026 là `28102026`, ngày mai là `29102026`. Có thể đặt mật khẩu cố định 4–8 số trong Menu phụ huynh. Sai 5 lần thì khóa nhập 60 giây. |
| Menu phụ huynh | Cho bé xem N phút · đổi thời lượng, thời gian thưởng, số câu đúng, giới hạn lượt thưởng mỗi ngày · bật/tắt phiên miễn phí đầu ngày · đổi mật khẩu · mở Cài đặt TV (30 phút) · dùng TV bình thường (60 phút) · khóa TV ngay. Tự thoát về màn hình chính sau 90 giây không bấm. |
| Màn hình chính | Hình nền công ty, đồng hồ, ngày dương, ngày âm kèm can chi, thời tiết hiện tại + 3 ngày (Open-Meteo), lịch tháng có ngày âm. Thời tiết lưu bản gần nhất nên mất mạng vẫn hiện. |

Dịch vụ bảo vệ **không đọc nội dung màn hình**, chỉ biết gói ứng dụng nào vừa mở cửa sổ.

## Cài lên TV (Windows, trong `D:\TV-Kids`)

1. `5-CAI-LAUNCHER-BE.bat`: cài ứng dụng, cấp quyền, đặt làm màn hình chính mặc định, mở thử.
2. `6-KIEM-TRA-TV.bat`: tự thử trên TV thật (HOME, chặn Cài đặt, mở YouTube Kids, hết giờ, khóa) và chụp ảnh màn hình TV vào `out\`.
3. `7-XEM-NHAT-KY.bat`: lấy nhật ký khi cần xem ứng dụng nào đã bị chặn.
4. `8-THOAT-LAUNCHER-BE.bat`: **thoát/hoàn tác** (tạm dừng hoặc gỡ hẳn), TV về màn hình chính gốc.
5. `9-DAT-LAI-MAT-KHAU.bat`: đặt lại mật khẩu về mật khẩu theo ngày (quên mật khẩu cố định).

Sau khi xong, nhớ **tắt "Gỡ lỗi mạng" (Network debugging) trên TV**: cổng adb 5555 không mã hóa, ai cùng mạng wifi đều vào được.

## Lệnh adb (cứu hộ, kiểm thử)

```
adb shell am broadcast -n com.minhminhhungyen.kidstv/.AdminReceiver -a com.minhminhhungyen.kidstv.<LỆNH> [--ei tham_số giá_trị]
```

`STATUS` xem trạng thái · `GRANT --ei minutes 30` (hoặc `--ei seconds N`) cho xem · `EXPIRE` khóa ngay · `RESET_DAY` về trạng thái đầu ngày mới · `RESET_PWD` đặt lại mật khẩu · `PARENT_ON --ei minutes N` / `PARENT_OFF` chế độ phụ huynh · `CONFIG --ei session N --ei bonus N --ei streak N --ei cap N --ei free 0|1` · `SHUTDOWN` tắt dịch vụ bảo vệ. Chỉ tài khoản adb shell gửi được; ứng dụng thường thì không.

Nhật ký: `adb logcat -d -s KidsTV:V`.

## Xây dựng và kiểm thử (GitHub Actions)

Mỗi lần đẩy lên `main`, workflow `Android CI`:

1. **Unit test**: lịch âm (Tết 2000–2029, Trung thu, tháng nhuận, từng ngày liên tục trong 12 năm), bài toán, mật khẩu theo ngày, đọc thời tiết.
2. **Build** `MMHY-KidsTV-release.apk` và `MMHY-KidsTV-debug.apk` (ký bằng khóa cố định `app/kids-tv.jks`, nên bản mới cài đè bản cũ được) và `FakeYTKids-debug.apk` (YouTube Kids **giả**, chỉ để thử trên máy ảo, không cài lên TV).
3. **Máy ảo Android 12, màn hình 1920×1080**: chạy `.github/scripts/emulator-checks.sh` đúng các lệnh adb mà file bat sẽ chạy, thử từ đầu đến cuối (chặn Cài đặt và ứng dụng lạ, OK mở YouTube Kids, hết giờ, bài toán đúng/sai, mật khẩu ngày và khóa 60 giây, mật khẩu cố định, chế độ phụ huynh, báo trước 1 phút, tắt/bật màn hình, tự khóa khi hết giờ), chụp ảnh từng bước.
4. Đẩy kết quả (APK, ảnh, `results.txt`) lên nhánh `ci-artifacts`.

Những gì máy ảo **không** thử được và chỉ kiểm chứng trên TV thật bằng `6-KIEM-TRA-TV.bat` và nhật ký: các cửa sổ riêng của TCL (nút Nguồn/Source, nút Cài đặt nhanh), giữ nguyên màn hình chính sau khi khởi động lại TV thật.

## Ghi chú bảo mật

- Repo này **riêng tư**. Khóa ký `app/kids-tv.jks` (mật khẩu `mmhykids`) nằm trong repo để các bản cài sau cài đè được nhau; đây không phải khóa phát hành lên cửa hàng ứng dụng.
- Mật khẩu theo ngày dễ đoán với bé lớn đã biết quy tắc. Muốn chắc hơn: Menu phụ huynh → Mật khẩu phụ huynh → đặt mật khẩu cố định.
- Ai có adb vào được TV (mạng LAN + Gỡ lỗi mạng đang bật) đều gỡ được bảo vệ. Tắt Gỡ lỗi mạng sau khi cài xong.
