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

echo Dat lai mat khau phu huynh ve mac dinh [ngay hom nay dang ddMMyyyy] va xoa khoa nhap sai ...
"%ADB%" -s %S% shell am broadcast -n %PKG%/.AdminReceiver -a %PKG%.RESET_PWD > "%OUT%\dat-lai-mat-khau.txt" 2>&1
type "%OUT%\dat-lai-mat-khau.txt"
echo.
echo XONG. Mat khau phu huynh gio la ngay hom nay, vi du 28102026.
pause
exit /b 0

:KHONG_KET_NOI
echo.
echo KHONG KET NOI DUOC TV. Kiem tra TV dang bat, cung mang, da bat ADB.
pause
exit /b 1
