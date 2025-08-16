# NFC Type A Emulator – Android App

An Android application for emulating **NFC Type A** cards (ISO 14443-3A / ISO 14443-4A) using compatible devices.
The app allows developers, researchers, and hobbyists to test NFC readers without requiring the original physical card.

## Prerequisites

- Android device with NFC capability (Android 5.0 or higher).
- [Android Studio](https://developer.android.com/studio) or the Android SDK with Java 11+.

## Building

```bash
./gradlew assembleDebug
```

The generated APK will be located in `app/build/outputs/apk/debug/`.

You can also open the project in Android Studio and run it directly on a connected device.

## Usage

1. Install the app on an NFC-capable Android device.
2. Enter up to two Application Identifiers (AIDs) in the provided fields.
3. Tap **Save AIDs** and hold the device near an NFC reader.
4. The communication log at the bottom shows APDU requests (red) and responses (green).

## Code Structure

- `MainActivity` – Compose UI and AID configuration.
- `TypeAEmulatorService` – Handles APDU commands from the NFC reader.
- `CommunicationLog` – Stores APDU exchanges for display.
