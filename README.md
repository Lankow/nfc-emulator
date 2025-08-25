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

## HTTP Control API

The app exposes a lightweight HTTP API that allows remote control over AIDs,
logging and scenarios. Enable the server from the **Server** screen and note
the device's IP address and chosen port. Commands are sent as `POST` requests
with a JSON body to `http://<DEVICE_IP>:<PORT>/`.

### Command structure

Each request must contain a top-level `Type` field that determines the payload:

#### `Type: "Aid"`

Manage registered Application Identifiers.

```json
{
  "Type": "Aid",
  "Add": "A0000002471001",   // optional AID to add
  "Remove": "",              // optional AID to remove
  "Clear": false              // set true to unregister all AIDs
}
```

#### `Type: "Comm"`

Control the communication log and scenarios.

```json
{
  "Type": "Comm",
  "Clear": false,            // clear the log when true
  "Save": true,              // save log to file when true
  "Mute": false,             // mute/unmute communication
  "CurrentScenario": "Start" // "Start", "Stop" or "Clear"
}
```

Requests receive a simple `200 OK` response. Additional command types may be
introduced in future versions (e.g. `Scenarios`).

## Code Structure

- `MainActivity` – Compose UI and AID configuration.
- `TypeAEmulatorService` – Handles APDU commands from the NFC reader.
- `CommunicationLog` – Stores APDU exchanges for display.
