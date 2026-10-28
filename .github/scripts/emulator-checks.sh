#!/usr/bin/env bash
# Chạy trên máy ảo trong CI: thử Kids TV MMHY như trên TV thật (màn hình 1920x1080), chụp ảnh từng bước.
# YouTube trên máy ảo là bản GIẢ (cùng tên gói com.google.android.youtube.tv, màn hình đỏ) vì máy ảo không có YouTube thật.
# Cách dùng: emulator-checks.sh <APK Kids TV> <APK YouTube giả> <thư mục kết quả>
set -u
APK="$1"
FAKE="$2"
OUT="$3"
PKG=com.minhminhhungyen.kidstv
ACT="$PKG/.MainActivity"
ADMIN="$PKG/.AdminReceiver"
KIDS=com.google.android.youtube.tv
FAKE_ACT="$KIDS/com.minhminhhungyen.fakekids.FakeKidsActivity"
HERE="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$OUT/screens" "$OUT/ui"
STEPS="$OUT/steps.txt"
RESULTS="$OUT/results.txt"
: > "$STEPS"
: > "$RESULTS"
PASSES=0
FAILS=0
N=0

log() { echo "$*" | tee -a "$STEPS"; }
pass() { PASSES=$((PASSES + 1)); echo "ĐẠT    $*" | tee -a "$RESULTS"; }
fail() { FAILS=$((FAILS + 1)); echo "HỎNG   $*" | tee -a "$RESULTS"; }
warn() { echo "LƯU Ý  $*" | tee -a "$RESULTS"; }
section() { echo "" | tee -a "$RESULTS"; echo "== $*" | tee -a "$RESULTS"; }

# ---- Đọc trạng thái ------------------------------------------------------------------------------------------
focus_line() { adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -m1 'mCurrentFocus'; }

# wait_focus <chuỗi có trong cửa sổ đang nhận phím> <số giây chờ tối đa>
wait_focus() {
  local i max=$(( ${2:-10} * 2 ))
  for ((i = 0; i < max; i++)); do
    if focus_line | grep -q -- "$1"; then return 0; fi
    sleep 0.5
  done
  log "  (chờ cửa sổ '$1' nhưng đang là: $(focus_line))"
  return 1
}

# focus_never <chuỗi> <số giây>: trong suốt thời gian đó cửa sổ nhận phím không được chứa chuỗi
focus_never() {
  local i
  for ((i = 0; i < $2; i++)); do
    if focus_line | grep -q -- "$1"; then return 1; fi
    sleep 1
  done
  return 0
}

klog() { adb logcat -d -s KidsTV:V 2>/dev/null | tr -d '\r'; }

# admin <HÀNH ĐỘNG> [tham số am]: gửi lệnh qua cổng adb của ứng dụng, in kết quả trả về
admin() {
  local a="$1"
  shift
  adb shell am broadcast -n "$ADMIN" -a "$PKG.$a" "$@" 2>&1 | tr -d '\r' | sed -n 's/.*data="\(.*\)".*/\1/p'
}
status() { admin STATUS; }
field() { status | tr ' ' '\n' | sed -n "s/^$1=//p" | head -1; }
rem() { local v; v=$(field remaining); echo "${v%s}"; }

# wait_field <tên> <giá trị> <số giây>
wait_field() {
  local i max=$(( ${3:-10} * 2 )) v=""
  for ((i = 0; i < max; i++)); do
    v=$(field "$1")
    if [ "$v" = "$2" ]; then return 0; fi
    sleep 0.5
  done
  log "  (chờ $1=$2 nhưng đang là '$v')"
  return 1
}

# Dịch vụ bảo vệ bị uiautomator tạm gỡ trong lúc chụp cây giao diện: chờ nó chạy lại trước khi thử chặn.
guard_ready() { wait_field guardRunning true 15 && sleep 1; }

