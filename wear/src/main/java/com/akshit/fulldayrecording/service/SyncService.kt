package com.akshit.fulldayrecording.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDateTime

class SyncService(private val context: Context) {
    private val nodeClient = Wearable.getNodeClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val dataClient = Wearable.getDataClient(context)
    
    companion object {
        private const val TAG = "SyncService"
        private const val SYNC_PATH = "/sync_recordings"
        private const val SYNC_SUCCESS_PATH = "/sync_success"
        private const val SYNC_FAILURE_PATH = "/sync_failure"
    }
    
    suspend fun syncRecordings() {
        try {
            val database = AppDatabase.getDatabase(context)
            val unsyncedRecordings = database.recordingDao().getUnsyncedRecordings().first()
            
            if (unsyncedRecordings.isEmpty()) {
                Log.d(TAG, "No unsynced recordings found")
                return
            }
            
            Log.d(TAG, "Found ${unsyncedRecordings.size} unsynced recordings")
            
            val connectedNodes = nodeClient.connectedNodes.await()
            if (connectedNodes.isEmpty()) {
                Log.w(TAG, "No connected nodes found")
                return
            }
            
            for (recording in unsyncedRecordings) {
                val success = syncRecording(recording, connectedNodes.first())
                if (success) {
                    // Mark as synced and delete local file
                    database.recordingDao().updateRecording(recording.copy(isSynced = true))
                    deleteLocalFile(recording.filePath)
                    Log.d(TAG, "Successfully synced recording: ${recording.fileName}")
                } else {
                    Log.e(TAG, "Failed to sync recording: ${recording.fileName}")
                }
            }
            
            // Clean up synced recordings from database
            database.recordingDao().deleteSyncedRecordings()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
        }
    }
    
    private suspend fun syncRecording(recording: RecordingEntity, node: Node): Boolean {
        return try {
            val file = File(recording.filePath)
            if (!file.exists()) {
                Log.w(TAG, "Recording file not found: ${recording.filePath}")
                return false
            }
            
            // Create data map with recording metadata
            val dataMap = DataMap().apply {
                putString("fileName", recording.fileName)
                putLong("startTime", recording.startTime.toEpochSecond(java.time.ZoneOffset.UTC))
                putLong("endTime", recording.endTime.toEpochSecond(java.time.ZoneOffset.UTC))
                putLong("duration", recording.duration)
                putLong("fileSize", recording.fileSize)
                putLong("id", recording.id)
            }
            
            // Send metadata first
            val metadataAsset = PutDataMapRequest.create(SYNC_PATH).dataMap.putDataMap("metadata", dataMap).asPutDataRequest()
            dataClient.putDataItem(metadataAsset).await()
            
            // Send file data
            val fileBytes = file.readBytes()
            val fileAsset = PutDataMapRequest.create(SYNC_PATH).dataMap.putByteArray("fileData", fileBytes).asPutDataRequest()
            dataClient.putDataItem(fileAsset).await()
            
            // Wait for confirmation
            val success = waitForSyncConfirmation(recording.id)
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing recording: ${recording.fileName}", e)
            false
        }
    }
    
    private suspend fun waitForSyncConfirmation(recordingId: Long): Boolean {
        return withTimeoutOrNull(30000) { // 30 second timeout
            try {
                // Listen for sync confirmation
                val dataItems = dataClient.dataItems.filter { 
                    it.uri.path == SYNC_SUCCESS_PATH || it.uri.path == SYNC_FAILURE_PATH 
                }.first()
                
                val success = dataItems.uri.path == SYNC_SUCCESS_PATH
                Log.d(TAG, "Sync confirmation received: $success")
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error waiting for sync confirmation", e)
                false
            }
        } ?: false
    }
    
    private fun deleteLocalFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted local file: $filePath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting local file: $filePath", e)
        }
    }
    
    fun startPeriodicSync() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    syncRecordings()
                    delay(5 * 60 * 1000) // Sync every 5 minutes
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic sync", e)
                    delay(60 * 1000) // Wait 1 minute before retry
                }
            }
        }
    }
} 