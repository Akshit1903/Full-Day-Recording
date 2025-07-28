package com.akshit.fulldayrecording.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.akshit.fulldayrecording.presentation.theme.FullDayRecordingWatchTheme
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: RecordingViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        
        setContent {
            WearApp(viewModel)
        }
    }
}

@Composable
fun WearApp(viewModel: RecordingViewModel) {
    FullDayRecordingWatchTheme {
        val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
        val recordings by viewModel.recordings.collectAsStateWithLifecycle()
        val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status text
                Text(
                    text = if (isRecording) "Recording..." else "Ready to Record",
                    color = if (isRecording) Color.Red else MaterialTheme.colors.primary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                // Recording button
                Button(
                    onClick = {
                        if (isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isRecording) Color.Red else MaterialTheme.colors.primary
                    ),
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp)
                ) {
                    Text(
                        text = if (isRecording) "STOP" else "START",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
                
                // Sync status
                if (unsyncedCount > 0) {
                    Text(
                        text = "$unsyncedCount unsynced",
                        color = MaterialTheme.colors.secondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Button(
                        onClick = { viewModel.syncRecordings() },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Sync", fontSize = 10.sp)
                    }
                }
                
                // Recent recordings
                if (recordings.isNotEmpty()) {
                    Text(
                        text = "Recent Recordings:",
                        color = MaterialTheme.colors.primary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    recordings.take(3).forEach { recording ->
                        RecordingItem(recording = recording)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingItem(recording: com.akshit.fulldayrecording.data.RecordingEntity) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val startTime = recording.startTime.format(formatter)
    val endTime = recording.endTime.format(formatter)
    val durationMinutes = recording.duration / (1000 * 60)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(8.dp)
    ) {
        Text(
            text = "$startTime - $endTime",
            fontSize = 10.sp,
            color = MaterialTheme.colors.primary
        )
        Text(
            text = "${durationMinutes}min",
            fontSize = 8.sp,
            color = MaterialTheme.colors.secondary
        )
        if (recording.isSynced) {
            Text(
                text = "✓ Synced",
                fontSize = 8.sp,
                color = Color.Green
            )
        } else {
            Text(
                text = "⏳ Pending",
                fontSize = 8.sp,
                color = Color.Yellow
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    // Preview placeholder
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Text("Recording App")
    }
}