# ---- Đọc giao diện (uiautomator) -------------------------------------------------------------------------------
dump_ui() {
  local i
  for i in 1 2 3; do
    adb shell rm -f /sdcard/ui.xml > /dev/null 2>&1
    adb shell uiautomator dump /sdcard/ui.xml > "$OUT/ui/dump-log.txt" 2>&1
    adb exec-out cat /sdcard/ui.xml > "$OUT/ui/last.xml" 2>/dev/null
    if grep -q '<hierarchy' "$OUT/ui/last.xml"; then return 0; fi
    sleep 1
  done
  log "  (không chụp được cây giao diện: $(head -c 200 "$OUT/ui/dump-log.txt"))"
  return 1
}

# ui_has <exact|prefix|contains|class> <giá trị>
ui_has() { dump_ui && python3 "$HERE/ui_find.py" "$OUT/ui/last.xml" "$@" > /dev/null; }

# wait_ui <exact|prefix|contains> <giá trị> <số lần thử, mỗi lần ~2 giây>
wait_ui() {
  local i
  for ((i = 0; i < ${3:-8}; i++)); do
    if ui_has "$1" "$2"; then return 0; fi
    sleep 1
  done
  log "  (chờ chữ '$2' nhưng không thấy)"
  return 1
}

# tap_ui <exact|prefix|contains> <giá trị>
tap_ui() {
  local xy
  dump_ui || return 1
  if ! xy=$(python3 "$HERE/ui_find.py" "$OUT/ui/last.xml" "$@"); then
    log "KHÔNG THẤY để chạm: $*"
    return 1
  fi
  # shellcheck disable=SC2086
  adb shell input tap $xy
  log "chạm: $* ($xy)"
  sleep 1.2
}

dismiss_error_dialog() {
  local xy
  if focus_line | grep -q -E 'Application Not Responding|Application Error'; then
    warn "hộp thoại lỗi của hệ thống che màn hình: $(focus_line)"
    if dump_ui && xy=$(python3 "$HERE/ui_find.py" "$OUT/ui/last.xml" exact "Close app"); then
      # shellcheck disable=SC2086
      adb shell input tap $xy
    else
      adb shell input keyevent 4
    fi
    sleep 2
  fi
}

shot() {
  dismiss_error_dialog
  N=$((N + 1))
  local name
  name=$(printf "%02d-%s" "$N" "$1")
  adb exec-out screencap -p > "$OUT/screens/$name.png"
  log "ảnh: $name"
}

# Bài toán: solve right|wrong → bấm đáp án đúng/sai của bài đang hiện; đặt LAST_Q
LAST_Q=""
solve() {
  local line="" q ans right wrong i
  for i in 1 2 3 4 5 6; do
    if dump_ui && line=$(python3 "$HERE/ui_math.py" "$OUT/ui/last.xml"); then break; fi
    line=""
    sleep 1
  done
  if [ -z "$line" ]; then
    log "KHÔNG ĐỌC ĐƯỢC bài toán"
    return 1
  fi
  IFS='|' read -r q ans right wrong <<< "$line"
  log "  bài toán: $q (đáp án $ans) → bấm $1"
  if [ "$1" = right ]; then
    # shellcheck disable=SC2086
    adb shell input tap $right
  else
    # shellcheck disable=SC2086
    adb shell input tap $wrong
  fi
  LAST_Q="$q"
}

# Chờ bài toán MỚI (khác bài vừa làm) hiện ra
wait_new_question() {
  local i line q
  for ((i = 0; i < 10; i++)); do
    if dump_ui && line=$(python3 "$HERE/ui_math.py" "$OUT/ui/last.xml"); then
      q="${line%%|*}"
      if [ "$q" != "$LAST_Q" ]; then return 0; fi
    fi
    sleep 1
  done
  return 1
}

# Mở màn hình khóa ngay (hết giờ giả lập) và chờ nó hiện
lock_now() {
  admin EXPIRE > /dev/null
  wait_field state LOCKED 6 && wait_focus "kidstv.MainActivity" 10 && wait_ui prefix "Hết giờ xem rồi" 8
}

# Từ màn hình khóa: bấm "Phụ huynh" để tới màn hình nhập mật khẩu
open_password_screen() {
  tap_ui exact "Phụ huynh" && wait_ui prefix "Nhập mật khẩu phụ huynh" 6
}

device_today() { adb shell date +%d%m%Y | tr -d '\r'; }

