@echo off
echo ========================================================
echo       Generating Signed Release APK...
echo       產生已簽署的正式版 APK...
echo ========================================================

call .\gradlew.bat assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ********************************************************
    echo [ERROR] Build Failed! 請檢查錯誤訊息。
    echo ********************************************************
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================================
echo       Build SUCCESS! APK Generated.
echo       建置成功！已產生 APK。
echo ========================================================
echo.
echo Opening Output Folder...
echo 開啟輸出資料夾...

start "" "app\build\outputs\apk\release"

pause
