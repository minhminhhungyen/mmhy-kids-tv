@echo off
setlocal
set "ADB=%~dp0platform-tools\adb.exe"
set "S=192.168.31.83:5555"
set "OUT=%~dp0out"
set "APK=%~dp0apk\MMHY-KidsTV-release.apk"
set "PKG=com.minhminhhungyen.kidstv"
set "KID=com.google.android.youtube.tv"
set "LOG=%OUT%\cai-launcher.log"
if not exist "%OUT%" mkdir "%OUT%"
echo ===== BUOC 5 %date% %time% ===== >> "%LOG%"

echo [1/9] Ket noi TV %S% ...
"%ADB%" connect %S%
"%ADB%" -s %S% get-state > "%OUT%\state.txt" 2>&1
findstr /x /c:"device" "%OUT%\state.txt" >nul
if errorlevel 1 goto KHONG_KET_NOI

echo [2/9] Kiem tra tep cai dat va ung dung YouTube tren TV ...
if not exist "%APK%" goto THIEU_APK
"%ADB%" -s %S% shell pm path %KID% > "%OUT%\youtube-check.txt" 2>&1
findstr /b /c:"package:" "%OUT%\youtube-check.txt" >nul
if errorlevel 1 goto CHUA_CO_YOUTUBE

echo [3/9] Luu trang thai cu de hoan tac duoc ...
"%ADB%" -s %S% shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME > "%OUT%\home-truoc-buoc5.txt" 2>&1
"%ADB%" -s %S% shell settings get secure enabled_accessibility_services > "%OUT%\a11y-truoc-buoc5.txt" 2>&1
"%ADB%" -s %S% shell pm list packages -d --user 0 > "%OUT%\tat-truoc-buoc5.txt" 2>&1

echo [4/9] Cai Kids TV MMHY len TV ...
echo Neu TV hien hop thoai Play Protect hoac Cai dat, dung remote chon Van cai dat [Install anyway].
"%ADB%" -s %S% install -r "%APK%" > "%OUT%\cai-apk.txt" 2>&1
type "%OUT%\cai-apk.txt"
findstr /c:"Success" "%OUT%\cai-apk.txt" >nul
if errorlevel 1 goto CAI_LOI

echo [5/9] Cap quyen cho ung dung ...
"%ADB%" -s %S% shell pm grant %PKG% android.permission.WRITE_SECURE_SETTINGS > "%OUT%\cap-quyen.txt" 2>&1
"%ADB%" -s %S% shell appops set %PKG% SYSTEM_ALERT_WINDOW allow >> "%OUT%\cap-quyen.txt" 2>&1
"%ADB%" -s %S% shell dumpsys deviceidle whitelist +%PKG% >> "%OUT%\cap-quyen.txt" 2>&1
type "%OUT%\cap-quyen.txt"

echo [6/9] Bat lai ung dung YouTube [buoc 2 truoc day da tat no] ...
"%ADB%" -s %S% shell pm enable --user 0 %KID% > "%OUT%\bat-youtube.txt" 2>&1
type "%OUT%\bat-youtube.txt"
"%ADB%" -s %S% shell pm list packages -d --user 0 > "%OUT%\tat-sau-bat-youtube.txt" 2>&1
findstr /x /c:"package:%KID%" "%OUT%\tat-sau-bat-youtube.txt" >nul
if not errorlevel 1 goto YOUTUBE_VAN_TAT

echo [7/9] Dat lam man hinh chinh mac dinh cua TV ...
"%ADB%" -s %S% shell cmd package set-home-activity --user 0 %PKG%/%PKG%.MainActivity > "%OUT%\dat-man-hinh-chinh.txt" 2>&1
type "%OUT%\dat-man-hinh-chinh.txt"
"%ADB%" -s %S% shell cmd role get-role-holders --user 0 android.app.role.HOME > "%OUT%\role-home.txt" 2>&1
"%ADB%" -s %S% shell dumpsys package preferred > "%OUT%\preferred.txt" 2>&1

echo [8/9] Mo ung dung, cho 8 giay roi kiem tra ...
"%ADB%" -s %S% shell am start -n %PKG%/%PKG%.MainActivity > "%OUT%\mo-ung-dung.txt" 2>&1
ping -n 9 127.0.0.1 >nul
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.STATUS > "%OUT%\trang-thai.txt" 2>&1
type "%OUT%\trang-thai.txt"
findstr /c:"guardRunning=true" "%OUT%\trang-thai.txt" >nul
if errorlevel 1 echo CANH BAO: dich vu bao ve CHUA chay.
findstr /c:"kidApp=true" "%OUT%\trang-thai.txt" >nul
if errorlevel 1 echo CANH BAO: ung dung YouTube dang tat hoac chua co.
findstr /c:"defaultHome=true" "%OUT%\trang-thai.txt" >nul
if errorlevel 1 echo LUU Y: TV nay khong cho doi man hinh chinh mac dinh. Van chan duoc bang dich vu bao ve, chi la bam HOME thi man hinh nhay nhe mot cai.
"%ADB%" -s %S% shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > "%OUT%\home-sau-buoc5.txt" 2>&1
"%ADB%" -s %S% shell pm list packages -d --user 0 > "%OUT%\tat-sau-buoc5.txt" 2>&1
"%ADB%" -s %S% exec-out screencap -p > "%OUT%\tv-man-hinh-chinh.png"
"%ADB%" -s %S% logcat -d -s KidsTV:V > "%OUT%\nhat-ky-buoc5.txt" 2>&1

echo [9/9] Xong.
echo.
echo XONG BUOC 5. Nhin man hinh TV: phai thay gio, thoi tiet, lich va o YouTube.
echo.
echo VIEC CON LAI LAM MOT LAN TREN TV [quan trong, de be khong vao duoc tai khoan nguoi lon]:
echo   Bam nut Phu huynh, nhap mat khau, chon Dung TV binh thuong, mo YouTube.
echo   Chon ho so cua be [Kids]. Vao Cai dat o ben trai, chon Ma phu huynh, dat 4 so.
echo   Xong bam HOME, vao lai Phu huynh, bam Thoat phu huynh.
echo.
echo Quay lai Claude va nhan: xong buoc 5
pause
exit /b 0

:THIEU_APK
echo.
echo KHONG THAY TEP CAI DAT: %APK%
echo Hay cho Claude chep tep MMHY-KidsTV-release.apk vao thu muc apk. CHUA CAI GI.
pause
exit /b 1

:CHUA_CO_YOUTUBE
echo.
echo CHUA THAY UNG DUNG YOUTUBE [%KID%] TREN TV. CHUA CAI GI.
echo Hay mo Google Play tren TV, tim YouTube, cai dat. Roi chay lai file nay.
pause
exit /b 1

:YOUTUBE_VAN_TAT
echo.
echo CANH BAO: ung dung YouTube van dang tat sau khi bat lai. Xem out\bat-youtube.txt
echo Kids TV da cai nhung o YouTube se hien Chua co YouTube. Quay lai Claude va dan noi dung tren.
pause
exit /b 1

:CAI_LOI
echo.
echo CAI DAT KHONG THANH CONG. Noi dung loi nam o tren va trong out\cai-apk.txt
echo TV van giu nguyen nhu cu. Quay lai Claude va dan noi dung loi.
pause
exit /b 1

:KHONG_KET_NOI
echo.
echo KHONG KET NOI DUOC TV. Kiem tra TV dang bat, cung mang, da bat ADB. CHUA CAI GI.
pause
exit /b 1