# ======================================================================================================================
adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done' || true
adb shell input keyevent 82 || true
adb shell settings put system screen_off_timeout 1800000 || true
adb shell settings put secure anr_show_background 0 || true
adb shell svc power stayon true || true
# Màn hình giống TV: 1920x1080, mật độ 320 (đúng cỡ 960x540 dp mà ứng dụng thiết kế)
adb shell wm size 1920x1080 || true
adb shell wm density 320 || true
adb shell input keyevent 3 || true
sleep 15
dismiss_error_dialog

section "0. Cài đặt"
adb install -r "$APK" > "$OUT/install-app.txt" 2>&1
cat "$OUT/install-app.txt"
if grep -q Success "$OUT/install-app.txt"; then pass "Cài Kids TV MMHY"; else fail "Cài Kids TV MMHY: $(cat "$OUT/install-app.txt")"; fi
adb install -r "$FAKE" > "$OUT/install-fake.txt" 2>&1
cat "$OUT/install-fake.txt"
if grep -q Success "$OUT/install-fake.txt"; then pass "Cài YouTube giả"; else fail "Cài YouTube giả: $(cat "$OUT/install-fake.txt")"; fi
# Đúng các lệnh adb mà bat trên Windows sẽ chạy trên TV thật
adb shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS > "$OUT/grant.txt" 2>&1 || true
adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow >> "$OUT/grant.txt" 2>&1 || true
adb shell cmd package set-home-activity "$ACT" >> "$OUT/grant.txt" 2>&1 || true
cat "$OUT/grant.txt"
adb logcat -c
adb shell am start -W -n "$ACT" > "$OUT/start.txt" 2>&1
cat "$OUT/start.txt"

section "1. Màn hình chính"
if wait_focus "kidstv.MainActivity" 15; then pass "Màn hình chính mở được"; else fail "Màn hình chính không mở"; fi
if wait_field guardRunning true 20; then pass "Dịch vụ bảo vệ tự bật (không cần vào Cài đặt trợ năng)"; else fail "Dịch vụ bảo vệ không chạy"; fi
ST=$(status)
log "trạng thái: $ST"
echo "$ST" | grep -q 'defaultHome=true' && pass "Là màn hình chính mặc định" || fail "Chưa là màn hình chính mặc định"
echo "$ST" | grep -q 'kidApp=true' && pass "Thấy YouTube đã cài" || fail "Không thấy YouTube"
echo "$ST" | grep -q 'state=RUNNING' && pass "Lần đầu trong ngày: tự cấp phiên xem miễn phí" || fail "Không có phiên xem miễn phí lúc đầu ngày: $ST"
R=$(rem)
if [ -n "$R" ] && [ "$R" -ge 1700 ] && [ "$R" -le 1800 ]; then pass "Phiên miễn phí đầu ngày = 30 phút (còn ${R}s)"; else fail "Thời gian phiên đầu ngày lạ: '${R}'"; fi

# Thời tiết: chờ tối đa 25 giây cho một lần tải
for _ in $(seq 1 25); do
  if klog | grep -q 'thoi tiet'; then break; fi
  sleep 1
done
klog | grep 'thoi tiet' | tail -3 | tee -a "$STEPS"
if klog | grep -q 'thoi tiet: da cap nhat'; then pass "Tải được thời tiết Open-Meteo và đọc được dữ liệu"; else warn "Chưa tải/đọc được thời tiết (xem steps.txt, logcat.txt)"; fi
sleep 3
shot home
if ui_has exact "--°C"; then warn "Ô thời tiết vẫn trống (--°C)"; else pass "Ô thời tiết có số liệu"; fi
if ui_has prefix "Âm lịch:"; then pass "Có dòng ngày âm lịch"; else fail "Thiếu dòng ngày âm lịch"; fi
if ui_has exact "YouTube"; then pass "Có ô YouTube"; else fail "Thiếu ô YouTube"; fi
# Chữ trên màn hình chính phải đổi ít để uiautomator (và trợ năng của TV) không bị ngập sự kiện
if ui_has prefix "Còn "; then pass "Có thanh thời gian còn lại"; else fail "Thiếu thanh thời gian còn lại"; fi

