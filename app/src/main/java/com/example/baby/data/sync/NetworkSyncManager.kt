package com.example.baby.data.sync

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

data class DiscoveredPeer(
    val ip: String,
    val port: Int,
    val deviceName: String
)

class NetworkSyncManager(
    private val serverPort: Int = 0,
    private val discoveryPort: Int = 56789
) {
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var running = false

    data class SyncExchangeResult(
        val imported: Int,
        val skipped: Int,
        val peerRecords: String?,
        val peerDeletedSyncIds: String?
    )

    fun startServer(): Int {
        serverSocket?.close()
        val socket = ServerSocket(serverPort)
        serverSocket = socket
        running = true
        return socket.localPort
    }

    fun stopServer() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    fun acceptSyncRequest(handler: (BufferedReader, PrintWriter) -> Unit) {
        val socket = serverSocket ?: return
        Thread {
            while (running) {
                try {
                    socket.accept().use { client ->
                        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                        val writer = PrintWriter(client.getOutputStream(), true)
                        handler(reader, writer)
                    }
                } catch (_: Exception) {
                    if (!running) break
                }
            }
        }.apply { isDaemon = true }.start()
    }

    fun discoverPeers(timeoutMs: Int = 3000): List<DiscoveredPeer> {
        val peers = mutableListOf<DiscoveredPeer>()
        val broadcastAddr = "255.255.255.255"
        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

        try {
            val localIp = getLocalIpAddress() ?: return peers

            val socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = timeoutMs
            }

            val discoverMsg = JSONObject().apply {
                put("type", "discover")
                put("deviceName", deviceName)
            }.toString()

            val sendBuf = discoverMsg.toByteArray(Charsets.UTF_8)
            val sendPacket = DatagramPacket(
                sendBuf, sendBuf.size,
                InetAddress.getByName(broadcastAddr), discoveryPort
            )
            socket.send(sendPacket)

            val recvBuf = ByteArray(4096)

            while (true) {
                try {
                    val packet = DatagramPacket(recvBuf, recvBuf.size)
                    socket.receive(packet)
                    val json = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val obj = JSONObject(json)
                    val peerIp = packet.address.hostAddress ?: continue

                    if (peerIp == localIp) continue

                    if (obj.optString("type") == "discover_response") {
                        peers.add(DiscoveredPeer(
                            ip = peerIp,
                            port = obj.optInt("port", 0),
                            deviceName = obj.optString("deviceName", "Unknown")
                        ))
                    }
                } catch (_: SocketTimeoutException) {
                    break
                }
            }
            socket.close()
        } catch (_: Exception) {}

        return peers.distinctBy { it.ip }
    }

    fun startDiscoveryListener(onPeerFound: (DiscoveredPeer) -> Unit) {
        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        Thread {
            try {
                val socket = DatagramSocket(discoveryPort)
                socket.broadcast = true
                val buf = ByteArray(4096)

                while (running) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val json = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val obj = JSONObject(json)
                        val localIp = getLocalIpAddress()
                        val peerIp = packet.address.hostAddress ?: continue

                        if (obj.optString("type") == "discover" && peerIp != localIp) {
                            val serverPort = serverSocket?.localPort ?: 0
                            if (serverPort > 0) {
                                val response = JSONObject().apply {
                                    put("type", "discover_response")
                                    put("deviceName", deviceName)
                                    put("port", serverPort)
                                }.toString()

                                val respBuf = response.toByteArray(Charsets.UTF_8)
                                val respPacket = DatagramPacket(
                                    respBuf, respBuf.size,
                                    packet.address, packet.port
                                )
                                socket.send(respPacket)
                            }

                            val discovered = DiscoveredPeer(
                                ip = peerIp,
                                port = obj.optInt("port", 0),
                                deviceName = obj.optString("deviceName", "Unknown")
                            )
                            onPeerFound(discovered)
                        }
                    } catch (_: Exception) {
                        if (!running) break
                    }
                }
                socket.close()
            } catch (_: Exception) {}
        }.apply { isDaemon = true }.start()
    }

    fun syncWithPeer(ip: String, port: Int, jsonPayload: String): SyncExchangeResult? {
        return try {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(ip, port), 5000)
            socket.soTimeout = 10000

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.println(jsonPayload)

            val response = reader.readLine()

            socket.close()

            if (response != null) {
                val obj = JSONObject(response)
                SyncExchangeResult(
                    imported = obj.optInt("imported", 0),
                    skipped = obj.optInt("skipped", 0),
                    peerRecords = if (obj.has("records")) obj.getString("records") else null,
                    peerDeletedSyncIds = if (obj.has("deletedSyncIds")) obj.getString("deletedSyncIds") else null
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
