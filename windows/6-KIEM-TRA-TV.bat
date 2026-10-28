@echo off
setlocal
set "ADB=%~dp0platform-tools\adb.exe"
set "S=192.168.31.83:5555"
set "OUT=%~dp0out"
set "PKG=com.minhminhhungyen.kidstv"
set "KIDS=com.google.android.youtube.tv"
set "R=%OUT%\kiem-tra.txt"
set "FAILS=0"
set "TRIES=1"
if not exist "%OUT%" mkdir "%OUT%"
echo ===== KIEM TRA TV %date% %time% ===== > "%R%"

echo [0] Ket noi TV %S% ...
"%ADB%" connect %S%
"%ADB%" -s %S% get-state > "%OUT%\state.txt" 2>&1
findstr /x /c:"device" "%OUT%\state.txt" >nul
if errorlevel 1 goto KHONG_KET_NOI
"%ADB%" -s %S% shell pm list packages %PKG% > "%OUT%\kt-goi.txt" 2>&1
findstr /c:"%PKG%" "%OUT%\kt-goi.txt" >nul
if errorlevel 1 goto CHUA_CAI

echo Dang xoa nhat ky cu va cap 10 phut xem de thu ...
"%ADB%" -s %S% logcat -c
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.GRANT --ei minutes 10 > "%OUT%\kt-grant.txt" 2>&1
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.STATUS > "%OUT%\kt-trang-thai.txt" 2>&1
type "%OUT%\kt-trang-thai.txt"
type "%OUT%\kt-trang-thai.txt" >> "%R%"
call :FIND "%OUT%\kt-trang-thai.txt" "guardRunning=true" "Dich vu bao ve dang chay"
call :FIND "%OUT%\kt-trang-thai.txt" "kidApp=true" "Thay ung dung YouTube dang bat"
call :NOTE "%OUT%\kt-trang-thai.txt" "defaultHome=true" "La man hinh chinh mac dinh cua TV [khong bat buoc, khong dat thi bam HOME se nhay nhe]"

echo [1] Bam HOME ...
"%ADB%" -s %S% shell input keyevent KEYCODE_HOME
call :CHK "kidstv.MainActivity" "HOME ve man hinh tre em" 5
"%ADB%" -s %S% exec-out screencap -p > "%OUT%\tv-1-man-hinh-chinh.png"

echo [2] Thu mo Cai dat TV ...
"%ADB%" -s %S% shell am start -a android.settings.SETTINGS > "%OUT%\kt-settings.txt" 2>&1
ping -n 6 127.0.0.1 >nul
call :CHK "kidstv.MainActivity" "Cai dat bi chan" 1
"%ADB%" -s %S% logcat -d -s KidsTV:V > "%OUT%\kt-nhat-ky-2.txt" 2>&1
call :FIND "%OUT%\kt-nhat-ky-2.txt" "chan com.android.tv.settings" "Nhat ky ghi da chan Cai dat"

echo [3] Thu mo Live TV cua TCL ...
"%ADB%" -s %S% shell am start -n com.tcl.tv/com.tcl.player.TVActivity > "%OUT%\kt-livetv.txt" 2>&1
ping -n 6 127.0.0.1 >nul
call :CHK "kidstv.MainActivity" "Live TV bi chan" 1
"%ADB%" -s %S% shell input keyevent KEYCODE_HOME
call :CHK "kidstv.MainActivity" "Ve lai man hinh tre em" 5

echo [4] Bam OK de mo YouTube, cho toi da 30 giay ...
"%ADB%" -s %S% shell input keyevent KEYCODE_DPAD_CENTER
call :CHK "%KIDS%/" "OK mo duoc YouTube" 15
ping -n 9 127.0.0.1 >nul
"%ADB%" -s %S% exec-out screencap -p > "%OUT%\tv-2-youtube.png"
"%ADB%" -s %S% shell "dumpsys window | grep mCurrentFocus" > "%OUT%\youtube-focus.txt" 2>&1
"%ADB%" -s %S% shell input keyevent KEYCODE_HOME
call :CHK "kidstv.MainActivity" "HOME tu YouTube ve man hinh tre em" 6

