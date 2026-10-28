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

echo.
echo Se tra TV ve man hinh chinh goc cua Google TV.
choice /c TGH /n /m "Bam T de TAM dung che do tre em [giu ung dung], G de GO HAN ung dung, H de huy: "
if errorlevel 3 goto HUY
if errorlevel 2 goto GO_HAN
goto TAM_DUNG

:TAM_DUNG
echo [1/3] Tat dich vu bao ve va cho phep dung moi ung dung 24 gio ...
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.SHUTDOWN > "%OUT%\thoat-1.txt" 2>&1
type "%OUT%\thoat-1.txt"
echo [2/3] Tra man hinh chinh goc cua TV ...
"%ADB%" -s %S% shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity > "%OUT%\thoat-2.txt" 2>&1
type "%OUT%\thoat-2.txt"
echo [3/3] Ve man hinh chinh ...
"%ADB%" -s %S% shell input keyevent KEYCODE_HOME
echo.
echo XONG. TV da ve man hinh chinh goc. Muon bat lai che do tre em: chay 5-CAI-LAUNCHER-BE.bat
pause
exit /b 0

:GO_HAN
echo [1/3] Tat dich vu bao ve ...
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.SHUTDOWN > "%OUT%\thoat-1.txt" 2>&1
type "%OUT%\thoat-1.txt"
echo [2/3] Tra man hinh chinh goc cua TV ...
"%ADB%" -s %S% shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity > "%OUT%\thoat-2.txt" 2>&1
type "%OUT%\thoat-2.txt"
echo [3/3] Go ung dung Kids TV MMHY ...
"%ADB%" -s %S% shell pm uninstall %PKG% > "%OUT%\thoat-3.txt" 2>&1
type "%OUT%\thoat-3.txt"
"%ADB%" -s %S% shell input keyevent KEYCODE_HOME
echo.
echo XONG. Da go ung dung. TV ve nhu truoc khi cai.
pause
exit /b 0

:HUY
echo Da huy, khong lam gi.
pause
exit /b 0

:KHONG_KET_NOI
echo.
echo KHONG KET NOI DUOC TV. Kiem tra TV dang bat, cung mang, da bat ADB.
pause
exit /b 1
