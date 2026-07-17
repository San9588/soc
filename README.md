# Option Chain Android App (soc)

This repository contains the base Android application for the **Option Chain** project.

## Features Developed
- **Material 3 (Monet / Material You) UI**: Dynamic theming on Android 12+, with default colors for older versions.
- **Jetpack Compose UI**: Modern, declarative UI framework.
- **Compose Navigation**: Easy navigation between `Live`, `History`, and `Settings` pages.
- **Websocket Client (Live Data)**: Uses `OkHttp` to connect to a local network websocket server. 
- **HTTP POST API (History Data)**: Uses `OkHttp` to POST JSON data and fetch history. 
- **Coroutines & Flows**: Efficient asynchronous state management without blocking the main UI thread.

## Requirements
- Minimum SDK: **API 29 (Android 10)**.
- Target SDK: **API 34**.
- Java Development Kit: **JDK 17** (required for AGP 8.2+).

## How to Test on Local Network
1. Open this project in **Android Studio**. It will automatically sync Gradle dependencies.
2. Go to the `Settings` page inside the app to see the default network configurations. 
3. Code-wise, URLs can be found and modified in `WebsocketClient.kt` and `HistoryApiClient.kt`.
4. Since `android:usesCleartextTraffic="true"` is enabled in the `AndroidManifest.xml`, testing over `ws://` and `http://` locally will work without SSL errors.

---

## How to Build via CLI on Ubuntu ARM64 Terminal

If you are using a terminal-only Ubuntu ARM64 environment (e.g. an ARM VM, Raspberry Pi, or Mac M-series Linux VM), follow these steps to build the APK via the Command Line:

### 1. Install Java (JDK 17) and Utilities
```bash
sudo apt update
sudo apt install openjdk-17-jdk wget unzip -y
```

### 2. Download Android Command Line Tools
Download the latest Android SDK command-line tools for Linux:
```bash
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
# Download the command line tools zip (check Android Studio downloads page for the absolute latest if this expires)
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip cmdline-tools.zip
mv cmdline-tools latest
rm cmdline-tools.zip
```

### 3. Set Environment Variables
Add these to your `~/.bashrc` (or `~/.zshrc` / `~/.profile`), then run `source ~/.bashrc`:
```bash
# Path to your ARM64 JDK 17
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

### 4. Accept SDK Licenses and Install Required Packages
```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 5. Build the Project
Go back to your cloned project directory:
```bash
cd /path/to/soc
# Give execution permission to the gradlew script
chmod +x gradlew

# Build the Debug APK
./gradlew assembleDebug
```

### 6. Find your APK
Once the build is completely successful, your generated APK will be available at:
`app/build/outputs/apk/debug/app-debug.apk`

You can use `scp` or `adb` to transfer this APK to your Android device to test.
