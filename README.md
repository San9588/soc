# Option Chain Android App (soc)

This repository contains the base Android application for the **Option Chain** project.

## Features Developed
- **Material 3 (Monet / Material You) UI**: Dynamic theming on Android 12+, with default colors for older versions.
- **Jetpack Compose UI**: Modern, declarative UI framework.
- **Compose Navigation**: Easy navigation between `Live` and `History` pages.
- **Websocket Client (Live Data)**: Uses `OkHttp` to connect to a local network websocket server. 
- **HTTP POST API (History Data)**: Uses `OkHttp` to POST JSON data and fetch history. 
- **Coroutines & Flows**: Efficient asynchronous state management without blocking the main UI thread.

## Requirements
- **Android Studio Giraffe/Hedgehog** or newer.
- Minimum SDK: **API 29 (Android 10)**.
- Target SDK: **API 34**.

## How to Test on Local Network
1. Open this project in **Android Studio**. It will automatically sync Gradle dependencies.
2. In `app/src/main/java/com/soc/optionchain/network/WebsocketClient.kt`, modify the `WS_URL` to match your local network's WebSocket IP (e.g., `ws://192.168.x.x:8080/live`).
3. In `app/src/main/java/com/soc/optionchain/network/HistoryApiClient.kt`, modify the `API_URL` to match your local network's HTTP API (e.g., `http://192.168.x.x:8080/history`).
4. Since `android:usesCleartextTraffic="true"` is enabled in the `AndroidManifest.xml`, testing over `ws://` and `http://` locally will work without SSL errors.

When deploying to production in the future, change the URLs to `wss://` and `https://` and set `usesCleartextTraffic` back to `false` if desired.