echo [5] Gia lap het gio ...
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.EXPIRE > "%OUT%\kt-expire.txt" 2>&1
ping -n 6 127.0.0.1 >nul
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.STATUS > "%OUT%\kt-trang-thai-khoa.txt" 2>&1
type "%OUT%\kt-trang-thai-khoa.txt" >> "%R%"
call :FIND "%OUT%\kt-trang-thai-khoa.txt" "state=LOCKED" "Het gio thi khoa"
call :CHK "kidstv.MainActivity" "Man hinh khoa dang hien" 3
"%ADB%" -s %S% exec-out screencap -p > "%OUT%\tv-3-man-hinh-khoa.png"

echo [6] Dang khoa ma mo YouTube ...
"%ADB%" -s %S% shell monkey -p %KIDS% -c android.intent.category.LEANBACK_LAUNCHER 1 > "%OUT%\kt-monkey.txt" 2>&1
ping -n 8 127.0.0.1 >nul
call :CHK "kidstv.MainActivity" "Dang khoa: YouTube bi chan" 1
"%ADB%" -s %S% exec-out screencap -p > "%OUT%\tv-4-sau-khi-chan.png"

echo [7] Dua TV ve trang thai dau ngay moi ...
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.RESET_DAY > "%OUT%\kt-reset-day.txt" 2>&1
"%ADB%" -s %S% logcat -d -s KidsTV:V > "%OUT%\kt-nhat-ky.txt" 2>&1

echo.
echo ===== KET QUA =====
type "%R%"
echo.
if "%FAILS%"=="0" goto TAT_CA_DAT
echo CO %FAILS% PHEP THU HONG. Quay lai Claude va nhan: xong buoc 6
pause
exit /b 1

:TAT_CA_DAT
echo TAT CA PHEP THU DEU DAT. Quay lai Claude va nhan: xong buoc 6
pause
exit /b 0

:CHK
rem %1 = chuoi can thay trong cua so dang nhan phim, %2 = ten phep thu, %3 = so lan thu, moi lan cach 2 giay [mac dinh 1]
set "TRIES=%~3"
if "%TRIES%"=="" set "TRIES=1"
:CHK_LOOP
"%ADB%" -s %S% shell "dumpsys window | grep mCurrentFocus" > "%OUT%\focus.txt" 2>&1
findstr /c:%1 "%OUT%\focus.txt" >nul
if not errorlevel 1 goto CHK_DAT
set /a TRIES-=1
if %TRIES% LEQ 0 goto CHK_HONG
ping -n 3 127.0.0.1 >nul
goto CHK_LOOP
:CHK_DAT
echo   DAT: %~2
echo DAT: %~2 >> "%R%"
goto :eof
:CHK_HONG
echo   HONG: %~2
echo HONG: %~2 >> "%R%"
type "%OUT%\focus.txt" >> "%R%"
set /a FAILS+=1
goto :eof

:FIND
rem %1 = tep can tim, %2 = chuoi can thay, %3 = ten phep thu
findstr /c:%2 %1 >nul
if errorlevel 1 goto FIND_HONG
echo   DAT: %~3
echo DAT: %~3 >> "%R%"
goto :eof
:FIND_HONG
echo   HONG: %~3
echo HONG: %~3 >> "%R%"
set /a FAILS+=1
goto :eof

:NOTE
rem Giong FIND nhung khong tinh la hong: chi ghi LUU Y neu khong thay.
findstr /c:%2 %1 >nul
if errorlevel 1 goto NOTE_THIEU
echo   DAT: %~3
echo DAT: %~3 >> "%R%"
goto :eof
:NOTE_THIEU
echo   LUU Y: %~3
echo LUU Y: %~3 >> "%R%"
goto :eof

:CHUA_CAI
echo.
echo CHUA CAI Kids TV MMHY tren TV. Hay chay 5-CAI-LAUNCHER-BE.bat truoc.
pause
exit /b 1

:KHONG_KET_NOI
echo.
echo KHONG KET NOI DUOC TV. Kiem tra TV dang bat, cung mang, da bat ADB.
pause
exit /b 1