section "2. Remote: Back không thoát, OK mở YouTube, Home quay về"
adb shell input keyevent 4
sleep 1.5
if focus_line | grep -q 'kidstv.MainActivity'; then pass "Nút Quay lại ở màn hình trẻ em không thoát ra"; else fail "Nút Quay lại làm thoát màn hình trẻ em: $(focus_line)"; fi
# Lần bấm phím điều hướng đầu tiên trên máy ảo chỉ thoát "chế độ cảm ứng"; TV thật không có chế độ này.
OPENED=1
for n in 1 2 3; do
  adb shell input keyevent 23
  if wait_focus "fakekids.FakeKidsActivity" 5; then OPENED=0; break; fi
done
if [ $OPENED -eq 0 ]; then pass "OK trên ô YouTube mở được YouTube"; else fail "OK không mở được YouTube"; fi
shot youtube-kids
adb shell input keyevent 3
if wait_focus "kidstv.MainActivity" 10; then pass "Nút Home từ YouTube về màn hình trẻ em"; else fail "Nút Home không về màn hình trẻ em"; fi

section "3. Chặn Cài đặt và ứng dụng khác"
guard_ready
adb logcat -c
adb shell am start -a android.settings.SETTINGS > /dev/null 2>&1
if wait_focus "com.android.settings" 6; then log "Cài đặt đã hiện lên, chờ bị chặn"; else warn "không bắt được cửa sổ Cài đặt kịp lúc (có thể đã bị chặn rất nhanh)"; fi
if wait_focus "kidstv.MainActivity" 10; then pass "Mở Cài đặt → bị đẩy về màn hình trẻ em"; else fail "Cài đặt không bị chặn: $(focus_line)"; fi
if klog | grep -q 'chan com.android.settings'; then pass "Nhật ký ghi đã chặn Cài đặt"; else fail "Nhật ký không có dòng chặn Cài đặt"; fi
shot settings-blocked

OTHER_PKG=""
for cand in com.android.camera2 com.android.gallery3d com.android.deskclock com.android.calendar com.android.contacts \
  com.android.dialer com.android.messaging com.android.documentsui com.android.music com.android.calculator2 \
  com.android.email com.android.soundrecorder com.android.traceur; do
  if adb shell pm path "$cand" 2> /dev/null | grep -q '^package:'; then OTHER_PKG="$cand"; break; fi
done
if [ -n "$OTHER_PKG" ]; then
  log "ứng dụng thường để thử chặn: $OTHER_PKG"
  adb logcat -c
  adb shell monkey -p "$OTHER_PKG" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
  sleep 1
  if wait_focus "kidstv.MainActivity" 10 && klog | grep -q "chan $OTHER_PKG"; then
    pass "Ứng dụng lạ ($OTHER_PKG) bị chặn và đẩy về màn hình trẻ em"
  else
    fail "Ứng dụng lạ ($OTHER_PKG) không bị chặn: $(focus_line)"
  fi
else
  warn "Máy ảo không có ứng dụng thường nào khác trong danh sách để thử chặn"
fi

section "4. Hết giờ: màn hình khóa"
guard_ready
if lock_now; then pass "Hết giờ → màn hình khóa hiện ra"; else fail "Hết giờ nhưng không thấy màn hình khóa"; fi
shot locked
adb logcat -c
adb shell am start -n "$FAKE_ACT" > /dev/null 2>&1
if wait_focus "kidstv.MainActivity" 10 && klog | grep -q 'khi dang khoa'; then
  pass "Đang khóa mà mở YouTube → bị đẩy về màn hình khóa"
else
  fail "YouTube mở được lúc đang khóa: $(focus_line)"
fi

