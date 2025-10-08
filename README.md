[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/28658-adbvortex.svg)](https://plugins.jetbrains.com/plugin/28658-adbvortex)

# AdbVortex

**AdbVortex** lets you share your computer’s internet connection with any Android device connected via **ADB**, directly from **IntelliJ IDEA** or **Android Studio**.

It provides a simple and elegant proxy controller that creates an HTTP bridge between your desktop and the device — perfect for testing backend APIs, staging servers, or internal environments that aren’t publicly accessible.

---

## ✨ Features
- 🧩 One-click Start/Stop HTTP proxy for ADB-connected devices  
- 🔍 Automatic device detection via `adb devices`  
- ⚙️ Real-time logs inside the IDE Tool Window  
- 🚀 Ideal for backend testing, QA, and corporate network debugging

---

## Quick info
- **Default proxy:** `127.0.0.1:8080`
- **Supported devices:** Single Android device (stable by default). Multi-device support is experimental.
- **Platform:** IntelliJ Platform plugin (works in IntelliJ IDEA & Android Studio).
- **Icon:** `src/main/resources/icons/adbvortex_32.png` (recommended 32×32 px).

---

## How it works

1. The plugin starts a lightweight **HTTP proxy** on your computer (default port `8080`).
2. It runs ADB commands to forward traffic:
   ```bash
   adb reverse tcp:<PORT> tcp:<PORT>
   adb shell settings put global http_proxy 127.0.0.1:<PORT>
   ```
3. The device routes its HTTP traffic through the host machine (your computer), allowing the device to reach services running on `localhost` of your computer.

> Note: For HTTPS you need a proxy that supports `CONNECT` and (optionally) user-installed certificates — AdbVortex focuses on HTTP for development/testing.

---

## Installation
You can install it directly from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/28658-adbvortex).

Or manually:
1. Download the `.zip` file from the Marketplace.
2. In Android Studio, go to `Settings → Plugins → Install Plugin from Disk`.
3. Select the downloaded file and restart the IDE.

---
## Quick start (development)

1. Open this project in **IntelliJ IDEA** or **Android Studio**.
2. Run the Gradle task **Run Plugin** (Gradle: `runIde`) to launch a sandbox IDE instance.
3. In the sandbox IDE open the **AdbVortex** Tool Window:
    - Pick a port (default: `8080`).
    - Click **Start** (device must be visible via `adb devices`).
    - On the device open an HTTP URL or test your app — traffic will be proxied through your machine.
4. Click **Stop** to clear proxy settings on the device and stop the local proxy.

---

## UI / UX notes

- The tool window shows logs and device connection status.
- If you want to show connected devices in the top panel, the plugin's UI includes a short list — edit `AdbVortexToolWindowFactory` to update the view.
- If logs reach the top and overlap UI controls, set the log panel to auto-scroll or add padding/margins above the button bar in the UI layout.

---

## Troubleshooting

- Device not shown in the IDE but `adb devices` lists it:
    - Ensure `adb` in PATH used by IntelliJ matches the one you run on the terminal.
    - Consider restarting ADB: `adb kill-server && adb start-server`.
- `offline` device in `adb devices`:
    - Reconnect the USB cable or trust debugging prompt on the device.
- Timeouts from proxy:
    - Check firewall/antivirus blocking.
    - Make sure the device uses the host proxy (verify `settings get global http_proxy` on device).
    - For apps using certificate pinning or HTTPS, HTTP proxying may not work — use a proper mitm proxy.

---

## Project structure
```
AdbVortex/
 ├── build.gradle.kts
 ├── settings.gradle.kts
 ├── gradle.properties
 ├── LICENSE
 ├── src/main/resources/META-INF/plugin.xml
 ├── src/main/resources/icons/adbvortex.svg
 └── src/main/kotlin/com/pensa/adbvortex/...
```

---

## License (MIT)

MIT License

Copyright (c) YEAR Your Name / Pensa

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---


