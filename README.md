[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/28658-adbvortex.svg)](https://plugins.jetbrains.com/plugin/28658-adbvortex)

# 🌀 AdbVortex

**AdbVortex** lets you share your computer’s internet connection with any Android device connected via **ADB**, directly from **IntelliJ IDEA** or **Android Studio**.

It’s a simple and elegant proxy controller that creates an HTTP bridge between your desktop and your Android device — perfect for testing backend APIs, staging servers, or internal environments that aren’t publicly accessible.

---

## ✨ Features

- 🧩 One-click **Start/Stop** HTTP proxy for ADB-connected devices  
- 🔍 Automatic device detection via `adb devices`  
- ⚙️ Real-time connection logs inside the IDE Tool Window  
- 🚀 Ideal for backend testing, QA, and debugging within restricted networks  

---

## ⚙️ Quick Info

| Item | Details |
|------|----------|
| **Default proxy** | `127.0.0.1:8080` |
| **Supported devices** | Single device (stable). Multi-device support experimental. |
| **Platform** | IntelliJ Platform Plugin — works in IntelliJ IDEA & Android Studio |
| **Icon** | `src/main/resources/icons/adbvortex_32.png` (recommended 32×32 px) |

---

## 🧠 How It Works

1. The plugin starts a lightweight **HTTP proxy** on your computer (default port `8080`).
2. It runs ADB commands to route traffic:
   ```bash
   adb reverse tcp:<PORT> tcp:<PORT>
   adb shell settings put global http_proxy 127.0.0.1:<PORT>
   ```
3. The device sends HTTP requests through your host machine, allowing apps on the device to access `localhost` services on your computer.

> ⚠️ For HTTPS traffic, use a proxy that supports `CONNECT` or install a trusted certificate.  
> AdbVortex focuses on HTTP proxying for local development and testing.

---

## 📦 Installation

### From JetBrains Marketplace  
👉 [**Install AdbVortex**](https://plugins.jetbrains.com/plugin/28658-adbvortex)

### Manual Installation  
1. Download the `.zip` from the Marketplace.  
2. In Android Studio: `Settings → Plugins → Install Plugin from Disk`.  
3. Select the downloaded file and restart the IDE.

---

## 🧩 Development (Quick Start)

1. Open this project in **IntelliJ IDEA** or **Android Studio**.  
2. Run the Gradle task **Run Plugin** (`runIde`) to launch a sandbox IDE instance.  
3. In the sandbox IDE, open the **AdbVortex** Tool Window:
   - Pick a port (default `8080`).
   - Click **Start** (ensure the device appears in `adb devices`).
   - On the device, open an HTTP URL or test your app — traffic flows through your PC.
4. Click **Stop** to clear proxy settings and shut down the local proxy.

---

## 🖥️ UI / UX Notes

- Displays real-time logs and device connection status.  
- The top toolbar can list connected devices — edit `AdbVortexToolWindowFactory` to customize.  
- If logs reach the top and overlap controls, enable **auto-scroll** or add padding/margins in the layout.

---

## 🧯 Troubleshooting

### Device not detected
- Verify that the `adb` binary used by IntelliJ matches your system’s `adb`.  
- Restart ADB:
  ```bash
  adb kill-server && adb start-server
  ```

### Device shows as `offline`
- Reconnect the USB cable or confirm the debug authorization prompt on the device.

### Timeout or no network on device
- Check firewall/antivirus rules.  
- Confirm device proxy with:
  ```bash
  adb shell settings get global http_proxy
  ```
- Some HTTPS apps with certificate pinning won’t work through a plain HTTP proxy.

---

## 📂 Project Structure
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

## 📜 License

**MIT License**  
© 2025 **Carlos Javier Torres Pensa** — [pensa.com.ar](https://pensa.com.ar)

Permission is hereby granted, free of charge, to any person obtaining a copy  
of this software and associated documentation files (the "Software"), to deal  
in the Software without restriction, including without limitation the rights  
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell  
copies of the Software, and to permit persons to whom the Software is  
furnished to do so, subject to the following conditions:

> The Software is provided “as is”, without warranty of any kind, express or implied,  
> including but not limited to the warranties of merchantability, fitness for a  
> particular purpose, and noninfringement.

---

## ☕ Support & Donations

If you find **AdbVortex** useful and want to support its development, you can:

[![Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/carolusx74)  
[**Buy me a coffee ☕**](https://www.buymeacoffee.com/carolusx74)

Your support helps keep the vortex spinning 🌪️ — thank you!

---

🌀 *AdbVortex — by Carlos Javier Torres Pensa*  
*Bridging Android and Desktop worlds, one proxy at a time.*
