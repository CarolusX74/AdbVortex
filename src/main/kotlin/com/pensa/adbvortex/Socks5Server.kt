package com.pensa.adbvortex

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class Socks5Server(private val console: ConsoleView?, private val port: Int = 1080) {
    @Volatile private var running = false
    private val exec = Executors.newCachedThreadPool()

    fun start() {
        if (running) return
        running = true
        exec.submit {
            val server = ServerSocket(port)
            console?.print("🧦 SOCKS5 escuchando en 127.0.0.1:$port\n", ConsoleViewContentType.SYSTEM_OUTPUT)
            while (running) exec.submit { handle(server.accept()) }
        }
    }

    fun stop() { running = false }

    private fun handle(client: Socket) {
        val cin = client.getInputStream()
        val cout = client.getOutputStream()
        try {
            // Greeting
            val ver = cin.read()
            val nMethods = cin.read()
            cin.skip(nMethods.toLong()) // sin auth
            cout.write(byteArrayOf(0x05, 0x00)); cout.flush()

            // Request
            if (cin.read() != 0x05) return
            val cmd = cin.read() // 0x01 CONNECT
            cin.read() // RSV
            val atyp = cin.read()

            val host = when (atyp) {
                0x01 -> (1..4).joinToString(".") { cin.read().toString() }
                0x03 -> {
                    val ln = cin.read()
                    val bytes = ByteArray(ln); cin.read(bytes); String(bytes)
                }
                0x04 -> { // IPv6 (simple)
                    val b = ByteArray(16); cin.read(b); b.joinToString(":") { "%02x".format(it) }
                }
                else -> return
            }
            val port = (cin.read() shl 8) or cin.read()

            if (cmd != 0x01) return // solo CONNECT

            val remote = Socket()
            remote.connect(InetSocketAddress(host, port), 5000)
            cout.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0,0,0,0, 0,0)); cout.flush()

            exec.submit { pipe(cin, remote.getOutputStream()) }
            exec.submit { pipe(remote.getInputStream(), cout) }

            console?.print("🧦 CONNECT $host:$port\n", ConsoleViewContentType.NORMAL_OUTPUT)
        } catch (e: Exception) {
            console?.print("⚠️ SOCKS5 error: ${e.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    private fun pipe(inp: InputStream, out: OutputStream) {
        val buf = ByteArray(64 * 1024)
        var n: Int
        try {
            while (inp.read(buf).also { n = it } > 0) { out.write(buf, 0, n); out.flush() }
        } catch (_: Exception) {}
    }
}
