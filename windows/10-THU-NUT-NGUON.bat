@echo off
setlocal
set "HERE=%~dp0"
set "ADB=%~dp0platform-tools\adb.exe"
set "S=192.168.31.83:5555"
set "OUT=%~dp0out"
set "PKG=com.minhminhhungyen.kidstv"
set "R=%OUT%\thu-nut-nguon.txt"
set "FAILS=0"
set "TRIES=10"
if not exist "%OUT%" mkdir "%OUT%"
echo ===== THU NUT NGUON %date% %time% ===== > "%R%"

echo [1] Ket noi TV %S% ...
"%ADB%" connect %S%
"%ADB%" -s %S% get-state > "%OUT%\state.txt" 2>&1
findstr /x /c:"device" "%OUT%\state.txt" >nul
if errorlevel 1 goto KHONG_KET_NOI
"%ADB%" -s %S% shell pm list packages %PKG% > "%OUT%\kt-goi.txt" 2>&1
findstr /c:"%PKG%" "%OUT%\kt-goi.txt" >nul
if errorlevel 1 goto CHUA_CAI

"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.STATUS > "%OUT%\np-ban-cai.txt" 2>&1
findstr /c:"gateCount=" "%OUT%\np-ban-cai.txt" >nul
if errorlevel 1 goto CHUA_CAP_NHAT

echo [2] Cho be xem 3 phut [coi nhu hom nay da dung phien mien phi] ...
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.PARENT_OFF > "%OUT%\np-parent-off.txt" 2>&1
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.GRANT --ei minutes 3 > "%OUT%\np-grant.txt" 2>&1
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.STATUS > "%OUT%\np-truoc.txt" 2>&1
type "%OUT%\np-truoc.txt"
type "%OUT%\np-truoc.txt" >> "%R%"
call :FIND "%OUT%\np-truoc.txt" "state=RUNNING" "TV dang trong phien xem"
echo.
echo =====================================================
echo  BAY GIO LAM TREN TV:
echo    1. Cam REMOTE cua TV, bam nut NGUON de TAT TV [man hinh den].
echo    2. Doi khoang 10 giay.
echo    3. Bam nut NGUON de BAT TV lai.
echo    4. Khi man hinh TV da sang, quay lai day va bam phim bat ky.
echo =====================================================
pause

echo [3] Ket noi lai TV sau khi tat/bat [co the mat vai giay] ...
:RECONNECT
"%ADB%" connect %S% >nul 2>&1
"%ADB%" -s %S% get-state > "%OUT%\state.txt" 2>&1
findstr /x /c:"device" "%OUT%\state.txt" >nul
if not errorlevel 1 goto DA_KET_NOI
set /a TRIES-=1
if %TRIES% LEQ 0 goto MAT_KET_NOI
ping -n 4 127.0.0.1 >nul
goto RECONNECT

:DA_KET_NOI
echo [4] Doc ket qua tren TV ...
ping -n 6 127.0.0.1 >nul
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.STATUS > "%OUT%\np-sau.txt" 2>&1
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.EVENTS > "%OUT%\np-su-kien.txt" 2>&1
"%ADB%" -s %S% exec-out screencap -p > "%OUT%\tv-6-sau-khi-bat.png"
"%ADB%" -s %S% shell "dumpsys window | grep mCurrentFocus" > "%OUT%\np-focus.txt" 2>&1
type "%OUT%\np-sau.txt"
type "%OUT%\np-sau.txt" >> "%R%"
echo ----- Cac lan tat/bat gan nhat ----->> "%R%"
type "%OUT%\np-su-kien.txt" >> "%R%"
call :FIND "%OUT%\np-su-kien.txt" "man hinh tat" "TV bao da TAT man hinh khi bam nut Nguon"
call :FIND "%OUT%\np-su-kien.txt" "gate (" "Ung dung da khoa cho be giai toan"
call :FIND "%OUT%\np-sau.txt" "gate=true" "Hien tai TV dang cho be giai toan"
call :NOTE "%OUT%\np-su-kien.txt" "BOOT_COMPLETED" "Nut Nguon lam TV KHOI DONG LAI han [thay BOOT_COMPLETED]"
call :NOTE "%OUT%\np-su-kien.txt" "man hinh bat state" "TV bao da BAT man hinh khi bat lai"
findstr /c:"kidstv.MainActivity" "%OUT%\np-focus.txt" >nul
if errorlevel 1 goto TOAN_KHONG_HIEN
echo   DAT: Man hinh Kids TV [toan] dang hien tren TV
echo DAT: Man hinh Kids TV dang hien >> "%R%"
goto KET_THUC
:TOAN_KHONG_HIEN
echo   HONG: Man hinh Kids TV khong hien tren TV
echo HONG: Man hinh Kids TV khong hien >> "%R%"
type "%OUT%\np-focus.txt" >> "%R%"
set /a FAILS+=1

:KET_THUC
echo.
echo [5] Lay them nhat ky he thong cho Claude ...
call "%HERE%7-XEM-NHAT-KY.bat"
echo.
echo ===== KET QUA =====
type "%R%"
echo.
if "%FAILS%"=="0" goto TAT_CA_DAT
echo CO %FAILS% PHEP THU HONG. Quay lai Claude va nhan: xong buoc 10
pause
exit /b 1

:TAT_CA_DAT
echo TAT CA DEU DAT. TV dang hien toan: be giai xong la xem duoc.
echo Quay lai Claude va nhan: xong buoc 10
pause
exit /b 0

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
rem Giong FIND nhung khong tinh la hong: chi ghi LUU Y.
findstr /c:%2 %1 >nul
if errorlevel 1 goto NOTE_THIEU
echo   LUU Y: %~3
echo LUU Y: %~3 >> "%R%"
goto :eof
:NOTE_THIEU
echo   [khong thay] %~3
echo [khong thay] %~3 >> "%R%"
goto :eof

:CHUA_CAI
echo.
echo CHUA CAI Kids TV MMHY tren TV. Hay chay 5-CAI-LAUNCHER-BE.bat truoc.
pause
exit /b 1

:CHUA_CAP_NHAT
echo.
echo UNG DUNG TREN TV CHUA LA BAN MOI [chua co che do toan khi bat TV]. Hay chay 5-CAI-LAUNCHER-BE.bat truoc.
pause
exit /b 1

:MAT_KET_NOI
echo.
echo KHONG KET NOI LAI DUOC TV sau khi tat/bat. Co the TV da khoi dong lai han va tat Go loi mang.
echo Hay bat lai Go loi mang [Network debugging] tren TV, roi chay 7-XEM-NHAT-KY.bat
echo Dieu nay cho Claude biet nut Nguon cua TV khoi dong lai han chu khong chi tat man hinh.
pause
exit /b 1

:KHONG_KET_NOI
echo.
echo KHONG KET NOI DUOC TV. Kiem tra TV dang bat, cung mang, da bat ADB.
pause
exit /b 1
