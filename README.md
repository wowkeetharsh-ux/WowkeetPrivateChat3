# Wowkeet Private Chat (com.wharsh.app)

Direct 1-to-1 seller support workspace and admin management panel with real-time messaging, order alert audio, and shop status toggle.

## How to Get APK / APK Kaise Banayein

### Tarika 1: Android Studio se (Direct)
1. Downloaded ZIP file ko extract karein.
2. Android Studio open karein aur **"Open"** par click karke folder select karein.
3. Gradle sync complete hone dein (1-2 minutes).
4. Top Menu mein jayein: **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. Notification popup aayega: **"APKs generated successfully"** -> **locate** par click karein.
6. Aapka `app-debug.apk` ready hai! Phone me transfer karke install kar lijiye.

### Tarika 2: GitHub Actions (Bina Android Studio ke Free Cloud Build)
1. Is project ko GitHub par naye repository me push karein.
2. Repo ke **"Actions"** tab mein jayein.
3. `Build Wowkeet Android APK` workflow automatically run hoga.
4. Run complete hone ke baad **Artifacts** section se direct `WowkeetPrivateChat-debug.apk` download kar sakte hain!

### Tarika 3: Command Line
```bash
chmod +x gradlew
./gradlew assembleDebug
```
Generated APK:
`app/build/outputs/apk/debug/app-debug.apk`
