package com.pensa.adbvortex.net

import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConnectionPool {

    private val tcpPool = ConcurrentHashMap<String, Socket>()
    private val httpPool = ConcurrentHashMap<String, HttpURLConnection>()
    private val idleTimeout = TimeUnit.MINUTES.toMillis(2)
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor()

    init {
        //- Purga automática cada 2 minutos
        cleanupExecutor.scheduleAtFixedRate({
            try {
                purgeIdleConnections()
            } catch (_: Exception) {}
        }, 2, 2, TimeUnit.MINUTES)
    }

    //- Obtiene o crea un socket TCP directo (para CONNECT)
    fun getOrCreate(host: String, port: Int): Socket {
        val key = "$host:$port"
        val existing = tcpPool[key]

        if (existing != null && existing.isConnected && !existing.isClosed) {
            return existing
        }

        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 5000)
            socket.soTimeout = 10000
            socket.keepAlive = true
            tcpPool[key] = socket
            socket
        } catch (e: IOException) {
            tcpPool.remove(key)
            throw e
        }
    }

    //- Obtiene o crea un HttpURLConnection (no persistente)
    fun getOrCreateHttp(url: URL): HttpURLConnection {
        val key = "${url.protocol}://${url.host}:${url.port.takeIf { it > 0 } ?: 80}"
        httpPool[key]?.disconnect()
        httpPool.remove(key)

        val conn = (url.openConnection() as HttpURLConnection).apply {
            doInput = true
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Connection", "close")
        }

        httpPool[key] = conn
        return conn
    }

    //- Cierra y limpia todas las conexiones
    fun closeAll() {
        tcpPool.values.forEach {
            try { it.close() } catch (_: Exception) {}
        }
        tcpPool.clear()

        httpPool.values.forEach {
            try { it.disconnect() } catch (_: Exception) {}
        }
        httpPool.clear()
    }

    //- Purga conexiones TCP inactivas
    private fun purgeIdleConnections() {
        tcpPool.entries.removeIf { (_, socket) ->
            try {
                socket.isClosed || !socket.isConnected || socket.isInputShutdown || socket.isOutputShutdown
            } catch (_: Exception) { true }
        }
    }

    fun shutdown() {
        try {
            cleanupExecutor.shutdownNow()
        } catch (_: Exception) {}
        closeAll()
    }
}
