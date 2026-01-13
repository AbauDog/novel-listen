# 產生 APK 及佈署指南 (Native Android)
幫我完成產生已簽署的 APK
本文件說明如何將 Native Kotlin Android 應用程式安裝到手機或產生 APK 檔案。

## 1. 快速安裝 (開發用)
這是最常用的方式，包含除錯(Debug)簽名，可以直接安裝到連接的手機。

### 指令安裝 (推薦)
在終端機 (Terminal) 執行：
```powershell
.\gradlew.bat installDebug
```
* **效果**：這會編譯並將 App **永久安裝** 到您的手機上（直到您手動移除）。
* **執行**：安裝後，您可以隨時在手機的應用程式列表中找到並開啟它，不需要每次都連接電腦。

### 檔案位置
如果您想將 APK 傳檔案給別人安裝：
* **路徑**：`app\build\outputs\apk\debug\app-debug.apk`
* **注意**：這是 Debug 版本，雖然可以安裝，但效能可能略低於正式版，且安裝時手機可能會提示「不安全的來源」（因為使用的是測試簽章）。

---

## 2. 一鍵產生已簽署正式版 (推薦)
這是最簡單的方式，可以自動產生已簽署的正式版 APK。

### 使用方式
1. 在專案根目錄中，找到並雙擊執行 `generate_signed_apk.bat`。
2. 等待腳本執行完畢。
3. 腳本完成後會自動開啟資料夾，您會看到 `app-release.apk`。

### 檔案位置
* **路徑**：`app\build\outputs\apk\release\app-release.apk`
* **注意**：
    * 此 APK 使用預設的 KeyStore (`release.keystore`) 簽章，密碼為 `android`。
    * **適合**：自行安裝、發布給測試人員。
    * **不適合**：上架 Google Play 商店 (需要使用您自己保管的安全金鑰)。

---

## 3. 手動產生正式版 APK (進階)
正式版經過優化 (ProGuard/R8)，檔案較小且執行較快，適合發布或長期使用。

### 產生未簽署的 APK (Unsigned)
```powershell
.\gradlew.bat assembleRelease
```
* **路徑**：`app\build\outputs\apk\release\app-release-unsigned.apk`
**實際路徑**：  c:\Users\tatung\Downloads\VSC\Novel_R\app\build\outputs\apk\debug\app-debug.apk
* **注意**：Android 不允許安裝未簽署的 APK。您必須先簽名才能安裝。

### 產生已簽署的 APK (Signed)
若要在 gradle 自動簽名，需在 `build.gradle` 設定 `signingConfigs`。
**建議方式 (使用 Android Studio)**：
1. 開啟 Android Studio。
2. 點選選單 **Build** > **Generate Signed Bundle / APK...**。
3. 選擇 **APK**。
4. 建立一個新的 Key store (金鑰庫) 或選擇現有的。
5. 選擇 `release` 版本並勾選 V1/V2 Signature。
6. 完成後，IDE 會提示您 APK 的位置。

---

## 4. 常見問題

### Q: `.\gradlew.bat installDebug` 之後還要執行 `adb shell am start ...` 嗎？
**A: 不用。** 
`installDebug` 只是負責「安裝」。
`adb shell am start ...` 是負責「自動開啟」App。
如果您只是想安裝，執行完 `installDebug` 後，您就可以拔掉傳輸線，在手機桌面上點擊 App 圖示開始使用了。

### Q: 為什麼手機顯示「App 未安裝」？
* 檢查是否已安裝過「不同簽名」的版本（例如之前用其他電腦編譯的）。請先解除安裝舊版再試。
* 檢查手機儲存空間是否足夠。

##### 自己常用備註
adb devices
C:\Users\tatung\AppData\Local\Android\Sdk\platform-tools\adb.exe devices

.\gradlew.bat clean
編譯安裝
.\gradlew.bat installDebug
執行餘手機
C:\Users\tatung\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -n com.example.novel_r/.MainActivity

APK 檔案位置：如果您需要傳送安裝檔給其他人，Debug 版的 APK 檔案位於： app\build\outputs\apk\debug\app-debug.apk
