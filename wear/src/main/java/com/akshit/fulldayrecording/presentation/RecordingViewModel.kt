package com.akshit.fulldayrecording.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshit.fulldayrecording.data.AppDatabase
import com.akshit.fulldayrecording.data.RecordingEntity
import com.akshit.fulldayrecording.service.AudioRecorderService
import com.akshit.fulldayrecording.service.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordingViewModel(application: Application) : AndroidViewModel(application) {
    private val audioRecorderService = AudioRecorderService(application)
    private val syncService = SyncService(application)
    private val database = AppDatabase.getDatabase(application)
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _recordings = MutableStateFlow<List<RecordingEntity>>(emptyList())
    val recordings: StateFlow<List<RecordingEntity>> = _recordings.asStateFlow()
    
    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()
    
    init {
        loadRecordings()
        startPeriodicSync()
        updateUnsyncedCount()
    }
    
    fun startRecording() {
        if (!_isRecording.value) {
            audioRecorderService.startRecording()
            _isRecording.value = true
        }
    }
    
    fun stopRecording() {
        if (_isRecording.value) {
            audioRecorderService.stopRecording()
            _isRecording.value = false
            loadRecordings()
            updateUnsyncedCount()
        }
    }
    
    fun syncRecordings() {
        viewModelScope.launch {
            syncService.syncRecordings()
            loadRecordings()
            updateUnsyncedCount()
        }
    }
    
    private fun loadRecordings() {
        viewModelScope.launch {
            database.recordingDao().getAllRecordings().collect { recordings ->
                _recordings.value = recordings
            }
        }
    }
    
    private fun updateUnsyncedCount() {
        viewModelScope.launch {
            val count = database.recordingDao().getUnsyncedCount()
            _unsyncedCount.value = count
        }
    }
    
    private fun startPeriodicSync() {
        syncService.startPeriodicSync()
    }
    
    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) {
            audioRecorderService.stopRecording()
        }
    }
} 