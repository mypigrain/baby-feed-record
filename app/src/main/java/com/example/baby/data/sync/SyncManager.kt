package com.example.baby.data.sync

import com.example.baby.data.FeedingRecord
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SyncPayload(
    val deviceName: String,
    val records: List<FeedingRecord>
)

data class SyncResult(
    val imported: Int = 0,
    val skipped: Int = 0
)

class SyncManager(private val dao: com.example.baby.data.FeedingDao) {

    private val formatVersion = 1

    suspend fun exportRecords(): String {
        val allRecords = dao.getAllRecords()
        val updatedRecords = mutableListOf<FeedingRecord>()

        for (record in allRecords) {
            if (record.syncId == null) {
                val newId = UUID.randomUUID().toString()
                dao.updateSyncId(record.id, newId)
                updatedRecords.add(record.copy(syncId = newId))
            } else {
                updatedRecords.add(record)
            }
        }

        val deletedSyncIds = dao.getAllDeletedSyncIds()

        return JSONObject().apply {
            put("formatVersion", formatVersion)
            put("exportedAt", System.currentTimeMillis())
            put("records", JSONArray().apply {
                updatedRecords.forEach { r ->
                    put(JSONObject().apply {
                        put("syncId", r.syncId)
                        put("timestamp", r.timestamp)
                        put("amountMl", r.amountMl ?: JSONObject.NULL)
                        put("type", r.type)
                        put("notes", r.notes ?: JSONObject.NULL)
                    })
                }
            })
            put("deletedSyncIds", JSONArray(deletedSyncIds))
        }.toString()
    }

    suspend fun importRecords(json: String): SyncResult {
        val root = JSONObject(json)
        val version = root.optInt("formatVersion", 0)
        if (version != formatVersion) {
            throw IllegalArgumentException("Unsupported format version: $version")
        }

        val recordsArray = root.getJSONArray("records")
        val existingSyncIds = dao.getAllSyncIds().toHashSet()
        val localDeletedSyncIds = dao.getAllDeletedSyncIds().toHashSet()
        var imported = 0
        var skipped = 0

        // Process deleted syncIds from peer: remove matching local records
        if (root.has("deletedSyncIds")) {
            val deletedArray = root.getJSONArray("deletedSyncIds")
            for (i in 0 until deletedArray.length()) {
                val syncId = deletedArray.optString(i, "")
                if (syncId.isNotEmpty() && syncId in existingSyncIds) {
                    dao.deleteBySyncId(syncId)
                }
            }
        }

        for (i in 0 until recordsArray.length()) {
            val obj = recordsArray.getJSONObject(i)
            val syncId = if (obj.has("syncId")) obj.getString("syncId") else continue

            // Skip if already present locally OR was previously deleted locally
            if (syncId in existingSyncIds || syncId in localDeletedSyncIds) {
                skipped++
                continue
            }

            val record = FeedingRecord(
                syncId = syncId,
                timestamp = obj.getLong("timestamp"),
                amountMl = if (obj.isNull("amountMl")) null else obj.getInt("amountMl"),
                type = obj.optString("type", "breast"),
                notes = if (obj.isNull("notes")) null else obj.getString("notes")
            )
            dao.insert(record)
            existingSyncIds.add(syncId)
            imported++
        }

        return SyncResult(imported = imported, skipped = skipped)
    }
}
