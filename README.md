# Ikazev AI Answer Bubble

Android native starter project: floating overlay bubble, user-approved screen capture, on-device OCR (ML Kit bundled model), and optional Gemini API answer assistance. GitHub Actions builds a debug APK on every push.

## Important
- No AI service can guarantee 100% correct answers.
- OCR and screenshot capture only run after the user explicitly starts capture and approves Android's screen-capture prompt.
- Android may prevent capture of protected/secure screens. Respect app privacy and exam/platform rules.
- Gemini API use may have free-tier limits and terms that change. A key is entered by the user in-app and stored locally; this is convenient for personal testing, not safe for a publicly distributed APK. Never commit API keys.
- The app has no contacts, SMS, microphone, location, or Accessibility permission.
- Debug APK is for testing. It is not a Play Store release build.

## Build in GitHub
1. Create a **public** GitHub repository named `Ikazev-𝙰𝚊𝙰` (GitHub repository names may reject some Unicode characters; if it does, use `Ikazev-AaA`).
2. Upload/push this project's files to the repository root.
3. Open **Actions** and allow workflows if GitHub asks.
4. Every push to `main` triggers `.github/workflows/android.yml`.
5. Download `Ikazev-Answer-Bubble-debug-apk` from the completed workflow run's **Artifacts** section.

## Push from Termux
See `TERMUX.md`. GitHub authentication usually uses a browser login/token; do not paste a token into shared scripts or commit it.

## Optional Gemini setup
Get an API key through Google's official AI Studio account, if eligible. Open the app, enter the key in Settings, and save it. The key remains on this device in app preferences, but an APK is not a secure place to protect a key from a determined device owner or reverse engineer. The app calls the Gemini REST API directly; usage may be rate-limited and availability varies by account/region.

## Permissions
- `INTERNET`: optional online AI request.
- `SYSTEM_ALERT_WINDOW`: floating bubble.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PROJECTION`: capture session service.
- Notifications permission is requested on Android 13+ to show the capture foreground-service notification.
- MediaProjection consent is requested each time capture starts.
