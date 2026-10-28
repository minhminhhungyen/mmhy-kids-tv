@echo off
setlocal
set "ADB=%~dp0platform-tools\adb.exe"
set "S=192.168.31.83:5555"
set "OUT=%~dp0out"
set "PKG=com.minhminhhungyen.kidstv"
if not exist "%OUT%" mkdir "%OUT%"

echo Ket noi TV %S% ...
"%ADB%" connect %S%
"%ADB%" -s %S% get-state > "%OUT%\state.txt" 2>&1
findstr /x /c:"device" "%OUT%\state.txt" >nul
if errorlevel 1 goto KHONG_KET_NOI

echo Dang lay nhat ky Kids TV ...
"%ADB%" -s %S% logcat -d -v time -s KidsTV:V > "%OUT%\nhat-ky-kidstv.txt" 2>&1
"%ADB%" -s %S% logcat -d -b crash > "%OUT%\nhat-ky-crash.txt" 2>&1
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.STATUS > "%OUT%\trang-thai.txt" 2>&1
"%ADB%" -s %S% shell dumpsys window | findstr /c:"mCurrentFocus" > "%OUT%\cua-so-hien-tai.txt" 2>&1
"%ADB%" -s %S% shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > "%OUT%\home-hien-tai.txt" 2>&1

echo Dang lay cac lan tat/bat TV [con nguyen ca sau khi TV khoi dong lai] ...
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.EVENTS > "%OUT%\su-kien-tv.txt" 2>&1

echo Dang lay thong tin nguon dien cua TV [nut Nguon bam ra gi] ...
"%ADB%" -s %S% shell "date; cat /proc/uptime" > "%OUT%\gio-va-uptime.txt" 2>&1
"%ADB%" -s %S% shell "getprop sys.boot.reason; getprop sys.boot.reason.last; getprop persist.sys.boot.reason; getprop ro.boot.bootreason; getprop ro.product.model; getprop ro.build.fingerprint" > "%OUT%\ly-do-khoi-dong.txt" 2>&1
"%ADB%" -s %S% shell dumpsys power > "%OUT%\nguon-dien.txt" 2>&1
"%ADB%" -s %S% logcat -d -b events -v time > "%OUT%\events-buffer-day-du.txt" 2>&1
findstr /i /c:"boot_progress_start" /c:"screen_toggled" /c:"power_screen_state" /c:"power_sleep_requested" /c:"power_soft_sleep" /c:"sleep_state" "%OUT%\events-buffer-day-du.txt" > "%OUT%\events-nguon.txt"
echo.
type "%OUT%\trang-thai.txt"
echo.
echo Da luu trong thu muc out: nhat-ky-kidstv.txt, su-kien-tv.txt, events-nguon.txt, nguon-dien.txt, ly-do-khoi-dong.txt, gio-va-uptime.txt, trang-thai.txt
echo Quay lai Claude va nhan: xong nhat ky
pause
exit /b 0

:KHONG_KET_NOI
echo.
echo KHONG KET NOI DUOC TV. Kiem tra TV dang bat, cung mang, da bat ADB.
pause
exit /b 1
