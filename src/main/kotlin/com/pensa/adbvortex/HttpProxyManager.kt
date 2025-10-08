package com.pensa.adbvortex

import java.io.*
import java.net.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.thread

class HttpProxyManager(private val log: (String) -> Unit) {

    @Volatile private var server: ServerSocket? = null
    private val acceptExec = Executors.newSingleThreadExecutor { r ->
        Thread(r, "adbvortex-accept").apply { isDaemon = true }
    }
    private val ioExec = Executors.newCachedThreadPool { r ->
        Thread(r, "adbvortex-io").apply { isDaemon = true }
    }

    fun start(port: Int) {
        if (server != null) return

        val srv = ServerSocket()
        srv.reuseAddress = true
        srv.bind(InetSocketAddress("127.0.0.1", port), 256)
        server = srv
        log("Proxy started on http://127.0.0.1:$port")

        acceptExec.execute {
            try {
                while (!srv.isClosed) {
                    val client = try { srv.accept() } catch (_: SocketException) { break }
                    client.tcpNoDelay = true
                    client.keepAlive = true
                    ioExec.execute { handleClient(client) }
                }
            } finally {
                safeClose(srv)
            }
        }
    }

    fun stop() {
        try {
            server?.close()
        } catch (_: Exception) { }
        server = null
        log("Proxy stopped.")
    }

    private fun handleClient(client: Socket) {
        var remote: Socket? = null
        try {
            // Timeout cortito sólo para leer request inicial
            client.soTimeout = 8000
            val cin = BufferedInputStream(client.getInputStream())
            val cout = BufferedOutputStream(client.getOutputStream())

            val requestLine = readLine(cin) ?: return
            val headers = mutableListOf<String>()
            while (true) {
                val h = readLine(cin) ?: ""
                if (h.isEmpty()) break
                headers += h
            }

            if (requestLine.startsWith("CONNECT ")) {
                val hostPort = requestLine.substring(8).substringBefore(" ")
                val hp = hostPort.split(":")
                val host = hp[0]
                val port = hp.getOrNull(1)?.toIntOrNull() ?: 443
                log("CONNECT $host:$port")

                remote = Socket()
                remote.tcpNoDelay = true
                remote.keepAlive = true
                // Conexión a destino con timeout de conexión (no de lectura)
                remote.connect(InetSocketAddress(host, port), 12000)

                // Ya no queremos timeouts de lectura en el túnel
                client.soTimeout = 0
                remote.soTimeout = 0

                // Establecer túnel
                cout.write("HTTP/1.1 200 Connection established\r\n\r\n".toByteArray())
                cout.flush()

                val rin = BufferedInputStream(remote.getInputStream())
                val rout = BufferedOutputStream(remote.getOutputStream())

                // Bombear en ambos sentidos
                val f1: Future<*> = ioExec.submit { pump(cin, rout, remote) }
                val f2: Future<*> = ioExec.submit { pump(rin, cout, client) }

                // Esperar a que ambos terminen (o fallen)
                runCatching { f1.get() }
                runCatching { f2.get() }
            } else {
                // Proxy HTTP "normal" para peticiones con URL absoluta
                handleHttpForward(requestLine, headers, cin, cout)
            }
        } catch (ste: SocketTimeoutException) {
            log("Error CONNECT: ${ste.javaClass.simpleName}: ${ste.message}")
        } catch (io: IOException) {
            log("Error CONNECT: ${io.javaClass.simpleName}: ${io.message}")
        } finally {
            safeClose(remote)
            safeClose(client)
        }
    }

    private fun handleHttpForward(
        requestLine: String,
        headers: List<String>,
        cin: InputStream,
        cout: OutputStream
    ) {
        try {
            // Request line: METHOD http://host[:port]/path HTTP/1.1
            val parts = requestLine.split(" ")
            if (parts.size < 3) return
            val method = parts[0]
            val url = URL(parts[1])

            val port = if (url.port == -1) if (url.protocol.equals("https", true)) 443 else 80 else url.port
            val remote = Socket()
            remote.tcpNoDelay = true
            remote.keepAlive = true
            remote.connect(InetSocketAddress(url.host, port), 12000)

            val rin = BufferedInputStream(remote.getInputStream())
            val rout = BufferedOutputStream(remote.getOutputStream())

            // Reescribir request line a ruta relativa para el servidor destino
            val newLine = "$method ${if (url.file.isNullOrEmpty()) "/" else url.file} HTTP/1.1\r\n"
            rout.write(newLine.toByteArray())

            // Filtrar cabeceras hop-by-hop
            headers.filterNot { h ->
                val k = h.substringBefore(':').trim().lowercase()
                k in setOf("proxy-connection", "connection", "keep-alive", "te", "trailers", "transfer-encoding", "upgrade")
            }.forEach { h ->
                rout.write(h.toByteArray()); rout.write("\r\n".toByteArray())
            }
            rout.write("\r\n".toByteArray())
            rout.flush()

            // Transferir cuerpo (si lo hubiera) de forma best-effort
            ioExec.submit { pump(cin, rout, remote) }
            pump(rin, cout, null) // respuesta → cliente

            safeClose(remote)
        } catch (e: Exception) {
            log("HTTP forward error: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun pump(input: InputStream, output: OutputStream, peer: Socket?) {
        val buf = ByteArray(64 * 1024)
        try {
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                output.write(buf, 0, n)
                output.flush()
            }
        } catch (_: SocketTimeoutException) {
            // en túnel ignoramos timeouts intermitentes si los hubiera
        } catch (_: IOException) {
        } finally {
            runCatching { output.flush() }
            // medio cierre para avisar fin de flujo
            runCatching { peer?.shutdownOutput() }
        }
    }

    private fun readLine(input: InputStream): String? {
        val bos = ByteArrayOutputStream(128)
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) return if (bos.size() == 0) null else bos.toString(Charsets.ISO_8859_1)
            if (b == '\n'.code && prev == '\r'.code) {
                val bytes = bos.toByteArray()
                // quitar CR
                return String(bytes, 0, bytes.size - 1, Charsets.ISO_8859_1)
            }
            bos.write(b)
            prev = b
            // seguridad para headers absurdamente grandes
            if (bos.size() > 64 * 1024) return null
        }
    }

    private fun safeClose(closeable: Closeable?) = runCatching { closeable?.close() }
}