section "5. Bài toán: đúng liên tiếp mới được xem thêm"
admin CONFIG --ei streak 3 --ei bonus 1 > /dev/null
lock_now
if ui_has contains "3 câu"; then pass "Màn hình khóa nêu đúng số câu cần làm (3, theo cấu hình thử)"; else warn "Không thấy '3 câu' trên màn hình khóa"; fi
shot math-question
solve right
sleep 0.8
if wait_field streak "1/3" 6; then pass "Trả lời đúng: chuỗi đúng tăng lên 1"; else fail "Chuỗi đúng không tăng: $(field streak)"; fi
shot math-after-right
wait_new_question || log "  (không thấy bài mới)"
solve wrong
if ui_has prefix "Chưa đúng"; then pass "Có lời báo trả lời chưa đúng kèm đáp án"; else warn "Không thấy lời báo 'Chưa đúng' (có thể đã tắt)"; fi
shot math-after-wrong
if wait_field streak "0/3" 6; then pass "Trả lời sai: chuỗi đúng về 0"; else fail "Trả lời sai nhưng chuỗi không về 0: $(field streak)"; fi
wait_new_question || log "  (không thấy bài mới)"
for k in 1 2 3; do
  solve right || break
  if [ "$k" -lt 3 ]; then
    sleep 0.8
    wait_new_question || log "  (không thấy bài mới)"
  fi
done
if wait_field state RUNNING 10; then pass "Đủ 3 câu đúng liên tiếp → được xem tiếp (thưởng 1 phút theo cấu hình thử)"; else fail "Đủ câu đúng nhưng không được xem tiếp: $(status)"; fi
R=$(rem)
if [ -n "$R" ] && [ "$R" -ge 1 ] && [ "$R" -le 60 ]; then pass "Thời gian thưởng đúng (còn ${R}s)"; else fail "Thời gian thưởng lạ: '${R}'"; fi
if wait_focus "fakekids.FakeKidsActivity" 8; then pass "Được thưởng xong thì tự mở YouTube"; else warn "Sau khi thưởng không tự mở YouTube: $(focus_line)"; fi
shot after-reward
if status | grep -q 'bonusToday=1'; then pass "Đếm lượt thưởng trong ngày"; else warn "Số lượt thưởng trong ngày lạ: $(status)"; fi
admin CONFIG --ei streak 20 --ei bonus 15 > /dev/null

section "6. Mật khẩu phụ huynh = ngày hôm nay (ddMMyyyy)"
guard_ready
adb shell input keyevent 3
sleep 1
lock_now || fail "Không khóa được để thử mật khẩu"
if open_password_screen; then pass "Nút Phụ huynh mở màn hình nhập mật khẩu"; else fail "Không mở được màn hình nhập mật khẩu"; fi
shot password
adb shell input text 11111111
if wait_ui prefix "Sai mật khẩu (1/5)" 6; then pass "Mật khẩu sai bị từ chối"; else fail "Mật khẩu sai không bị từ chối"; fi
shot password-wrong
for _ in 1 2 3 4; do
  adb shell input text 11111111
  sleep 1.2
done
if wait_ui contains "Chờ 60 giây" 6; then pass "Sai 5 lần thì bị khóa 60 giây"; else fail "Sai 5 lần không bị khóa"; fi
shot password-lockout
adb shell input text "$(device_today)"
if wait_ui prefix "Thử lại sau" 6; then pass "Đang bị khóa 60 giây thì mật khẩu đúng cũng chưa nhận"; else fail "Trong lúc khóa 60 giây mật khẩu đúng vẫn được nhận"; fi
admin RESET_PWD > /dev/null
TODAY=$(device_today)
log "mật khẩu hôm nay trên máy ảo: $TODAY"
adb shell input text "$TODAY"
if wait_ui exact "Menu phụ huynh" 8; then pass "Mật khẩu = ngày hôm nay ($TODAY) mở được Menu phụ huynh"; else fail "Mật khẩu ngày $TODAY không mở được Menu phụ huynh"; fi
shot parent-menu

section "7. Menu phụ huynh"
# 7a. Đặt mật khẩu cố định 1234
if tap_ui exact "Mật khẩu phụ huynh" && wait_ui prefix "Mật khẩu cố định mới" 6; then
  adb shell input text 1234
  tap_ui exact "OK"
  if wait_ui prefix "Nhập lại mật khẩu mới" 6; then
    adb shell input text 1234
    tap_ui exact "OK"
  fi
  if wait_ui exact "Menu phụ huynh" 8 && [ "$(field customPwd)" = "true" ]; then pass "Đặt được mật khẩu cố định 1234"; else fail "Đặt mật khẩu cố định không thành"; fi
