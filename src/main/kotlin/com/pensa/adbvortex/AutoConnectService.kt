package com.pensa.adbvortex

import java.io.BufferedReader
import java.io.InputStreamReader

class AutoConnectService(
    private val log: (String) -> Unit,
    private val proxy: HttpProxyManager
) {
    private var running = false
    private var port = 8080

    fun start() {
        if (running) return
        running = true
        log("🔌 Iniciando AutoConnect...")

        Thread {
            try {
                val devices = getConnectedDevices()
                if (devices.isEmpty()) {
                    log("⚠️ No hay dispositivos ADB conectados")
                    return@Thread
                }

                val device = devices.first()
                log("📱 Dispositivo detectado: $device")
                log("↔️ Estableciendo redirección ADB (puerto $port)...")

                runCommand("adb -s $device reverse tcp:$port tcp:$port")
                runCommand("adb -s $device shell settings put global http_proxy 127.0.0.1:$port")

                proxy.start(port)
                log("✅ Proxy iniciado en 127.0.0.1:$port")
            } catch (e: Exception) {
                log("❌ Error en AutoConnect: ${e.message}")
            }
        }.start()
    }

    private fun getConnectedDevices(): List<String> {
        val process = ProcessBuilder("adb", "devices").start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        return reader.readLines()
            .filter { it.endsWith("device") && !it.startsWith("List") }
            .map { it.split("\t").first() }
    }

    private fun runCommand(cmd: String) {
        Runtime.getRuntime().exec(cmd).waitFor()
    }

    fun stop() {
        running = false
        runCommand("adb shell settings put global http_proxy :0")
        log("🛑 AutoConnect detenido")
    }
}
