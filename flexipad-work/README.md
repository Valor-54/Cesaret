# FlexiPad — Shizuku Bridge

Android virtual gamepad / touch-overlay project with:

- Jetpack Compose UI
- editable gamepad profiles stored with Room
- floating overlay service
- virtual buttons, D-pad, joysticks and triggers
- drag/edit mode and live tester
- Shizuku UserService bridge
- system-level touch injection through `InputManager.injectInputEvent`
- multi-touch pointer tracking
- UDP input driver
- Bluetooth HID driver
- accessibility fallback
- haptic feedback
- profile JSON import/export support
- orientation and opacity controls
- GeForce NOW launcher
- debug APK included in `.build-outputs/app-debug.apk`

## Main Shizuku flow

`HomeViewModel -> ShizukuInputBridge -> InputUserService -> InputManager`

`InputUserService` keeps active pointer states and constructs `MotionEvent`
instances for DOWN / MOVE / POINTER_DOWN / POINTER_UP / UP.

## Build

Open this directory in Android Studio and let Gradle sync.

Requirements:
- Android Studio
- JDK 17
- Android SDK matching `compileSdk = 36.1`

The project contains a previously built debug APK at:

`.build-outputs/app-debug.apk`

## Device setup

1. Install and start Shizuku on the Android device.
2. Start Shizuku using Wireless Debugging or ADB.
3. Open FlexiPad.
4. Grant the requested Shizuku permission.
5. Grant overlay permission.
6. Enable the accessibility service if the app asks for it.
7. Start the overlay and select/edit a profile.

The app package is `com.aistudio.flexipad.wtov`.

## Source layout

- `app/src/main/java/com/example/shizuku/` — Shizuku bridge and UserService
- `app/src/main/java/com/example/service/` — overlay/accessibility services
- `app/src/main/java/com/example/input/` — input drivers
- `app/src/main/java/com/example/model/` — profiles and controls
- `app/src/main/java/com/example/data/` — Room/database/repository
- `app/src/main/java/com/example/ui/` — Compose screens and controls
