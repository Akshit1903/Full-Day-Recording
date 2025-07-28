package com.akshit.fulldayrecording.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import com.akshit.fulldayrecording.data.RecordingEntity
import com.akshit.fulldayrecording.data.AppDatabase

class AudioRecorderService(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)
    private var currentFile: File? = null
    private var startTime: LocalDateTime? = null
    
    companion object {
        private const val TAG = "AudioRecorderService"
        private const val BATCH_DURATION_MINUTES = 30L // 30 minutes per batch
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
    }
    
    fun startRecording() {
        if (isRecording.get()) {
            Log.w(TAG, "Recording already in progress")
            return
        }
        
        isRecording.set(true)
        startTime = LocalDateTime.now()
        
        currentRecordingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isRecording.get()) {
                    val batchStartTime = LocalDateTime.now()
                    val fileName = generateFileName(batchStartTime)
                    val file = createRecordingFile(fileName)
                    
                    startMediaRecorder(file)
                    currentFile = file
                    
                    // Record for BATCH_DURATION_MINUTES
                    delay(BATCH_DURATION_MINUTES * 60 * 1000)
                    
                    if (isRecording.get()) {
                        stopMediaRecorder()
                        saveRecordingToDatabase(file, batchStartTime, LocalDateTime.now())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during recording", e)
                stopMediaRecorder()
            }
        }
        
        Log.i(TAG, "Recording started")
    }
    
    fun stopRecording() {
        if (!isRecording.get()) {
            Log.w(TAG, "No recording in progress")
            return
        }
        
        isRecording.set(false)
        currentRecordingJob?.cancel()
        
        stopMediaRecorder()
        
        Log.i(TAG, "Recording stopped")
    }
    
    private fun startMediaRecorder(file: File) {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioChannels(1)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                
                prepare()
                start()
            }
            
            Log.d(TAG, "MediaRecorder started for file: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting MediaRecorder", e)
            throw e
        }
    }
    
    private fun stopMediaRecorder() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d(TAG, "MediaRecorder stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        }
    }
    
    private fun createRecordingFile(fileName: String): File {
        val recordingsDir = File(context.filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }
        return File(recordingsDir, fileName)
    }
    
    private fun generateFileName(startTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        return "recording_${startTime.format(formatter)}.m4a"
    }
    
    private suspend fun saveRecordingToDatabase(file: File, startTime: LocalDateTime, endTime: LocalDateTime) {
        try {
            val duration = java.time.Duration.between(startTime, endTime).toMillis()
            val fileSize = file.length()
            
            val recording = RecordingEntity(
                fileName = file.name,
                filePath = file.absolutePath,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                fileSize = fileSize
            )
            
            // Save to database
            val database = AppDatabase.getDatabase(context)
            database.recordingDao().insertRecording(recording)
            
            Log.d(TAG, "Recording saved to database: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving recording to database", e)
        }
    }
    
    fun isRecording(): Boolean = isRecording.get()
} 