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
"%ADB%" -s %S% logcat -d -s KidsTV:V > "%OUT%\nhat-ky-kidstv.txt" 2>&1
"%ADB%" -s %S% logcat -d -b crash > "%OUT%\nhat-ky-crash.txt" 2>&1
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.STATUS > "%OUT%\trang-thai.txt" 2>&1
"%ADB%" -s %S% shell dumpsys window | findstr /c:"mCurrentFocus" > "%OUT%\cua-so-hien-tai.txt" 2>&1
"%ADB%" -s %S% shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > "%OUT%\home-hien-tai.txt" 2>&1
echo.
type "%OUT%\trang-thai.txt"
echo.
echo Da luu: out\nhat-ky-kidstv.txt, out\nhat-ky-crash.txt, out\trang-thai.txt
echo Quay lai Claude va nhan: xong nhat ky
pause
exit /b 0

:KHONG_KET_NOI
echo.
echo KHONG KET NOI DUOC TV. Kiem tra TV dang bat, cung mang, da bat ADB.
pause
exit /b 1
