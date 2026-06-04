package com.herohan.uvcdemo

import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herohan.uvcdemo.ui.BaseCameraViewModel
import com.herohan.uvcdemo.ui.CameraPreview
import com.herohan.uvcdemo.ui.DeviceListDialog
import com.herohan.uvcdemo.ui.theme.UVCAndroidTheme
import com.herohan.uvcdemo.utils.TimeFormatter
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import kotlinx.coroutines.delay

class RecordVideoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UVCAndroidTheme {
                RecordVideoScreen()
            }
        }
    }
}

@Composable
private fun RecordVideoScreen(
    viewModel: BaseCameraViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Toast handling
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Recording timer
    var recordStartTime by remember { mutableStateOf(0L) }
    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            recordStartTime = SystemClock.elapsedRealtime()
            while (true) {
                delay(250)
                viewModel.updateRecordTime(SystemClock.elapsedRealtime() - recordStartTime)
            }
        } else {
            viewModel.updateRecordTime(0)
        }
    }

    Scaffold(
        topBar = {
            RecordVideoTopAppBar()
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues),
        ) {
            if (uiState.isCameraConnected) {
                CameraPreview(
                    onAddSurface = { surface -> viewModel.addSurface(surface, false) },
                    onRemoveSurface = { surface -> viewModel.removeSurface(surface) },
                    previewWidth = uiState.previewWidth,
                    previewHeight = uiState.previewHeight,
                    modifier = Modifier.align(Alignment.Center),
                )

                if (uiState.isRecording) {
                    Text(
                        text = TimeFormatter.formatRecordTimeMMSS(uiState.recordTimeMillis.toInt()),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Red,
                    )
                }

                FloatingActionButton(
                    onClick = {
                        XXPermissions.with(context as RecordVideoActivity)
                            .permission(PermissionLists.getManageExternalStoragePermission())
                            .permission(PermissionLists.getRecordAudioPermission())
                            .request { _, allGranted ->
                                if (allGranted) viewModel.toggleVideoRecord()
                            }
                    },
                    containerColor = if (uiState.isRecording) Color.Red
                    else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = stringResource(R.string.action_record),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.main_connect_usb_tip),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // Dialogs
    if (uiState.showDeviceListDialog) {
        DeviceListDialog(
            deviceList = uiState.deviceList,
            currentDevice = uiState.selectedDevice,
            onDeviceSelected = { device ->
                if (uiState.isCameraConnected) viewModel.safelyEject()
                viewModel.selectDevice(device)
            },
            onDismiss = { viewModel.dismissDeviceListDialog() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordVideoTopAppBar() {
    TopAppBar(
        title = { Text(stringResource(R.string.entry_record_video)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