else
  fail "Không vào được màn hình đặt mật khẩu cố định"
fi
shot parent-menu-custom-password
# 7b. Cho bé xem N phút
if tap_ui prefix "Cho bé xem"; then
  if wait_field state RUNNING 6 && wait_focus "fakekids.FakeKidsActivity" 8; then pass "Phụ huynh bấm 'Cho bé xem' → bé xem được ngay"; else fail "Cho bé xem không có tác dụng: $(status)"; fi
fi
R=$(rem)
if [ -n "$R" ] && [ "$R" -ge 1700 ]; then pass "Thời lượng đúng 30 phút (còn ${R}s)"; else warn "Thời lượng lạ: '${R}'"; fi
# 7c. Mật khẩu cố định hoạt động, mật khẩu ngày không còn hiệu lực
guard_ready
adb shell input keyevent 3
sleep 1
lock_now || fail "Không khóa được để thử mật khẩu cố định"
open_password_screen
adb shell input text 1234
if wait_ui exact "Menu phụ huynh" 8; then pass "Mật khẩu cố định 1234 mở được Menu phụ huynh (tự nhận khi gõ đủ 4 số)"; else fail "Mật khẩu cố định không hoạt động"; fi
admin RESET_PWD > /dev/null
if [ "$(field customPwd)" = "false" ]; then pass "Lệnh đặt lại mật khẩu quay về mật khẩu theo ngày"; else fail "Đặt lại mật khẩu không có tác dụng"; fi
# 7d. Mở Cài đặt TV trong chế độ phụ huynh, hết chế độ thì tự về
guard_ready
if tap_ui prefix "Mở Cài đặt"; then
  if wait_focus "com.android.settings" 8; then pass "Phụ huynh mở được Cài đặt TV"; else fail "Không mở được Cài đặt trong chế độ phụ huynh: $(focus_line)"; fi
  sleep 3
  if focus_never "kidstv.MainActivity" 7; then pass "Trong chế độ phụ huynh Cài đặt không bị chặn"; else fail "Cài đặt vẫn bị chặn trong chế độ phụ huynh"; fi
  shot parent-settings-open
  admin PARENT_OFF > /dev/null
  if wait_focus "kidstv.MainActivity" 12; then pass "Hết chế độ phụ huynh → tự về màn hình trẻ em"; else fail "Hết chế độ phụ huynh nhưng vẫn ở Cài đặt: $(focus_line)"; fi
fi
# 7e. Khóa TV ngay (từ Menu phụ huynh)
lock_now >/dev/null 2>&1
if open_password_screen; then
  adb shell input text "$(device_today)"
  if wait_ui exact "Menu phụ huynh" 8 && tap_ui exact "Khóa TV ngay"; then
    if wait_field state LOCKED 6 && wait_ui prefix "Hết giờ xem rồi" 8; then pass "Khóa TV ngay hoạt động"; else fail "Khóa TV ngay không có tác dụng"; fi
  else
    fail "Không vào được Menu phụ huynh để thử Khóa TV ngay"
  fi
fi

section "8. Đến giờ: báo trước, tạm dừng khi tắt màn hình, tự khóa"
admin PARENT_OFF > /dev/null
guard_ready
adb logcat -c
admin GRANT --ei seconds 65 > /dev/null
adb shell am start -n "$FAKE_ACT" > /dev/null 2>&1
wait_focus "fakekids.FakeKidsActivity" 8 || warn "Không mở được YouTube giả để thử hết giờ"
shot timer-running
for _ in $(seq 1 14); do
  if klog | grep -q 'alarm com.minhminhhungyen.kidstv.alarm.WARN1'; then break; fi
  sleep 1
