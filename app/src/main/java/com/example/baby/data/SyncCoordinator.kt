package com.example.baby.data.sync

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.baby.data.FeedingDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SyncCoordinator(
    private val dao: FeedingDao,
    private val lifecycleOwner: LifecycleOwner,
    private val scope: CoroutineScope
) : LifecycleEventObserver {

    private val syncManager = SyncManager(dao)
    private val networkManager = NetworkSyncManager()
    private var syncJob: Job? = null
    private var serverPort: Int = 0

    private val tag = "SyncCoordinator"

    fun start() {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun stop() {
        lifecycleOwner.lifecycle.removeObserver(this)
        stopSync()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> startSync()
            Lifecycle.Event.ON_STOP -> stopSync()
            Lifecycle.Event.ON_DESTROY -> {
                stop()
            }
            else -> {}
        }
    }

    private fun startSync() {
        if (syncJob != null) return

        // Start TCP server and get assigned port
        serverPort = networkManager.startServer()
        Log.d(tag, "Sync server started on port $serverPort")

        // Start discovery listener (responds to UDP broadcasts)
        networkManager.startDiscoveryListener { peer ->
            Log.d(tag, "Discovered peer: ${peer.deviceName} at ${peer.ip}:${peer.port}")
        }

        // Accept incoming sync requests
        networkManager.acceptSyncRequest { reader, writer ->
            handleIncomingSync(reader, writer)
        }

        // Periodic discovery + sync cycle
        syncJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (serverPort <= 0) {
                    delay(30_000)
                    continue
                }

                try {
                    val peers = networkManager.discoverPeers(timeoutMs = 2000)
                    Log.d(tag, "Discovered ${peers.size} peer(s) during scan")

                    for (peer in peers) {
                        if (!isActive) break
                        try {
                            syncWithPeer(peer)
                        } catch (e: Exception) {
                            Log.w(tag, "Sync with ${peer.deviceName} failed: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Discovery cycle failed: ${e.message}")
                }

                delay(30_000)
            }
        }
    }

    private fun stopSync() {
        syncJob?.cancel()
        syncJob = null
        networkManager.stopServer()
        Log.d(tag, "Sync stopped")
    }

    private suspend fun syncWithPeer(peer: DiscoveredPeer) {
        // Export our records
        val localJson = syncManager.exportRecords()

        // Build sync payload: include local records and let peer know our server port
        val payload = JSONObject().apply {
            put("type", "sync")
            put("port", serverPort)
            put("deviceName", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            put("records", JSONObject(localJson).getJSONArray("records"))
        }.toString()

        // Connect to peer and exchange
        val result = networkManager.syncWithPeer(peer.ip, peer.port, payload)
        if (result != null && result.peerRecords != null) {
            // Import peer's records
            val peerPayload = JSONObject().apply {
                put("formatVersion", 1)
                put("records", JSONArray(result.peerRecords))
            }.toString()

            val syncResult = syncManager.importRecords(peerPayload)
            if (syncResult.imported > 0) {
                Log.d(tag, "Imported ${syncResult.imported} records from ${peer.deviceName}")
            }
        }
    }

    private fun handleIncomingSync(reader: java.io.BufferedReader, writer: java.io.PrintWriter) {
        try {
            val remoteJson = reader.readLine() ?: return

            // SyncManager runs on IO but we're already on a background thread
            kotlinx.coroutines.runBlocking {
                // Export our records BEFORE importing (to send only pre-existing records)
                val ourRecords = syncManager.exportRecords()

                // Import peer's records
                val root = JSONObject(remoteJson)
                val peerRecords = root.optJSONArray("records")
                val payload = JSONObject().apply {
                    put("formatVersion", 1)
                    put("records", peerRecords)
                }.toString()
                val syncResult = syncManager.importRecords(payload)

                // Send response with our records (from before import)
                val response = JSONObject().apply {
                    put("type", "sync_response")
                    put("imported", syncResult.imported)
                    put("skipped", syncResult.skipped)
                    put("records", JSONObject(ourRecords).getJSONArray("records"))
                }.toString()

                writer.println(response)
                Log.d(tag, "Handled sync: imported ${syncResult.imported}, skipped ${syncResult.skipped}")
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to handle incoming sync: ${e.message}")
        }
    }
}
