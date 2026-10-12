# ClassicScan

An offline document scanner for Android. Edge detection, scan filters, on-device text recognition, searchable PDFs, and QR/barcode scanning. No account and no cloud: your scans stay on the phone unless you share them.

## Features

**Scanning**
- Live edge detection with auto-capture once the page holds steady
- Modes: Document, Book (splits a two-page spread), ID card (front and back on one A4 sheet), Whiteboard, QR/barcode
- Import from the gallery, and a Quick Settings tile to start a scan in one tap

**Editing**
- Crop with draggable corner and edge handles, a magnifier, and pinch-zoom
- Rotate, then filters: Original, Auto, Grayscale, B&W, Color, Whiteboard
- Brightness and contrast with a live preview
- Draw or sign on the page
- Automatic shadow removal and text sharpening for clean, readable scans

**Documents**
- Reorder, duplicate and delete pages, with pinch-zoom in the viewer
- Full-text search across every scan (on-device OCR)
- Export to PDF (optionally searchable), JPEG or PNG, at A4, Letter or original size
- Share, save to a file, or save to a folder

**Privacy and comfort**
- Fully offline: text recognition and barcode scanning run on the device with ML Kit
- Optional biometric app lock
- Light and dark themes, English and Bengali (বাংলা)
- Adaptive layouts for phones, tablets and foldables

## Tech stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Camera | CameraX |
| On-device ML | ML Kit Text Recognition, ML Kit Barcode Scanning |
| Storage | Room, DataStore |
| DI | Koin |
| Min / target SDK | 26 / 37 |

The image pipeline (edge detection, perspective warp, filters) and the PDF writer are written from scratch, without OpenCV or a PDF library.

## Project structure

```
app/src/main/java/com/classicscan/
├── image/      edge detection, perspective warp, filters, OCR
├── export/     PDF writer, JPEG/PNG export, sharing
├── codes/      QR and barcode scanning, history
├── data/       Room database, repositories, settings
└── ui/         Compose screens: home, camera, editor, viewer, settings
```

## Building

Requirements: Android Studio (recent stable) with its bundled JDK.

```bash
git clone https://github.com/<your-username>/classicscan.git
cd classicscan
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

### Release signing

Release builds read signing details from `keystore.properties` in the project root:

```properties
storeFile=your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

```bash
./gradlew assembleRelease
```

Without `keystore.properties`, the release APK is unsigned. `keystore.properties` and `*.jks` are git-ignored, so never commit them.

## Permissions

| Permission | Why |
|---|---|
| `CAMERA` | Scanning documents and codes |
| `USE_BIOMETRIC` | Optional app lock |

There is no `INTERNET` permission: it is stripped from the merged manifest, so the app cannot reach the network at all. The text and barcode models are bundled with the app. `ACCESS_NETWORK_STATE` is still merged in by ML Kit; it only lets the app check whether a connection exists and cannot send anything.

## License

```
Copyright 2026 ClassicScan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