done
if klog | grep -q 'alarm com.minhminhhungyen.kidstv.alarm.WARN1'; then pass "Báo thức nhắc trước 1 phút đã chạy"; else fail "Báo thức nhắc trước 1 phút không chạy"; fi
guard_ready
BEFORE=$(rem)
adb shell input keyevent 223
if wait_field state PAUSED 8; then pass "Tắt màn hình TV → tạm dừng đếm giờ"; else fail "Tắt màn hình nhưng không tạm dừng: $(status)"; fi
sleep 9
adb shell input keyevent 224
adb shell wm dismiss-keyguard > /dev/null 2>&1
adb shell input keyevent 82
if wait_field state RUNNING 10; then pass "Bật lại màn hình → đếm tiếp"; else fail "Bật lại màn hình nhưng không đếm tiếp: $(status)"; fi
AFTER=$(rem)
log "còn lại trước khi tắt: ${BEFORE}s, sau khi bật lại: ${AFTER}s"
if [ -n "$BEFORE" ] && [ -n "$AFTER" ] && [ "$AFTER" -ge $((BEFORE - 6)) ]; then pass "Giờ không bị trừ trong lúc tắt màn hình"; else fail "Giờ bị trừ trong lúc tắt màn hình (${BEFORE}s → ${AFTER}s)"; fi
# Chờ hết giờ thật: màn hình khóa phải tự hiện trên YouTube
adb shell am start -n "$FAKE_ACT" > /dev/null 2>&1
wait_focus "fakekids.FakeKidsActivity" 8 || warn "Không mở lại được YouTube giả sau khi bật màn hình"
WAIT=$(( ${AFTER:-60} + 25 ))
if wait_focus "kidstv.MainActivity" "$WAIT" && wait_field state LOCKED 8; then pass "Hết giờ thật → màn hình khóa tự hiện lên trên YouTube"; else fail "Hết giờ thật nhưng không tự khóa: $(status)"; fi
shot timeup
if wait_ui prefix "Hết giờ xem rồi" 8; then pass "Màn hình khóa đang hiển thị sau khi hết giờ"; else fail "Không thấy chữ trên màn hình khóa sau khi hết giờ"; fi
klog | grep -E 'alarm|bringToFront|goHome|expire|pause|resume|grant' | tail -25 | tee -a "$STEPS"
if klog | grep -q 'alarm com.minhminhhungyen.kidstv.alarm.END'; then pass "Báo thức hết giờ đã chạy"; else warn "Báo thức hết giờ không thấy trong nhật ký (khóa do dịch vụ bảo vệ)"; fi
if klog | grep -q 'bringToFront: timeup'; then pass "Báo thức tự đưa màn hình khóa lên trước (đặc quyền chạy nền hoạt động)"; else warn "Màn hình khóa lên nhờ dịch vụ bảo vệ chứ không phải nhờ báo thức"; fi
adb logcat -d 2>/dev/null | grep -i 'Background activity start' | grep -i kidstv | head -3 | tee -a "$STEPS" > /dev/null

# ---- Tổng kết -------------------------------------------------------------------------------------------------
sleep 2
adb logcat -d > "$OUT/logcat.txt"
grep -E 'FATAL EXCEPTION| E AndroidRuntime' "$OUT/logcat.txt" > "$OUT/crashes.txt" || true
grep -E 'ANR in com.minhminhhungyen|am_anr.*kidstv' "$OUT/logcat.txt" > "$OUT/anr.txt" || true
PID=$(adb shell pidof "$PKG" | tr -d '\r')
{
  echo "pid=$PID"
  echo "android=$(adb shell getprop ro.build.version.release | tr -d '\r') sdk=$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
  adb shell dumpsys package "$PKG" | grep -E 'versionName|versionCode|targetSdk' | head -4
  adb shell wm size
  adb shell wm density
  echo "trạng thái cuối: $(status)"
  echo "đạt=$PASSES hỏng=$FAILS"
} > "$OUT/summary.txt"
cat "$OUT/summary.txt"

if [ -s "$OUT/crashes.txt" ]; then
  fail "Ứng dụng bị crash trong lúc thử"
  cat "$OUT/crashes.txt"
fi
if [ -s "$OUT/anr.txt" ]; then
  fail "Ứng dụng bị treo (ANR) trong lúc thử"
  cat "$OUT/anr.txt"
fi
if [ -z "$PID" ]; then
  fail "Tiến trình ứng dụng không còn chạy ở cuối bài thử"
fi

echo ""
echo "===== KẾT QUẢ ====="
cat "$RESULTS"
echo "Đạt: $PASSES, hỏng: $FAILS"
[ "$FAILS" -eq 0 ]
